package com.laborbook.income.screen.cashentry.fragment

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.laborbook.base.BaseBottomsheetFragment
import com.laborbook.base.analytics.ConstantEventAttributes
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.base.toReadableDate
import com.laborbook.base.toggleKeyboard
import com.laborbook.income.model.DeleteTransactionResponseModel
import com.laborbook.income.model.Transaction
import com.laborbook.income.model.TransactionRequest
import com.laborbook.income.screen.cashentry.uistate.CashInOutUiState
import com.laborbook.income.R
import com.laborbook.income.databinding.FragmentCashInBinding
import com.laborbook.income.screen.cashentry.viewmodel.CashInOutViewModel
import com.laborbook.income.util.Constants
import com.laborbook.income.util.IncomeObserverUtil
import com.laborbook.income.util.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val ID = "id"
private const val TRANSACTION_TYPE = "transaction_type"
private const val DATE = "date"
private const val AMOUNT = "amount"
private const val REASON = "reason"
private const val PAYMENT_METHOD = "payment_method"

class CashInOutBottomSheetFragment : BaseBottomsheetFragment<FragmentCashInBinding>() {

    private val incomeObserverUtil: IncomeObserverUtil by inject()

    override val screenName: String
        get() = ConstantEventNames.CASH_IN_OUT_BS

    private val viewModel: CashInOutViewModel by viewModel()
    private var id: String? = ""
    private var amount: String? = ""
    private var date: String? = ""
    private var transactionType: String = ""
    private var reason: String? = ""
    private var isUpdate: Boolean = false
    private var paymentMethod: String = "cash" // Default to cash

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            id = it.getString(ID)
            transactionType = it.getString(TRANSACTION_TYPE).toString()
            date = it.getString(DATE)
            amount = it.getString(AMOUNT)
            reason = it.getString(REASON)
            val paymentMethodArg = it.getString(PAYMENT_METHOD)
            paymentMethod = if (paymentMethodArg.isNullOrEmpty()) {
                "cash"
            } else {
                // Normalize to lowercase for consistency
                paymentMethodArg.lowercase()
            }
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentCashInBinding? {
        return FragmentCashInBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (date.isNullOrEmpty()) {
            date = Utils.getCurrentTimeInISOFormat()
        }
        setUpViews()
        viewModelObserver()
        setOnClickListeners()
        lifecycleScope.launch {
            delay(500)
            binding?.etAmount?.toggleKeyboard(requireActivity())
        }
    }

    private fun viewModelObserver() {
        viewModel.uiState().observe(viewLifecycleOwner) {
            when (it) {
                is CashInOutUiState.ExpenseEntered -> updateSaveButtonState()

                is CashInOutUiState.ERROR -> {
                    binding?.pb?.hide()
                    dismiss()
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }

                is CashInOutUiState.LOADING -> binding?.pb?.show()
                is CashInOutUiState.CREATE_SUCCESS -> {
                    binding?.pb?.hide()
                    incomeObserverUtil.onIncomeAddedOrUpdated?.invoke(it.data as Transaction, false)
                    dismiss()
                }

                is CashInOutUiState.UPDATE_SUCCESS -> {
                    binding?.pb?.hide()
                    incomeObserverUtil.onIncomeAddedOrUpdated?.invoke(it.data as Transaction, true)
                    dismiss()
                }

                is CashInOutUiState.DELETE_SUCCESS -> {
                    recordClickEvent(ConstantEventNames.DELETE_INCOME_CONFIRM)
                    binding?.pb?.hide()
                    incomeObserverUtil.onIncomeDeleted?.invoke(it.data as DeleteTransactionResponseModel)
                    dismiss()
                }
            }
        }
    }

    private fun updateSaveButtonState() {
        val isAmountFilled = binding?.etAmount?.text?.isNotEmpty() == true
        binding?.btnSave?.isEnabled = isAmountFilled
    }

    private fun setUpViews() {
        binding?.apply {
            tvDate.text = date?.toReadableDate()
            tvTitle.text =
                if (transactionType == Constants.CREDIT) getString(R.string.cash_in) else getString(
                    R.string.cash_out
                )
            if (amount?.isNotEmpty() == true && amount != "0") {
                isUpdate = true
                etAmount.setText(amount)
                if (etAmount.text.toString().isNotEmpty()) {
                    etAmount.setSelection(etAmount.text.toString().length)
                }
                btnSave.isEnabled = true
                btnDelete.show()
            } else {
                isUpdate = false
                btnSave.isEnabled = false
                btnDelete.hide()
            }
            etDescription.setText(reason)
            // Add input filter to limit to 8 digits
            etAmount.filters = arrayOf(MaxDigitsInputFilter(8))
            viewModel.attachExpenseTextWatcher(etAmount)
            
            // Initialize payment method toggle (default to cash)
            setupPaymentMethodToggle()
        }
    }
    
