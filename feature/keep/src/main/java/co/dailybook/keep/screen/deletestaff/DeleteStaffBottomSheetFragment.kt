package co.dailybook.keep.screen.deletestaff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import co.dailybook.boilerplate.uikit.views.hide
import co.dailybook.boilerplate.uikit.views.show
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import co.dailybook.base.BaseBottomsheetFragment
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.keep.databinding.FragmentDeleteStaffBottomsheetBinding
import co.dailybook.keep.screen.calendar.utils.ObserverUtil
import co.dailybook.keep.screen.deletestaff.uistate.DeleteStaffUiState
import co.dailybook.keep.screen.deletestaff.viewmodel.DeleteStaffViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


private const val ID = "id"
private const val NAME = "name"

class DeleteStaffBottomsheetFragment : BaseBottomsheetFragment<FragmentDeleteStaffBottomsheetBinding>() {

    override val screenName: String
        get() = ConstantEventNames.DELETE_STAFF_BS
    private var id: String? = ""
    private var name: String? = ""
    private val viewModel: DeleteStaffViewModel by viewModel()
    private val observerUtil: ObserverUtil by inject()

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
        arguments?.let {
            id = it.getString(ID)
            name = it.getString(NAME)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentDeleteStaffBottomsheetBinding? {
        return FragmentDeleteStaffBottomsheetBinding.inflate(inflater, container, false)
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
        setUpViews()
        viewModelObserver()
        setOnClickListeners()
    }

    private fun viewModelObserver() {
        viewModel.uiState().observe(viewLifecycleOwner) {
            when (it) {
                is DeleteStaffUiState.Loading -> {
                    binding?.pb?.show()
                }

                is DeleteStaffUiState.Success -> {
                    binding?.pb?.hide()
                    dismiss()
                    observerUtil.goBackFromCalendar?.invoke(true)
                }

                is DeleteStaffUiState.Error -> {
                    binding?.pb?.hide()
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private fun setUpViews() {
        binding?.apply {
            tvName.text = name
            try {
                name?.isNotEmpty()?.let {
                    tvInitial.text = name?.first().toString()
                }
            }catch (e: Exception){}
        }
    }

    private fun setOnClickListeners() {
        binding?.ivClose?.setOnClickListener {
            dismiss()
        }

        binding?.btnDeleteStaff?.setOnActiveListener {
            id?.let { viewModel.deleteStaffUser(it) }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(id: String, name: String) =
            DeleteStaffBottomsheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ID, id)
                    putString(NAME, name)
                }
            }
    }
}