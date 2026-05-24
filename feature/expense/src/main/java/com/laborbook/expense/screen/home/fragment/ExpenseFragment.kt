package com.laborbook.expense.screen.home.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.laborbook.base.AdUnitConstants
import com.laborbook.base.BaseFragment
import com.laborbook.base.Logger
import com.laborbook.base.analytics.ConstantEventAttributes
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.expense.databinding.FragmentExpenseBinding
import com.laborbook.expense.model.Transaction
import com.laborbook.expense.model.TransactionItem
import com.laborbook.expense.model.TransactionsResponseModel
import com.laborbook.expense.screen.cashentry.fragment.CashInOutBottomSheetFragment
import com.laborbook.expense.screen.details.fragment.TransactionDetailsBottomSheetFragment
import com.laborbook.expense.screen.home.adapter.TransactionListAdapter
import com.laborbook.expense.screen.home.uistate.TransactionUiState
import com.laborbook.expense.screen.home.viewmodel.TransactionSummaryViewModel
import com.laborbook.expense.screen.home.viewmodel.TransactionsViewModel
import com.laborbook.expense.screen.transactionstatus.fragment.TransactionStatusFragment
import com.laborbook.expense.util.Constants
import com.laborbook.expense.util.ExpenseObserverUtil
import com.laborbook.expense.util.Utils
import com.laborbook.expense.screen.monthchooser.MonthYearChooserFragment
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.base.datastore.shouldShowGoogleAds
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseFragment : BaseFragment<FragmentExpenseBinding>() {

    private lateinit var calendar: Calendar
    private val viewModel: TransactionsViewModel by viewModel()
    private val expenseObserverUtil: ExpenseObserverUtil by inject()
    private var expenseAdapter: TransactionListAdapter? = null
    private val adUnitId = AdUnitConstants.NativeAds.EXPENSE_LIST
    private var currentPage = 1
    private var isLastPage = false
    internal val allTransactionItems = mutableListOf<TransactionItem>()
    internal var currentYear: Int = 2024
    private var currentDate: Int = 1
    private var monthName: String? = "Jan"
    internal var monthNumber: Int = 1
    private var isFetching = false
    internal val transactionSummaryViewModel: TransactionSummaryViewModel by viewModel()
    var onSummaryLoaded: ((Double) -> Unit)? = null

    override val screenName: String
        get() = ConstantEventNames.EXPENSE

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentExpenseBinding? {
        return FragmentExpenseBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            // Only initialize ads if user is not Pro and ads are enabled
            if (dataStoreManager.shouldShowGoogleAds()) {
                MobileAds.initialize(requireContext())
            }
        }
        getCurrentDateInfo()
        setClickListeners()
        setupView()
        setupRecyclerView()
        observeViewModel()
        loadInitialData()
        setupSearchListener()
        
        // Observe Pro status changes to remove ads immediately when user upgrades
        observeProStatusChanges()
    }
    
    /**
     * Observe Pro status changes and remove ads from adapter when user becomes Pro
     */
    private fun observeProStatusChanges() {
        dataStoreManager.read(DataStoreManager.PRO_STATUS, false)
            .onEach { isPro ->
                if (isPro && expenseAdapter != null) {
                    // User became Pro - remove all ads from adapter immediately
                    expenseAdapter?.removeAllAds()
                }
            }
            .launchIn(lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        expenseAdapter?.releaseAds()
        viewModel.clearState()
    }

    private fun setupSearchListener() {
        binding?.etSearchExpense?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // No action needed
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim().lowercase()
                filterItems(query)
            }
        })
    }

    private fun updateAdapterWithTransactions(forceRefreshAds: Boolean = false) {
        val transactions = allTransactionItems
            .filterIsInstance<TransactionItem.TransactionItemView>()
            .map { it.transaction }
        expenseAdapter?.submitOriginalList(transactions, forceRefreshAds)
        expenseAdapter?.submitList(allTransactionItems.toList())
    }

    private fun filterItems(query: String) {
        try {
            if (query.isEmpty()) {
                // If query is empty, show original items
                updateAdapterWithTransactions()
            } else {
                // Filter items based on the query and update adapter
                val filteredItems = allTransactionItems.filter {
                    it is TransactionItem.TransactionItemView && it.transaction.reason.lowercase()
                        .contains(query)
                }
                expenseAdapter?.submitList(filteredItems)
            }
        }catch (e: Exception){}
    }

    private fun loadInitialData() {
        // Clear the existing data to prevent duplication
        resetData()
        viewModel.getTransactions(monthNumber.toString(), currentYear.toString(), pageNo = currentPage)
        transactionSummaryViewModel.getTransactionSummary(monthNumber.toString(), currentYear.toString())
    }

    private fun resetData() {
        // Clear lists and reset fields for new data load
        allTransactionItems.clear()
        currentPage = 1
        isLastPage = false
        isFetching = false
        binding?.etSearchExpense?.setText("") // Reset search field
    }

    private fun setupView() {
        binding?.apply {
            if (hideHeader) {
                tvExpense.visibility = View.GONE
                tvMonthYear.visibility = View.GONE
            } else {
                tvMonthYear.text = monthName?.take(3).plus(" ").plus(currentYear.toString())
            }
        }
    }

    private fun setClickListeners() {
        binding?.apply {
            btnCashOut.setOnClickListener {
                openCashEntryBottomSheet("", Constants.DEBIT, Utils.getCurrentTimeInISOFormat(), "", "")
                recordClickEvent(ConstantEventNames.CASH_OUT, hashMapOf(Pair(ConstantEventAttributes.EXPENSE_TYPE, Constants.DEBIT)))
            }

            tvMonthYear.setOnClickListener {
                openMonthYearChooser()
                recordClickEvent(ConstantEventNames.CHANGE_EXPENSE_MONTH, hashMapOf(Pair(ConstantEventAttributes.CURRENT_MONTH, tvMonthYear.text.toString())))
            }

            tvViewReports.setOnClickListener {
                openReportsFragment()
                recordClickEvent(ConstantEventNames.VIEW_REPORTS)
            }
        }
    }

    private fun openReportsFragment() {
        val transactionList = allTransactionItems
            .filterIsInstance<TransactionItem.TransactionItemView>()
            .map { it.transaction }
            .filter { it.type == Constants.DEBIT } // Filter by transaction type
        
        val transactionsArrayList = ArrayList(transactionList)
        fragmentNavigator.start(
            com.laborbook.expense.screen.reports.fragment.TransactionReportsFragment.newInstance(
                transactionsArrayList,
                Constants.DEBIT,
                monthNumber,
                currentYear
            )
        )
    }

    private fun setupRecyclerView() {
        expenseAdapter = TransactionListAdapter(
            { transaction, position ->
                openTransactionDetails(transaction.id, transaction.type, transaction.date, transaction.amount.toString(), transaction.reason, transaction.paymentMethod)
                recordClickEvent(ConstantEventNames.VIEW_EXPENSE, hashMapOf(Pair(ConstantEventAttributes.EXPENSE_TYPE, transaction.type)))
            },
            {
                if (!isLastPage && !isFetching) { // Trigger load more only if not fetching
                    isFetching = true
                    loadMoreData()
                }
            },
            adUnitId
        )

        binding?.rvExpense?.adapter = expenseAdapter
    }

    private fun observeViewModel() {
        viewModel.uiState().observe(viewLifecycleOwner) { state ->
            when (state) {
                is TransactionUiState.LOADING -> if (currentPage == 1) updateViewVisibility(loading = true)
                is TransactionUiState.SUCCESS -> {
                    updateViewVisibility(loading = false, empty = state.data?.transactions.isNullOrEmpty() && currentPage == 1)
                    isFetching = false
                    if (state.data?.transactions?.isNotEmpty() == true) {
                        val newItems = groupExpensesByDate(state.data)
                        appendNewItems(newItems)
                        isLastPage = state.data.isLastPage
                        // Refresh ads on first page load (initial load or pull-to-refresh)
                        if (currentPage == 1) {
                            updateAdapterWithTransactions(forceRefreshAds = true)
                        }
                    }
                }
                is TransactionUiState.ERROR -> {
                    updateViewVisibility(loading = false, empty = true)
                    isFetching = false
                }
            }
        }

        transactionSummaryViewModel.uiState().observe(viewLifecycleOwner) { state ->
            when (state) {
                is TransactionUiState.SUCCESS -> {
                    binding?.apply {
                        tvTotalCashOut.text = "₹ ${state.data?.totalDebit ?: 0}"
                        tvTotalEntries.text = "${state.data?.totalEntriesCount ?: 0}"
                    }
                    onSummaryLoaded?.invoke(state.data?.totalDebit ?: 0.0)
                }
                is TransactionUiState.LOADING -> {}
                is TransactionUiState.ERROR -> {}
            }
        }

        expenseObserverUtil.clearExpenseSearchText = {
            if(it && binding?.etSearchExpense?.text?.isNotEmpty() == true) {
                binding?.etSearchExpense?.setText("")
            }
        }
    }

    internal fun updateViewVisibility(loading: Boolean = false, empty: Boolean = false) {
        binding?.apply {
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            rvExpense.visibility = if (loading || empty) View.GONE else View.VISIBLE
            llTableHeader.visibility = if (loading || empty) View.GONE else View.VISIBLE
            dividerHeader.visibility = if (loading || empty) View.GONE else View.VISIBLE

            if (empty) {
                showEmptyStateViews()
            } else {
                hideEmptyStateViews()
            }
        }
    }

    private fun showEmptyStateViews() {
        binding?.apply {
            tvAddFirstEntry.show()
            ivArrow.show()
        }
    }

    private fun hideEmptyStateViews() {
        binding?.apply {
            tvAddFirstEntry.hide()
            ivArrow.hide()
        }
    }

    private fun loadMoreData() {
        if (!isLastPage) {
            viewModel.getTransactions(monthNumber.toString(), currentYear.toString(), ++currentPage)
        }
    }

    private fun groupExpensesByDate(response: TransactionsResponseModel): List<TransactionItem> {
        val items = mutableListOf<TransactionItem>()

        // Add all transactions directly without date headers
        response.transactions.forEach { expense ->
            items.add(TransactionItem.TransactionItemView(expense))
        }
        return items
    }

    private fun appendNewItems(newItems: List<TransactionItem>) {
        // Add all new transaction items directly without date headers
        val newTransactions = newItems.filterIsInstance<TransactionItem.TransactionItemView>()
        allTransactionItems.addAll(newTransactions)

        // Notify the adapter with the updated list.
        updateAdapterWithTransactions()
    }

    private fun openTransactionDetails(id: String, transactionType: String, date: String, amount: String, reason: String, paymentMethod: String? = null) {
        // Set up observer callbacks before opening details fragment to ensure they're ready
        // These callbacks will be used when editing from the details fragment
        expenseObserverUtil.onExpenseAddedOrUpdated = { expense, isUpdate ->
            if(isUpdate) {
                handleExpenseUpdate(expense)
            } else {
                handleExpenseAddition(expense)
            }
            updateViewVisibility(loading = false, empty = allTransactionItems.isEmpty())
            transactionSummaryViewModel.getTransactionSummary(monthNumber.toString(), currentYear.toString())
        }
        expenseObserverUtil.onExpenseDeleted = { deleteTransaction ->
            handleExpenseDeletion(deleteTransaction.id)
            updateViewVisibility(loading = false, empty = allTransactionItems.isEmpty())
            transactionSummaryViewModel.getTransactionSummary(monthNumber.toString(), currentYear.toString())
        }
        
        fragmentNavigator.start(TransactionDetailsBottomSheetFragment.newInstance(id, transactionType, date, amount, reason, paymentMethod))
    }

    private fun openCashEntryBottomSheet(id: String, transactionType: String, date: String, amount: String, reason: String, paymentMethod: String? = null) {
        fragmentNavigator.start(CashInOutBottomSheetFragment.newInstance(id, transactionType, date, amount, reason, paymentMethod))
        expenseObserverUtil.onExpenseAddedOrUpdated = { expense, isUpdate ->
            if(isUpdate) {
                handleExpenseUpdate(expense)
            } else {
                handleExpenseAddition(expense)
            }
            updateViewVisibility(loading = false, empty = allTransactionItems.isEmpty())
            transactionSummaryViewModel.getTransactionSummary(monthNumber.toString(), currentYear.toString())
            lifecycleScope.launch {
                delay(500)
                fragmentNavigator.start(TransactionStatusFragment.newInstance(expense.type, expense.amount.toString(), isUpdate))
            }
        }
        expenseObserverUtil.onExpenseDeleted = { deleteTransaction ->
            handleExpenseDeletion(deleteTransaction.id)
            updateViewVisibility(loading = false, empty = allTransactionItems.isEmpty())
            transactionSummaryViewModel.getTransactionSummary(monthNumber.toString(), currentYear.toString())
        }
    }

    internal fun handleExpenseAddition(transaction: Transaction) {
        val newExpense = TransactionItem.TransactionItemView(transaction)

        // Add the new expense at the top of the list
        allTransactionItems.add(0, newExpense)

        // Update the adapter with the modified list
        updateAdapterWithTransactions()

        // Use postDelayed to add a delay before scrolling smoothly to the 0th position
        binding?.rvExpense?.postDelayed({
            if (isAdded && context != null) {
                binding?.rvExpense?.smoothScrollToPosition(0)
            }
        }, 500) // 500ms delay before scrolling
    }

    internal fun handleExpenseUpdate(transaction: Transaction) {
        val updatedExpense = TransactionItem.TransactionItemView(transaction)

        // Find the index of the expense item that needs to be updated based on its ID
        val expenseIndex = allTransactionItems.indexOfFirst {
            it is TransactionItem.TransactionItemView && it.transaction.id == transaction.id
        }

        if (expenseIndex != -1) {
            // Update the existing expense item in the list
            allTransactionItems[expenseIndex] = updatedExpense

            // Update the adapter with the modified list
            expenseAdapter?.submitList(allTransactionItems.toList())

            // Use postDelayed to add a delay before scrolling smoothly to the updated expense's position
            binding?.rvExpense?.postDelayed({
                if (isAdded && context != null) {
                    binding?.rvExpense?.smoothScrollToPosition(expenseIndex)
                }
            }, 500) // 500ms delay before scrolling
        } else {
            // If the expense doesn't exist, consider handling it or logging it (optional)
            Logger.i("Expense with ID: ${transaction.id} not found for update.")
        }
    }

    internal fun handleExpenseDeletion(expenseId: String) {
        // Find the position of the expense item that needs to be deleted
        val position = allTransactionItems.indexOfFirst { it is TransactionItem.TransactionItemView && it.transaction.id == expenseId }

        if (position != -1) {
            // Remove the expense item
            allTransactionItems.removeAt(position)

            // Update the adapter with the modified list
            expenseAdapter?.submitList(allTransactionItems.toList())
        }
    }

    private fun getCurrentDateInfo() {
        calendar = Calendar.getInstance()
        monthNumber = calendar.get(Calendar.MONTH) + 1
        monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        currentYear = calendar.get(Calendar.YEAR)
        currentDate = calendar.get(Calendar.DATE)
    }

    private fun openMonthYearChooser() {
        val monthChooserFragment = MonthYearChooserFragment.newInstance(monthNumber - 1, currentYear)
        monthChooserFragment.setOnSelectionCallback { selectedMonth, selectedYear ->
            monthNumber = selectedMonth + 1
            currentYear = selectedYear
            monthName = Calendar.getInstance().apply {
                clear() // Clear all fields to avoid unexpected carry-over from previous state
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.YEAR, selectedYear) // Explicitly set the year
            }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())

            binding?.tvMonthYear?.text = getFormattedMonthYear()
            changeMonthAndGetExpenses()
        }
        monthChooserFragment.show(parentFragmentManager, "MonthYearChooserFragment")
    }

    private fun getFormattedMonthYear(): String {
        return "${monthName?.take(3)} $currentYear"
    }

    private fun changeMonthAndGetExpenses() {
        resetData()
        transactionSummaryViewModel.getTransactionSummary(
            monthNumber.toString(),
            currentYear.toString()
        )
        viewModel.getTransactions(
            monthNumber.toString(),
            currentYear.toString(),
            pageNo = currentPage
        )
    }
    
    fun changeMonth(selectedMonth: Int, selectedYear: Int) {
        monthNumber = selectedMonth + 1
        currentYear = selectedYear
        monthName = Calendar.getInstance().apply {
            clear()
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.YEAR, selectedYear)
        }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        changeMonthAndGetExpenses()
    }

    companion object {
        private const val ARG_HIDE_HEADER = "hide_header"
        
        @JvmStatic
        fun newInstance() = ExpenseFragment()
        
        @JvmStatic
        fun newInstance(hideHeader: Boolean) = ExpenseFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_HIDE_HEADER, hideHeader)
            }
        }
    }
    
    private var hideHeader: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideHeader = arguments?.getBoolean(ARG_HIDE_HEADER, false) ?: false
    }
}