package com.dailybook.keep.screen.advance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.dailybook.base.BaseBottomsheetFragment
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.base.toFormattedDate
import com.dailybook.keep.R
import com.dailybook.keep.databinding.FragmentAdvanceDetailsBottomsheetBinding
import com.dailybook.keep.screen.calendar.utils.Constants
import java.util.Locale

private const val ARG_ID = "id"
private const val ARG_NAME = "name"
private const val ARG_DATE = "date"
private const val ARG_ADVANCE = "advance"
private const val ARG_REASON = "reason"
private const val ARG_ATTENDANCE_STATUS = "attendance_status"
private const val ARG_OT_MINUTES = "ot_minutes"
private const val ARG_OT_PER_HOUR = "ot_per_hour"
private const val ARG_OT_TOTAL_AMOUNT = "ot_total_amount"
private const val ARG_PAYMENT_METHOD = "advance_payment_method"

class AdvanceDetailsBottomsheetFragment : BaseBottomsheetFragment<FragmentAdvanceDetailsBottomsheetBinding>() {

    override val screenName: String
        get() = ConstantEventNames.ADVANCE_BS

    private var id: String? = null
    private var name: String? = null
    private var date: String? = null
    private var advance: String? = null
    private var reason: String? = null
    private var attendanceStatus: String? = null
    private var otMinutes: Double? = null
    private var otPerHour: Double? = null
    private var otTotalAmount: Double? = null
    private var paymentMethod: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            id = it.getString(ARG_ID)
            name = it.getString(ARG_NAME)
            date = it.getString(ARG_DATE)
            advance = it.getString(ARG_ADVANCE)
            reason = it.getString(ARG_REASON)
            attendanceStatus = it.getString(ARG_ATTENDANCE_STATUS)
            otMinutes = it.getDouble(ARG_OT_MINUTES, 0.0)
            otPerHour = it.getDouble(ARG_OT_PER_HOUR, 0.0)
            otTotalAmount = it.getDouble(ARG_OT_TOTAL_AMOUNT, 0.0)
            paymentMethod = it.getString(ARG_PAYMENT_METHOD)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentAdvanceDetailsBottomsheetBinding? {
        return FragmentAdvanceDetailsBottomsheetBinding.inflate(inflater, container, false)
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
            tvHeaderDate.text = date?.toFormattedDate()
            val status = attendanceStatus?.trim().orEmpty()
            if (status.isEmpty() || status == Constants.ATTENDANCE_STATUS_NULL) {
                tvAttendance.visibility = View.GONE
            } else {
                tvAttendance.visibility = View.VISIBLE
                val drawableRes = when (status) {
                    Constants.ATTENDANCE_STATUS_ABSENT -> R.drawable.ic_absent_filled
                    Constants.ATTENDANCE_STATUS_PRESENT -> R.drawable.ic_present_filled
                    Constants.ATTENDANCE_STATUS_HALF_PRESENT -> R.drawable.ic_present_half_filled
                    Constants.ATTENDANCE_STATUS_ONE_AND_HALF_PRESENT -> R.drawable.ic_present_one_and_half_filled
                    Constants.ATTENDANCE_STATUS_DOUBLE_PRESENT -> R.drawable.ic_double_present_filled
                    Constants.ATTENDANCE_STATUS_PAID_LEAVE -> R.drawable.ic_paid_leave_filled
                    else -> R.drawable.ic_present_empty
                }
                tvAttendance.setImageDrawable(ContextCompat.getDrawable(requireContext(), drawableRes))
            }
            tvAdvanceAmount.text = if (advance.isNullOrEmpty() || advance == "0") {
                getString(R.string._0)
            } else {
                getString(R.string.rupee).plus(" ").plus(advance)
            }
            // Overtime row visibility and value
            val hasOt = (otTotalAmount ?: 0.0) > 0.0 || (otMinutes ?: 0.0) > 0.0 || (otPerHour ?: 0.0) > 0.0
            if (hasOt) {
                tvOtLabel.visibility = View.VISIBLE
                tvOtValue.visibility = View.VISIBLE
                val total = otTotalAmount ?: 0.0
                tvOtValue.text = when {
                    total > 0.0 -> getString(R.string.rupee).plus(" ").plus(total.toInt())
                    else -> "${otMinutes?.toInt() ?: 0} min @ ${getString(R.string.rupee)} ${(otPerHour ?: 0.0).toInt()}"
                }
                // Always show compact OT tag when OT exists, even if attendance is not marked
                ivOtTag.visibility = View.VISIBLE
            } else {
                tvOtLabel.visibility = View.GONE
                tvOtValue.visibility = View.GONE
                ivOtTag.visibility = View.GONE
            }
            tvNotes.text = reason ?: ""
            tvEdit.text = getString(R.string.edit)
            
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
        }
    }

    private fun setupClicks() {
        binding?.ivClose?.setOnClickListener { dismiss() }
        binding?.btnOk?.setOnClickListener { dismiss() }
        binding?.tvEdit?.setOnClickListener {
            val fragment = PayAdvanceBottomsheetFragment.newInstance(
                id = id ?: "",
                name = name ?: "",
                date = date ?: "",
                advance = advance ?: "",
                reason = reason ?: ""
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

    companion object {
        @JvmStatic
        fun newInstance(
            id: String,
            name: String,
            date: String,
            advance: String,
            reason: String,
            attendanceStatus: String,
            otMinutes: Double,
            otPerHour: Double,
            otTotalAmount: Double,
            paymentMethod: String? = null
        ) = AdvanceDetailsBottomsheetFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ID, id)
                putString(ARG_NAME, name)
                putString(ARG_DATE, date)
                putString(ARG_ADVANCE, advance)
                putString(ARG_REASON, reason)
                putString(ARG_ATTENDANCE_STATUS, attendanceStatus)
                putDouble(ARG_OT_MINUTES, otMinutes)
                putDouble(ARG_OT_PER_HOUR, otPerHour)
                putDouble(ARG_OT_TOTAL_AMOUNT, otTotalAmount)
                putString(ARG_PAYMENT_METHOD, paymentMethod)
            }
        }
    }
}


