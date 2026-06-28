package co.dailybook.keep.screen.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import co.dailybook.base.BaseFragment
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.expense.screen.home.fragment.ExpenseFragment
import co.dailybook.income.screen.home.fragment.IncomeFragment
import co.dailybook.keep.R as KeepR
import co.dailybook.keep.databinding.FragmentCashbookBinding
import co.dailybook.keep.screen.monthchooser.MonthYearChooserFragment
import java.util.Calendar
import java.util.Locale

class CashbookFragment : BaseFragment<FragmentCashbookBinding>() {

    private var currentYear: Int = 2024
    private var monthName: String? = "Jan"
    private var monthNumber: Int = 1
    private var currentExpenseFragment: ExpenseFragment? = null
    private var currentIncomeFragment: IncomeFragment? = null

    private var totalCashOut: Double = 0.0
    private var totalCashIn: Double = 0.0

    override val screenName: String
        get() = ConstantEventNames.EXPENSE

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentCashbookBinding? {
        return FragmentCashbookBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCurrentDateInfo()
        setupView()
        setupViewPager()
        setupTabs()
        setClickListeners()
    }

    private fun getCurrentDateInfo() {
        val calendar = Calendar.getInstance()
        monthNumber = calendar.get(Calendar.MONTH) + 1
        monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        currentYear = calendar.get(Calendar.YEAR)
    }

    private fun setupView() {
        binding?.apply {
            tvMonthYear.text = monthName?.take(3).plus(" ").plus(currentYear.toString())
        }
    }

    private fun setClickListeners() {
        binding?.tvMonthYear?.setOnClickListener {
            openMonthYearChooser()
        }
    }

    private fun openMonthYearChooser() {
        val monthChooserFragment = MonthYearChooserFragment.newInstance(monthNumber - 1, currentYear)
        monthChooserFragment.setOnSelectionCallback { selectedMonth, selectedYear ->
            monthNumber = selectedMonth + 1
            currentYear = selectedYear
            monthName = Calendar.getInstance().apply {
                clear()
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.YEAR, selectedYear)
            }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())

            binding?.tvMonthYear?.text = getFormattedMonthYear()
            
            // Notify child fragments about month change
            notifyFragmentsMonthChanged()
        }
        monthChooserFragment.show(parentFragmentManager, "MonthYearChooserFragment")
    }

    private fun getFormattedMonthYear(): String {
        return "${monthName?.take(3)} $currentYear"
    }

    private fun notifyFragmentsMonthChanged() {
        // Notify both fragments about the month change and reset summary
        totalCashIn = 0.0
        totalCashOut = 0.0
        updateBalanceSummary()
        currentExpenseFragment?.changeMonth(monthNumber - 1, currentYear)
        currentIncomeFragment?.changeMonth(monthNumber - 1, currentYear)
    }

    fun onExpenseSummaryLoaded(totalDebit: Double) {
        totalCashOut = totalDebit
        updateBalanceSummary()
    }

    fun onIncomeSummaryLoaded(totalCredit: Double) {
        totalCashIn = totalCredit
        updateBalanceSummary()
    }

    private fun updateBalanceSummary() {
        val balance = totalCashIn - totalCashOut
        binding?.tvTotalCashIn?.text = "₹${totalCashIn.toLong()}"
        binding?.tvTotalCashOutSummary?.text = "₹${totalCashOut.toLong()}"
        binding?.tvBalance?.apply {
            text = "₹${balance.toLong()}"
            val balanceColor = if (balance >= 0)
                androidx.core.content.ContextCompat.getColor(requireContext(), co.dailybook.keep.R.color.green)
            else
                androidx.core.content.ContextCompat.getColor(requireContext(), co.dailybook.boilerplate.uikit.R.color.button_red_color)
            setTextColor(balanceColor)
        }
    }

    private fun setupViewPager() {
        binding?.viewPager?.adapter = CashbookPagerAdapter(this)
        binding?.viewPager?.isUserInputEnabled = true // Enable swipe
        
        // Update tab UI when page changes
        binding?.viewPager?.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabUI(position)
            }
        })
    }

    private fun setupTabs() {
        binding?.apply {
            btnExpenseTab.setOnClickListener {
                switchToTab(0)
            }
            btnIncomeTab.setOnClickListener {
                switchToTab(1)
            }
            // Set initial state after views are measured
            btnExpenseTab.post {
            updateTabUI(0)
            }
        }
    }

    private fun switchToTab(position: Int) {
        binding?.viewPager?.currentItem = position
        updateTabUI(position)
    }

    private fun updateTabUI(position: Int) {
        binding?.apply {
            if (position == 0) {
                // Expense tab selected - blue text with blue underline
                btnExpenseTab.setTextColor(ContextCompat.getColor(requireContext(), KeepR.color.color_primary))
                updateUnderlineWidth(vExpenseUnderline, btnExpenseTab)
                vExpenseUnderline.visibility = View.VISIBLE
                // Income tab unselected - grey text, no underline
                btnIncomeTab.setTextColor(ContextCompat.getColor(requireContext(), KeepR.color.tab_unselected_text_color))
                vIncomeUnderline.visibility = View.GONE
            } else {
                // Income tab selected - blue text with blue underline
                btnIncomeTab.setTextColor(ContextCompat.getColor(requireContext(), KeepR.color.color_primary))
                updateUnderlineWidth(vIncomeUnderline, btnIncomeTab)
                vIncomeUnderline.visibility = View.VISIBLE
                // Expense tab unselected - grey text, no underline
                btnExpenseTab.setTextColor(ContextCompat.getColor(requireContext(), KeepR.color.tab_unselected_text_color))
                vExpenseUnderline.visibility = View.GONE
            }
        }
    }
    
    private fun updateUnderlineWidth(underlineView: View, textView: android.widget.TextView) {
        // Measure the text width (without padding)
        textView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val textWidth = textView.measuredWidth.toFloat()
        
        // Get the text paint to measure actual text width
        val textPaint = textView.paint
        val actualTextWidth = textPaint.measureText(textView.text.toString())
        
        // Set the underline width to match the actual text width
        val layoutParams = underlineView.layoutParams
        layoutParams.width = actualTextWidth.toInt()
        underlineView.layoutParams = layoutParams
    }

    private inner class CashbookPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    ExpenseFragment.newInstance(hideHeader = true).also {
                        currentExpenseFragment = it
                        it.onSummaryLoaded = { totalDebit -> onExpenseSummaryLoaded(totalDebit) }
                    }
                }
                1 -> {
                    IncomeFragment.newInstance(hideHeader = true).also {
                        currentIncomeFragment = it
                        it.onSummaryLoaded = { totalCredit -> onIncomeSummaryLoaded(totalCredit) }
                    }
                }
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = CashbookFragment()
    }
}