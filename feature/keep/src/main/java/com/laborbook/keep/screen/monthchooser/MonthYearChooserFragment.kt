package com.laborbook.keep.screen.monthchooser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.laborbook.base.BaseBottomsheetFragment
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.income.screen.monthchooser.MonthYearChooserFragment
import com.laborbook.keep.R
import com.laborbook.keep.databinding.FragmentMonthYearChooserBinding
import java.util.Calendar

class MonthYearChooserFragment : BaseBottomsheetFragment<FragmentMonthYearChooserBinding>() {

    override val screenName: String
        get() = ConstantEventNames.MONTH_YEAR_BS

    private var selectedMonth: Int = 0 // 0-based index (January = 0)
    private var selectedYear: Int = 0

    private var onSelectionCallback: ((Int, Int) -> Unit)? = null

    companion object {
        fun newInstance(currentMonth: Int, currentYear: Int): MonthYearChooserFragment {
            return MonthYearChooserFragment().apply {
                arguments = Bundle().apply {
                    putInt("currentMonth", currentMonth)
                    putInt("currentYear", currentYear)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val calendar = Calendar.getInstance()
        selectedMonth = arguments?.getInt("currentMonth", calendar.get(Calendar.MONTH)) ?: calendar.get(Calendar.MONTH)
        selectedYear = arguments?.getInt("currentYear", calendar.get(Calendar.YEAR)) ?: calendar.get(Calendar.YEAR)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentMonthYearChooserBinding? {
        return FragmentMonthYearChooserBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setOnClickListeners()
    }

    private fun setupViews() {
        binding?.apply {
            tvMonth.text = getMonthName(selectedMonth)
            tvYear.text = selectedYear.toString()
        }
    }

    private fun setOnClickListeners() {
        binding?.ivClose?.setOnClickListener {
            dismiss()
        }

        binding?.llMonth?.setOnClickListener {
            val months = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.select_month))
                .setSingleChoiceItems(months, selectedMonth) { dialogInterface, index ->
                    selectedMonth = index
                    binding?.tvMonth?.text = months[index]
                    dialogInterface.dismiss()
                }
                .create()
            dialog.show()
        }

        binding?.llYear?.setOnClickListener {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val years = (currentYear - 10..currentYear).map { it.toString() }.toTypedArray()
            val selectedIndex = years.indexOf(selectedYear.toString())
            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.select_year))
                .setSingleChoiceItems(years, selectedIndex) { dialogInterface, index ->
                    selectedYear = years[index].toInt()
                    binding?.tvYear?.text = years[index]
                    dialogInterface.dismiss()
                }
                .create()
            dialog.show()
        }

        binding?.btnOk?.setOnClickListener {
            onSelectionCallback?.invoke(selectedMonth, selectedYear)
            dismiss()
        }
    }

    fun setOnSelectionCallback(callback: (Int, Int) -> Unit) {
        onSelectionCallback = callback
    }

    private fun getMonthName(monthIndex: Int): String {
        return arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )[monthIndex]
    }
}