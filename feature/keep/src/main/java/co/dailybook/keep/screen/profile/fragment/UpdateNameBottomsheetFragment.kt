package co.dailybook.keep.screen.profile.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import co.dailybook.boilerplate.uikit.views.hide
import co.dailybook.boilerplate.uikit.views.show
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import co.dailybook.base.BaseBottomsheetFragment
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.base.toggleKeyboard
import co.dailybook.keep.R
import co.dailybook.keep.databinding.FragmentUpdateNameBottomsheetBinding
import co.dailybook.keep.model.UpdateUserNameRequestBody
import co.dailybook.keep.screen.profile.uistate.UserUiState
import co.dailybook.keep.screen.profile.viewmodel.UserProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class UpdateNameBottomsheetFragment : BaseBottomsheetFragment<FragmentUpdateNameBottomsheetBinding>() {

    override val screenName: String
        get() = ConstantEventNames.UPDATE_NAME_BS
    private val viewModel: UserProfileViewModel by sharedViewModel()

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentUpdateNameBottomsheetBinding? {
        return FragmentUpdateNameBottomsheetBinding.inflate(inflater,container,false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViews()
        setOnClickListeners()
        setViewModelObservers()
        lifecycleScope.launch {
            delay(500)
            binding?.etName?.toggleKeyboard(requireActivity())
        }
    }

    private fun setViewModelObservers() {
        viewModel.uiState().observe(viewLifecycleOwner) {
            when (it) {
                is UserUiState.UpdateUserNameSuccess -> {
                    lifecycleScope.launch {
                        dataStoreManager.write(DataStoreManager.USER_NAME,
                            binding?.etName?.text.toString()
                        )
                        binding?.pb?.hide()
                        Toast.makeText(requireContext(), getString(R.string.user_name_updated_successfully), Toast.LENGTH_SHORT).show()
                        dismiss()
                        viewModel.triggerUpdateUserNameUiState()
                    }
                }
                is UserUiState.Error -> {
                    binding?.pb?.hide()
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                is UserUiState.Loading -> {
                    binding?.pb?.show()
                }
                is UserUiState.RefreshUserNameSuccess -> {}
                else -> {}
            }
        }
    }

    private fun setViews() {
        lifecycleScope.launch {
            binding?.etName?.setText(dataStoreManager.read(DataStoreManager.USER_NAME, "").first())
            if (binding?.etName?.getText().toString().isNotEmpty()) {
                binding?.etName?.setSelection(binding?.etName?.getText().toString().length)
            }
        }
    }

    private fun setOnClickListeners() {
        binding?.ivClose?.setOnClickListener {
            dismiss()
        }

        binding?.btnUpdateName?.setOnClickListener {
            lifecycleScope.launch {
                val name = binding?.etName?.text.toString()
                viewModel.updateUserName(dataStoreManager.read(DataStoreManager.USER_ID, "").first(), UpdateUserNameRequestBody(name))
                recordClickEvent(ConstantEventNames.EDIT_PROFILE_NAME)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = UpdateNameBottomsheetFragment()
    }
}