package co.dailybook.income.screen.details.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import co.dailybook.base.BaseBottomsheetFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.base.toReadableDate
import co.dailybook.income.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import co.dailybook.income.databinding.FragmentTransactionDetailsBinding
import co.dailybook.income.screen.cashentry.fragment.CashInOutBottomSheetFragment
import co.dailybook.income.util.Constants
import co.dailybook.income.util.IncomeObserverUtil
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
        get() = ConstantEventNames.INCOME_TRANSACTION_DETAILS

    private val incomeObserverUtil: IncomeObserverUtil by inject()
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
            
            // Check privacy mode for income (CREDIT) transactions
            if (transactionType == Constants.CREDIT && !amount.isNullOrEmpty() && amount != "0") {
                lifecycleScope.launch {
                    val isPrivacyModeEnabled = dataStoreManager.read(DataStoreManager.PRIVACY_MODE_ENABLED, false).first()
                    
                    if (isPrivacyModeEnabled) {
                        // Hide amount and show secret icon
                        tvAmount.visibility = View.GONE
                        ivSecretAmount.visibility = View.VISIBLE
                        // Make edit button visually disabled but keep it enabled so click works
                        tvEdit.alpha = 0.5f
                    } else {
                        // Show amount with color
                        tvAmount.visibility = View.VISIBLE
                        ivSecretAmount.visibility = View.GONE
                        
                        val amountText = getString(R.string.rupee).plus(" ").plus(amount)
                        tvAmount.text = amountText
                        
                        // Set color: green for CREDIT (cash in)
                        val amountColor = ContextCompat.getColor(requireContext(), R.color.button_green_color)
                        tvAmount.setTextColor(amountColor)
                        // Make edit button fully visible
                        tvEdit.alpha = 1.0f
                    }
                }
            } else {
                // For DEBIT or zero amount, show amount normally
                tvAmount.visibility = View.VISIBLE
                ivSecretAmount.visibility = View.GONE
                
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
                // Make edit button fully visible for non-income transactions
                tvEdit.alpha = 1.0f
            }
            
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
            // Check if this is an income transaction and privacy mode is enabled
            if (transactionType == Constants.CREDIT && !amount.isNullOrEmpty() && amount != "0") {
                lifecycleScope.launch {
                    val isPrivacyModeEnabled = dataStoreManager.read(DataStoreManager.PRIVACY_MODE_ENABLED, false).first()
                    
                    if (isPrivacyModeEnabled) {
                        // Show toast message to unhide income on main thread
                        withContext(Dispatchers.Main) {
                            val context = context ?: requireContext()
                            Toast.makeText(
                                context,
                                getString(R.string.unhide_income_to_edit),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }
                    
                    // Privacy mode is disabled, proceed with edit
                    openEditFragment()
                }
            } else {
                // Not an income transaction or zero amount, proceed with edit
                openEditFragment()
            }
        }
    }
    
    private fun openEditFragment() {
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
    
    private fun setupObserverCallbacks() {
        // Ensure observer callbacks are set up so the list refreshes after updates
        // Find IncomeFragment in the activity's fragment manager
        val incomeFragment = findIncomeFragment()
        
        incomeFragment?.let { fragment ->
            // Set up callbacks to match what's done in IncomeFragment.openCashEntryBottomSheet
            incomeObserverUtil.onIncomeAddedOrUpdated = { expense, isUpdate ->
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
            incomeObserverUtil.onIncomeDeleted = { deleteTransaction ->
                fragment.handleExpenseDeletion(deleteTransaction.id)
                fragment.updateViewVisibility(loading = false, empty = fragment.allTransactionItems.isEmpty())
                fragment.transactionSummaryViewModel.getTransactionSummary(
                    fragment.monthNumber.toString(), 
                    fragment.currentYear.toString()
                )
            }
        }
    }
    
    private fun findIncomeFragment(): co.dailybook.income.screen.home.fragment.IncomeFragment? {
        // Try to find IncomeFragment in the activity's fragment manager
        // Bottom sheets are shown as dialogs, so we need to look in the activity's fragment manager
        return try {
            val activity = requireActivity()
            val fragmentManager = activity.supportFragmentManager
            
            // Search in all fragments recursively
            fun findFragmentRecursive(fragments: List<androidx.fragment.app.Fragment>): co.dailybook.income.screen.home.fragment.IncomeFragment? {
                for (fragment in fragments) {
                    if (fragment is co.dailybook.income.screen.home.fragment.IncomeFragment) {
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

