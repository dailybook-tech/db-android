package com.dailybook.keep.screen.profile.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.dailybook.base.BaseBottomsheetFragment
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.keep.databinding.FragmentEditProfileBottomsheetBinding
import com.dailybook.keep.screen.calendar.utils.Constants
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditProfileBottomsheetFragment : BaseBottomsheetFragment<FragmentEditProfileBottomsheetBinding>() {
    override val screenName: String
        get() = ConstantEventNames.EDIT_PROFILE

    private var staffId: String? = null
    private var staffName: String? = null
    private var staffMobile: String? = null
    private var initialSalary: Double? = null
    private var initialSalaryType: String? = null
    private var initialBonus: Double? = null
    private val viewModel: com.dailybook.keep.screen.profile.viewmodel.EditProfileViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        staffId = arguments?.getString(ARG_STAFF_ID)
        staffName = arguments?.getString(ARG_STAFF_NAME)
        staffMobile = arguments?.getString(ARG_STAFF_MOBILE)
        initialSalary = arguments?.getDouble(ARG_INITIAL_SALARY, 0.0)
        initialSalaryType = arguments?.getString(ARG_INITIAL_SALARY_TYPE)
        initialBonus = arguments?.getDouble(ARG_INITIAL_BONUS, 0.0).takeIf { it != 0.0 }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentEditProfileBottomsheetBinding? {
        return FragmentEditProfileBottomsheetBinding.inflate(inflater, container, false)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListeners()
        setupTextChangeListeners()
        binding?.etStaffName?.setText(staffName.orEmpty())
        binding?.etMobileNumber?.setText(staffMobile.orEmpty())
        
        // Prefill salary field if data is available
        initialSalary?.let { salary ->
            if (salary > 0) {
                binding?.etSalaryAmount?.setText(salary.toString())
            }
        }

        // Prefill bonus field if data is available
        initialBonus?.let { bonus ->
            if (bonus > 0) {
                binding?.etBonusAmount?.setText(bonus.toString())
            }
        }

        updateSaveButtonState()

        // Observe update results

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                val newName = binding?.etStaffName?.text?.toString()?.trim().orEmpty()
                val bundle = Bundle().apply { putString("updated_staff_name", newName) }
                parentFragmentManager.setFragmentResult("edit_profile_result", bundle)
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(requireContext(), result.exceptionOrNull()?.message ?: "Update failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupTextChangeListeners() {
        binding?.etStaffName?.addTextChangedListener { updateSaveButtonState() }
        binding?.etMobileNumber?.addTextChangedListener { updateSaveButtonState() }
        binding?.etSalaryAmount?.addTextChangedListener { updateSaveButtonState() }
        binding?.etBonusAmount?.addTextChangedListener { updateSaveButtonState() }
    }

    private fun setOnClickListeners() {
        binding?.ivClose?.setOnClickListener { dismiss() }
        binding?.btnSave?.setOnClickListener {
            val newName = binding?.etStaffName?.text?.toString()?.trim().orEmpty()
            val newMobile = binding?.etMobileNumber?.text?.toString()?.trim().orEmpty()
            val salaryText = binding?.etSalaryAmount?.text?.toString()?.trim().orEmpty()
            val bonusText = binding?.etBonusAmount?.text?.toString()?.trim().orEmpty()
            val salary = salaryText.toDoubleOrNull()
            val bonus = bonusText.toDoubleOrNull()
            val salaryType = Constants.SALARY_TYPE_DAILY
            val nameChanged = newName != staffName
            val mobileChanged = newMobile.isNotEmpty() && newMobile != staffMobile
            val salaryChanged = salary != null && salary != initialSalary
            val bonusChanged = bonus != null && bonus != initialBonus

            if (!nameChanged && !mobileChanged && !salaryChanged && !bonusChanged) {
                dismiss()
                return@setOnClickListener
            }

            viewModel.updateStaffProfile(
                staffId = staffId ?: return@setOnClickListener,
                newName = newName,
                newMobileNumber = if (mobileChanged) newMobile else null,
                salaryType = salaryType,
                salary = salary,
                salaryChanged = salaryChanged || bonusChanged,
                bonus = bonus
            )
        }
    }

    private fun updateSaveButtonState() {
        val newName = binding?.etStaffName?.text?.toString()?.trim().orEmpty()
        val newMobile = binding?.etMobileNumber?.text?.toString()?.trim().orEmpty()
        val salaryText = binding?.etSalaryAmount?.text?.toString()?.trim().orEmpty()
        val bonusText = binding?.etBonusAmount?.text?.toString()?.trim().orEmpty()
        val salary = salaryText.toDoubleOrNull()
        val bonus = bonusText.toDoubleOrNull()

        val nameChanged = newName.isNotEmpty() && newName != staffName
        val mobileChanged = newMobile.isNotEmpty() && newMobile != staffMobile
        val salaryChanged = salary != null && salary != initialSalary
        val bonusChanged = bonus != null && bonus != initialBonus

        binding?.btnSave?.isEnabled = newName.isNotEmpty() && (nameChanged || mobileChanged || salaryChanged || bonusChanged)
    }

    companion object {
        private const val ARG_STAFF_ID = "staff_id"
        private const val ARG_STAFF_NAME = "staff_name"
        private const val ARG_STAFF_MOBILE = "staff_mobile"
        private const val ARG_INITIAL_SALARY = "initial_salary"
        private const val ARG_INITIAL_SALARY_TYPE = "initial_salary_type"
        private const val ARG_INITIAL_BONUS = "initial_bonus"
        fun newInstance(
            staffId: String,
            staffName: String,
            staffMobile: String? = null,
            initialSalary: Double? = null,
            initialSalaryType: String? = null,
            initialBonus: Double? = null
        ): EditProfileBottomsheetFragment {
            val fragment = EditProfileBottomsheetFragment()
            val args = Bundle()
            args.putString(ARG_STAFF_ID, staffId)
            args.putString(ARG_STAFF_NAME, staffName)
            staffMobile?.let { args.putString(ARG_STAFF_MOBILE, it) }
            initialSalary?.let { args.putDouble(ARG_INITIAL_SALARY, it) }
            initialSalaryType?.let { args.putString(ARG_INITIAL_SALARY_TYPE, it) }
            initialBonus?.let { args.putDouble(ARG_INITIAL_BONUS, it) }
            fragment.arguments = args
            return fragment
        }
    }
} 