    private fun setupPaymentMethodToggle() {
        binding?.apply {
            // Set initial state based on paymentMethod (default to cash)
            resetPaymentMethodBackgrounds()
            selectPaymentMethod(paymentMethod)
            
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
        binding?.apply {
            ivClose.setOnClickListener { dismiss() }

            btnSave.setOnClickListener {
                addOrUpdateExpenseAmount(etAmount.text.toString().trim())
                recordClickEvent(
                    ConstantEventNames.SAVE_INCOME,
                    hashMapOf(
                        Pair(ConstantEventAttributes.INCOME_TYPE, transactionType),
                        Pair(ConstantEventAttributes.AMOUNT, amount.toString()),
                        Pair(ConstantEventAttributes.IS_UPDATE_INCOME, isUpdate.toString()),
                        Pair(ConstantEventAttributes.DATE, date.toString())
                    )
                )
            }

            btnDelete.setOnClickListener {
                showDeleteConfirmationDialog()
                recordClickEvent(
                    ConstantEventNames.DELETE_INCOME_TRY,
                    hashMapOf(
                        Pair(ConstantEventAttributes.INCOME_TYPE, transactionType),
                        Pair(ConstantEventAttributes.AMOUNT, amount.toString()),
                        Pair(ConstantEventAttributes.IS_UPDATE_INCOME, isUpdate.toString())
                    )
                )
            }

            tvDateEdit.setOnClickListener {
                openDatePicker()
                recordClickEvent(ConstantEventNames.EDIT_INCOME_DATE)
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_expense))
            .setMessage(getString(R.string.are_you_sure_you_want_to_delete_this_expense))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                id?.let {
                    viewModel.deleteExpense(it)
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        date?.let {
            try {
                calendar.time = sdf.parse(it) ?: Date()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                date = formatToISO(calendar)
                binding?.tvDate?.text = date?.toReadableDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatToISO(calendar: Calendar): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        return isoFormat.format(calendar.time)
    }

    private fun addOrUpdateExpenseAmount(amount: String) {
        reason = binding?.etDescription?.text.toString()
        this.amount = amount

        if(reason.isNullOrEmpty()) reason = "Income"

        // ✅ Guard against empty or invalid amount input
        val validAmount = amount.toDoubleOrNull()
        if (validAmount == null) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Check for maximum 8 digits
        val digitCount = amount.replace(".", "").replace("-", "").length
        if (digitCount > 8) {
            Toast.makeText(context, "Maximum 8 digits allowed", Toast.LENGTH_SHORT).show()
            return
        }

        val transactionRequest = TransactionRequest(
            reason ?: "Income",
            date.toString(),
            validAmount,
            transactionType,
            if (paymentMethod == "online") "UPI" else "CASH", // Set transaction_mode based on payment method
            paymentMethod
        )

        if (isUpdate) {
            id?.let { viewModel.updateExpense(it, transactionRequest) }
        } else {
            viewModel.createExpense(transactionRequest)
        }
    }

    /**
     * InputFilter to limit the number of digits in the EditText
     */
    private class MaxDigitsInputFilter(private val maxDigits: Int) : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val existingText = dest?.toString() ?: ""
            val newText = existingText.substring(0, dstart) + source?.subSequence(start, end) + existingText.substring(dend)
            
            // Remove decimal point and negative sign for digit count
            val digitCount = newText.replace(".", "").replace("-", "").length
            
            return if (digitCount > maxDigits) {
                ""
            } else {
                null // Accept the input
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            id: String,
            transactionType: String,
            date: String,
            amount: String,
            reason: String,
            paymentMethod: String? = null,
        ) =
            CashInOutBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ID, id)
                    putString(TRANSACTION_TYPE, transactionType)
                    putString(DATE, date)
                    putString(AMOUNT, amount)
                    putString(REASON, reason)
                    putString(PAYMENT_METHOD, paymentMethod)
                }
            }
    }
}