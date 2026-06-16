package com.dailybook.expense.screen.details.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.dailybook.base.BaseBottomsheetFragment
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.base.toReadableDate
import com.dailybook.expense.R
import com.dailybook.expense.databinding.FragmentTransactionDetailsBinding
import com.dailybook.expense.screen.cashentry.fragment.CashInOutBottomSheetFragment
import com.dailybook.expense.util.Constants
import com.dailybook.expense.util.ExpenseObserverUtil
import org.koin.android.ext.android.inject
import java.util.Locale

private const val ARG_ID = "id"
private const val ARG_TRANSACTION_TYPE = "transaction_type"
private const val ARG_DATE = "date"
private const val ARG_AMOUNT = "amount"
private const val ARG_REASON = "reason"
private const val ARG_PAYMENT_METHOD = "payment_method"

class TransactionDetailsBottomSheetFragment : BaseBottomsheetFragment<FragmentTransactionDetailsBinding>() {

    override val screenName: String
        get() = ConstantEventNames.EXPENSE_TRANSACTION_DETAILS

    private val expenseObserverUtil: ExpenseObserverUtil by inject()
    private var id: String? = null
    private var transactionType: String? = null
    private var date: String? = null
    private var amount: String? = null
    private var reason: String? = null
    private var paymentMethod: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            id = it.getString(ARG_ID)
            transactionType = it.getString(ARG_TRANSACTION_TYPE)
            date = it.getString(ARG_DATE)
            amount = it.getString(ARG_AMOUNT)
            reason = it.getString(ARG_REASON)
            paymentMethod = it.getString(ARG_PAYMENT_METHOD)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentTransactionDetailsBinding? {
        return FragmentTransactionDetailsBinding.inflate(inflater, container, false)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClicks()
    }

    private fun setupViews() {
        binding?.apply {
            tvHeaderDate.text = date?.toReadableDate()
            
            // Set transaction type title
            tvTitle.text = if (transactionType == Constants.CREDIT) {
                getString(R.string.cash_in)
            } else {
                getString(R.string.cash_out)
            }
            
            // Set amount with color based on transaction type
            val amountText = if (amount.isNullOrEmpty() || amount == "0") {
                getString(R.string._0)
            } else {
                getString(R.string.rupee).plus(" ").plus(amount)
            }
            tvAmount.text = amountText
            
            // Set color: red for DEBIT (cash out), green for CREDIT (cash in)
            val amountColor = if (transactionType == Constants.DEBIT) {
                ContextCompat.getColor(requireContext(), R.color.error_state_color)
            } else {
                ContextCompat.getColor(requireContext(), R.color.button_green_color)
            }
            tvAmount.setTextColor(amountColor)
            
            // Set reason/notes
            tvNotes.text = reason ?: ""
            
            // Payment method visibility and value
            if (!paymentMethod.isNullOrEmpty()) {
                tvPaymentMethodLabel.visibility = View.VISIBLE
                tvPaymentMethodValue.visibility = View.VISIBLE
                // Capitalize first letter for display
                val displayMethod = paymentMethod?.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                } ?: ""
                tvPaymentMethodValue.text = displayMethod
            } else {
                tvPaymentMethodLabel.visibility = View.GONE
                tvPaymentMethodValue.visibility = View.GONE
            }
            
            tvEdit.text = getString(R.string.edit)
        }
    }

    private fun setupClicks() {
        binding?.ivClose?.setOnClickListener { dismiss() }
        binding?.btnOk?.setOnClickListener { dismiss() }
        binding?.tvEdit?.setOnClickListener {
            // Set up observer callbacks before opening edit fragment to ensure list refreshes
            setupObserverCallbacks()
            
            val fragment = CashInOutBottomSheetFragment.newInstance(
                id = id ?: "",
                transactionType = transactionType ?: "",
                date = date ?: "",
                amount = amount ?: "",
                reason = reason ?: "",
                paymentMethod = paymentMethod
            )
            dismiss()
            try {
                // Post with a slight delay to ensure the current sheet is fully dismissed
                requireActivity().window?.decorView?.postDelayed({
                    fragmentNavigator.start(fragment)
                }, 150)
            } catch (_: Exception) {}
        }
    }
    
    private fun setupObserverCallbacks() {
        // Ensure observer callbacks are set up so the list refreshes after updates
        // Find ExpenseFragment in the activity's fragment manager
        val expenseFragment = findExpenseFragment()
        
        expenseFragment?.let { fragment ->
            // Set up callbacks to match what's done in ExpenseFragment.openCashEntryBottomSheet
            expenseObserverUtil.onExpenseAddedOrUpdated = { expense, isUpdate ->
                if (isUpdate) {
                    fragment.handleExpenseUpdate(expense)
                } else {
                    fragment.handleExpenseAddition(expense)
                }
                fragment.updateViewVisibility(loading = false, empty = fragment.allTransactionItems.isEmpty())
                fragment.transactionSummaryViewModel.getTransactionSummary(
                    fragment.monthNumber.toString(), 
                    fragment.currentYear.toString()
                )
            }
            expenseObserverUtil.onExpenseDeleted = { deleteTransaction ->
                fragment.handleExpenseDeletion(deleteTransaction.id)
                fragment.updateViewVisibility(loading = false, empty = fragment.allTransactionItems.isEmpty())
                fragment.transactionSummaryViewModel.getTransactionSummary(
                    fragment.monthNumber.toString(), 
                    fragment.currentYear.toString()
                )
            }
        }
    }
    
    private fun findExpenseFragment(): com.dailybook.expense.screen.home.fragment.ExpenseFragment? {
        // Try to find ExpenseFragment in the activity's fragment manager
        // Bottom sheets are shown as dialogs, so we need to look in the activity's fragment manager
        return try {
            val activity = requireActivity()
            val fragmentManager = activity.supportFragmentManager
            
            // Search in all fragments recursively
            fun findFragmentRecursive(fragments: List<androidx.fragment.app.Fragment>): com.dailybook.expense.screen.home.fragment.ExpenseFragment? {
                for (fragment in fragments) {
                    if (fragment is com.dailybook.expense.screen.home.fragment.ExpenseFragment) {
                        return fragment
                    }
                    if (fragment.childFragmentManager.fragments.isNotEmpty()) {
                        val found = findFragmentRecursive(fragment.childFragmentManager.fragments)
                        if (found != null) return found
                    }
                }
                return null
            }
            
            findFragmentRecursive(fragmentManager.fragments)
        } catch (e: Exception) {
            null
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
            paymentMethod: String? = null
        ) = TransactionDetailsBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ID, id)
                putString(ARG_TRANSACTION_TYPE, transactionType)
                putString(ARG_DATE, date)
                putString(ARG_AMOUNT, amount)
                putString(ARG_REASON, reason)
                putString(ARG_PAYMENT_METHOD, paymentMethod)
            }
        }
    }
}

