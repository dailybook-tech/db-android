package co.dailybook.keep.screen.advance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import co.dailybook.boilerplate.uikit.views.hide
import co.dailybook.boilerplate.uikit.views.show
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import co.dailybook.base.BaseBottomsheetFragment
import co.dailybook.base.analytics.ConstantEventAttributes
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.base.toFormattedDate
import co.dailybook.base.toggleKeyboard
import co.dailybook.keep.R
import co.dailybook.keep.databinding.FragmentPayAdvanceBottomsheetBinding
import co.dailybook.keep.model.AddAdvanceRequestBody
import co.dailybook.keep.model.Advance
import co.dailybook.keep.screen.advance.uistate.AddAdvanceUiState
import co.dailybook.keep.screen.advance.viewmodel.AddAdvanceViewModel
import co.dailybook.keep.screen.calendar.utils.ObserverUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


private const val ID = "id"
private const val NAME = "name"
private const val DATE = "date"
private const val ADVANCE = "advance"
private const val REASON = "reason"

class PayAdvanceBottomsheetFragment :
    BaseBottomsheetFragment<FragmentPayAdvanceBottomsheetBinding>() {

    override val screenName: String
        get() = ConstantEventNames.ADVANCE_BS
    private var id: String? = ""
    private var advance: String? = ""
    private var date: String? = ""
    private var name: String? = ""
    private var reason: String? = ""
    private var paymentMethod: String = "cash" // Default to cash
    private val viewModel: AddAdvanceViewModel by viewModel()
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
            date = it.getString(DATE)
            advance = it.getString(ADVANCE)
            reason = it.getString(REASON)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentPayAdvanceBottomsheetBinding? {
        return FragmentPayAdvanceBottomsheetBinding.inflate(inflater, container, false)
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
        lifecycleScope.launch {
            delay(500)
            binding?.etAdvance?.toggleKeyboard(requireActivity())
        }
    }

    private fun viewModelObserver() {
        viewModel.uiState().observe(viewLifecycleOwner) {
            when (it) {
                is AddAdvanceUiState.Loading -> {
                    binding?.pb?.show()
                }

                is AddAdvanceUiState.Success -> {
                    binding?.pb?.hide()
                    observerUtil.refreshCalendar?.invoke(true, true, advance ?: "", date?.take(2)?.toInt()
                        ?.minus(1)
                        ?: 0)
                    lifecycleScope.launch {
                        dataStoreManager.write(DataStoreManager.INTERACTED_WITH_APP_FEATURES, true)
                    }
                    dismiss()
                }

                is AddAdvanceUiState.Error -> {
                    binding?.pb?.hide()
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }

                is AddAdvanceUiState.AdvanceEntered -> {}

                else -> {}
            }
        }
    }

    private fun setUpViews() {
        binding?.apply {
            tvStaffName.text = getString(R.string.to).plus(" ").plus(name)
            tvDate.text = date?.toFormattedDate()
            if (advance?.isNotEmpty() == true && advance?.equals("0") == false) {
                etAdvance.setText(advance)
                // Set cursor to the end of the content
                if (etAdvance.getText().toString().isNotEmpty()) {
                    etAdvance.setSelection(etAdvance.getText().toString().length)
                }
                btnRemoveAdvance.show()
            } else {
                btnRemoveAdvance.hide()
            }
            etDescription.setText(reason)
            setupInputWatchers()
            updateAddButtonState()

            // Initialize payment method toggle (default to cash)
            setupPaymentMethodToggle()
        }
    }

    private fun setupInputWatchers() {
        binding?.apply {
            etAdvance.doAfterTextChanged { updateAddButtonState() }
            etDescription.doAfterTextChanged { updateAddButtonState() }
        }
    }

    private fun updateAddButtonState() {
        binding?.apply {
            val amountText = etAdvance.text?.toString()?.trim().orEmpty()
            val notesText = etDescription.text?.toString()?.trim().orEmpty()
            val hasAmount = amountText.isNotEmpty() && amountText != "0"
            val hasNotes = notesText.isNotEmpty()
            btnAddAdvance.isEnabled = hasAmount || hasNotes
        }
    }
    
    private fun setupPaymentMethodToggle() {
        binding?.apply {
            // Set initial state - cash is selected by default
            resetPaymentMethodBackgrounds()
            rbCash.background = ContextCompat.getDrawable(requireContext(), R.drawable.toggle_button_right_selected)
            rbCash.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            rbOnline.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_primary))
            paymentMethod = "cash"
            
            rbOnline.setOnClickListener {
                selectPaymentMethod("online")
            }
            
            rbCash.setOnClickListener {
                selectPaymentMethod("cash")
            }
        }
    }
    
    private fun selectPaymentMethod(method: String) {
        binding?.apply {
            paymentMethod = method
            
            if (method == "online") {
                rbOnline.background = ContextCompat.getDrawable(requireContext(), R.drawable.toggle_button_left_selected)
                rbOnline.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                rbCash.background = ContextCompat.getDrawable(requireContext(), R.drawable.toggle_button_right)
                rbCash.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_primary))
            } else {
                rbOnline.background = ContextCompat.getDrawable(requireContext(), R.drawable.toggle_button_left)
                rbOnline.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_primary))
                rbCash.background = ContextCompat.getDrawable(requireContext(), R.drawable.toggle_button_right_selected)
                rbCash.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
    }
    
    private fun resetPaymentMethodBackgrounds() {
        binding?.apply {
            rbOnline.background = ContextCompat.getDrawable(requireContext(), R.drawable.toggle_button_left)
            rbOnline.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_primary))
            
            rbCash.background = ContextCompat.getDrawable(requireContext(), R.drawable.toggle_button_right)
            rbCash.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_primary))
        }
    }

    private fun setOnClickListeners() {
        binding?.ivClose?.setOnClickListener {
            dismiss()
        }

        binding?.btnAddAdvance?.setOnClickListener {
            addOrUpdateAdvance(binding?.etAdvance?.text.toString().trim())
            recordClickEvent(ConstantEventNames.ADD_ADVANCE, hashMapOf(Pair(ConstantEventAttributes.AMOUNT,
                binding?.etAdvance?.text.toString().trim()
            )))
        }

        binding?.btnRemoveAdvance?.setOnClickListener {
            addOrUpdateAdvance("0")
            recordClickEvent(ConstantEventNames.DELETE_ADVANCE)
        }
    }

    private fun addOrUpdateAdvance(newAdvance: String) {
        try {
            advance = newAdvance
            val updatedDescription = binding?.etDescription?.text?.toString()?.trim().orEmpty()
            val parsedAdvance = newAdvance.toIntOrNull() ?: 0
            if (parsedAdvance != 0 || updatedDescription.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        viewModel.addAdvance(
                            id ?: "",
                            AddAdvanceRequestBody(
                                Advance(
                                    date ?: "",
                                    parsedAdvance,
                                    updatedDescription,
                                    paymentMethod
                                ), dataStoreManager.read(DataStoreManager.USER_ID, "").first()
                            )
                        )
                    }catch (e: Exception){}
                }
            }
        }catch (e: Exception){}
    }

    companion object {
        @JvmStatic
        fun newInstance(id: String, name: String, date: String, advance: String, reason: String) =
            PayAdvanceBottomsheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ID, id)
                    putString(NAME, name)
                    putString(DATE, date)
                    putString(ADVANCE, advance)
                    putString(REASON, reason)
                }
            }
    }
}