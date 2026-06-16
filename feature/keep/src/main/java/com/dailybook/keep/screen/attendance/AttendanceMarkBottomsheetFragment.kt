package com.dailybook.keep.screen.attendance

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.dailybook.base.BaseBottomsheetFragment
import com.dailybook.base.analytics.ConstantEventAttributes
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.base.toFormattedDate
import com.dailybook.keep.R
import com.dailybook.keep.databinding.FragmentAttendanceMarkBottomsheetBinding
import com.dailybook.keep.model.AttendanceItem
import com.dailybook.keep.screen.calendar.fragment.OvertimeBottomSheetFragment
import com.dailybook.keep.screen.calendar.utils.Constants
import com.dailybook.keep.screen.calendar.utils.ObserverUtil
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val NAME = "name"
private const val DATE = "date"
private const val ATTENDANCE_STATUS = "attendance_status"
private const val OT_MINUTES = "ot_minutes"
private const val OT_PER_HOUR = "ot_per_hour"
private const val USER_ID = "user_id"
class AttendanceMarkBottomsheetFragment : BaseBottomsheetFragment<FragmentAttendanceMarkBottomsheetBinding>() {

    override val screenName: String
        get() = ConstantEventNames.ATTENDANCE_BS

    private var attendanceStatus: String? = ""
    private var date: String? = ""
    private var name: String? = ""
    private var userId: String? = null
    private var hasOvertime: Boolean = false
    private var existingOtMinutes: Double = 0.0
    private var existingOtPerHour: Double = 0.0
    private var shiftType: String = "day"
    private var clockInTimestamp: String? = null
    private var clockOutTimestamp: String? = null
    private val observerUtil: ObserverUtil by inject()

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).also {
        it.timeZone = TimeZone.getDefault()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(USER_ID)
            name = it.getString(NAME)
            date = it.getString(DATE)
            attendanceStatus = it.getString(ATTENDANCE_STATUS)
            existingOtMinutes = it.getDouble(OT_MINUTES, 0.0)
            existingOtPerHour = it.getDouble(OT_PER_HOUR, 0.0)
            hasOvertime = existingOtMinutes > 0 && existingOtPerHour > 0
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentAttendanceMarkBottomsheetBinding? {
        return FragmentAttendanceMarkBottomsheetBinding.inflate(inflater,container,false)
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
        setOnClickListeners()
        setViews()
    }

    private fun setViews() {
        binding?.apply {
            btnMarkAttendance.isEnabled = !attendanceStatus.isNullOrEmpty()
            btnRemoveAttendance.isVisible = !attendanceStatus.isNullOrEmpty()
            tvStaffName.text = name
            tvDate.text = date?.toFormattedDate()

            // Shift toggle initial state
            updateShiftToggleUI()
            llClock.isVisible = isPresentStatus(attendanceStatus)

            // Show OT status if it exists
            if (hasOvertime) {
                ivOtPill.setImageResource(R.drawable.ic_ot_pill)
            } else {
                ivOtPill.setImageResource(R.drawable.ic_ot_pill_unfilled)
            }

            // Reset all radio button backgrounds first
            resetRadioButtonBackgrounds()
            
            when(attendanceStatus){
                Constants.ATTENDANCE_STATUS_ABSENT -> {
                    absent.isChecked = true
                    absent.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg_selected)
                    absent.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                Constants.ATTENDANCE_STATUS_HALF_PRESENT -> {
                    halfPresent.isChecked = true
                    halfPresent.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg_selected)
                    halfPresent.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                Constants.ATTENDANCE_STATUS_PRESENT -> {
                    present.isChecked = true
                    present.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg_selected)
                    present.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                Constants.ATTENDANCE_STATUS_ONE_AND_HALF_PRESENT -> {
                    presentHalf.isChecked = true
                    presentHalf.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg_selected)
                    presentHalf.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                Constants.ATTENDANCE_STATUS_DOUBLE_PRESENT -> {
                    doublePresent.isChecked = true
                    doublePresent.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg_selected)
                    doublePresent.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                Constants.ATTENDANCE_STATUS_PAID_LEAVE -> {
                    paidLeave.isChecked = true
                    paidLeave.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg_selected)
                    paidLeave.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
            }
        }
    }

    private fun setOnClickListeners() {
        binding?.apply {
            btnMarkAttendance.setOnClickListener {
                val remarks = etRemarks.text?.toString()?.takeIf { it.isNotBlank() }
                observerUtil.onDailyAttendanceMarkListener?.invoke(
                    AttendanceItem(
                        attendanceDate = date,
                        attendanceStatus = attendanceStatus,
                        shiftType = shiftType,
                        remarks = remarks,
                        clockIn = clockInTimestamp,
                        clockOut = clockOutTimestamp,
                    )
                )
                recordClickEvent(ConstantEventNames.MARK_ATTENDANCE_FROM_BS, hashMapOf(Pair(ConstantEventAttributes.STATUS, attendanceStatus?:"")))
                dismiss()
            }

            btnRemoveAttendance.setOnClickListener {
                attendanceStatus = Constants.ATTENDANCE_STATUS_NULL
                observerUtil.onDailyAttendanceMarkListener?.invoke(AttendanceItem(date, attendanceStatus))
                recordClickEvent(ConstantEventNames.REMOVE_ATTENDANCE_FROM_BS, hashMapOf(Pair(ConstantEventAttributes.STATUS, attendanceStatus?:"")))
                dismiss()
            }

            btnShiftDay.setOnClickListener {
                shiftType = "day"
                updateShiftToggleUI()
            }

            btnShiftNight.setOnClickListener {
                shiftType = "night"
                updateShiftToggleUI()
            }

            btnClockIn.setOnClickListener { showTimePicker(isClockIn = true) }
            btnClockOut.setOnClickListener { showTimePicker(isClockIn = false) }

            ivClose?.setOnClickListener {
                dismiss()
            }

            ivOtPill.setOnClickListener {
                if (hasOvertime) {
                    // If OT is already active, open OT bottom sheet with existing data
                    OvertimeBottomSheetFragment.newInstance(userId ?: "", date ?: "", existingOtMinutes, existingOtPerHour)
                        .show(parentFragmentManager, OvertimeBottomSheetFragment.TAG)
                } else {
                    // If OT is not active, open OT bottom sheet to add new OT
                    OvertimeBottomSheetFragment.newInstance(userId ?: "", date ?: "")
                        .show(parentFragmentManager, OvertimeBottomSheetFragment.TAG)
                }
            }

            // Set individual click listeners for each radio button since they're nested in LinearLayouts
            val radioButtons = listOf(absent, halfPresent, present, presentHalf, doublePresent, paidLeave)
            radioButtons.forEach { radioButton ->
                radioButton.setOnClickListener {
                    val selectedStatus = radioButton.tag.toString()
                    
                    // Reset all radio button backgrounds first
                    resetRadioButtonBackgrounds()
                    
                    // Update the selected radio button background
                    radioButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg_selected)
                    radioButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    
                    // Uncheck all other radio buttons
                    radioButtons.forEach { rb ->
                        if (rb != radioButton) {
                            rb.isChecked = false
                        }
                    }
                    radioButton.isChecked = true

                    // Update attendance status
                    attendanceStatus = selectedStatus
                    btnMarkAttendance.isEnabled = !attendanceStatus.isNullOrEmpty()
                    btnRemoveAttendance.isVisible = !attendanceStatus.isNullOrEmpty()
                    llClock.isVisible = isPresentStatus(attendanceStatus)
                    recordClickEvent(ConstantEventNames.SELECT_ATTENDANCE_FROM_BS, hashMapOf(Pair(ConstantEventAttributes.STATUS, attendanceStatus?:"")))
                }
            }
        }
    }

    private fun isPresentStatus(status: String?): Boolean {
        return status in listOf(
            Constants.ATTENDANCE_STATUS_PRESENT,
            Constants.ATTENDANCE_STATUS_ONE_AND_HALF_PRESENT,
            Constants.ATTENDANCE_STATUS_DOUBLE_PRESENT,
            Constants.ATTENDANCE_STATUS_PAID_LEAVE,
        )
    }

    private fun updateShiftToggleUI() {
        binding?.apply {
            val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg_selected)
            val unselectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg)
            val white = ContextCompat.getColor(requireContext(), android.R.color.white)
            val grey = ContextCompat.getColor(requireContext(), R.color.custom_radio_text_color)
            if (shiftType == "day") {
                btnShiftDay.background = selectedBg
                btnShiftDay.setTextColor(white)
                btnShiftNight.background = unselectedBg
                btnShiftNight.setTextColor(grey)
            } else {
                btnShiftNight.background = selectedBg
                btnShiftNight.setTextColor(white)
                btnShiftDay.background = unselectedBg
                btnShiftDay.setTextColor(grey)
            }
        }
    }

    private fun showTimePicker(isClockIn: Boolean) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                val timestamp = isoFormat.format(picked.time)
                val label = String.format("%02d:%02d", hour, minute)
                if (isClockIn) {
                    clockInTimestamp = timestamp
                    binding?.btnClockIn?.text = getString(R.string.clock_in) + "\n$label"
                } else {
                    clockOutTimestamp = timestamp
                    binding?.btnClockOut?.text = getString(R.string.clock_out) + "\n$label"
                }
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun resetRadioButtonBackgrounds() {
        binding?.apply {
            absent.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg)
            absent.setTextColor(ContextCompat.getColor(requireContext(), R.color.custom_radio_text_color))
            
            halfPresent.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg)
            halfPresent.setTextColor(ContextCompat.getColor(requireContext(), R.color.custom_radio_text_color))
            
            present.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg)
            present.setTextColor(ContextCompat.getColor(requireContext(), R.color.custom_radio_text_color))
            
            presentHalf.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg)
            presentHalf.setTextColor(ContextCompat.getColor(requireContext(), R.color.custom_radio_text_color))
            
            doublePresent.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg)
            doublePresent.setTextColor(ContextCompat.getColor(requireContext(), R.color.custom_radio_text_color))
            
            paidLeave.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_radio_button_bg)
            paidLeave.setTextColor(ContextCompat.getColor(requireContext(), R.color.custom_radio_text_color))
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(userId: String, name: String, date: String, attendance: String, otMinutes: Double = 0.0, otPerHour: Double = 0.0) = AttendanceMarkBottomsheetFragment().apply {
            arguments = Bundle().apply {
                putString(USER_ID, userId)
                putString(NAME, name)
                putString(DATE, date)
                putString(ATTENDANCE_STATUS, attendance)
                putDouble(OT_MINUTES, otMinutes)
                putDouble(OT_PER_HOUR, otPerHour)
            }
        }
    }
}