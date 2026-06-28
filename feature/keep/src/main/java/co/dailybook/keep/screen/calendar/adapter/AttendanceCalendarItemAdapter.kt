package co.dailybook.keep.screen.calendar.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.text.TextUtils
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.dailybook.boilerplate.analytics.AnalyticsPlatforms
import co.dailybook.boilerplate.uikit.views.hide
import co.dailybook.boilerplate.uikit.views.invisible
import co.dailybook.boilerplate.uikit.views.show
import co.dailybook.base.analytics.Analytics
import co.dailybook.base.analytics.ConstantEventAttributes
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.base.analytics.ConstantEventSources
import co.dailybook.base.navigator.FragmentNavigator
import co.dailybook.keep.R
import co.dailybook.keep.databinding.ItemCalendarAttendanceBinding
import co.dailybook.keep.model.AttendanceItem
import co.dailybook.keep.model.CalendarItem
import co.dailybook.keep.screen.advance.PayAdvanceBottomsheetFragment
import co.dailybook.keep.screen.advance.AdvanceDetailsBottomsheetFragment
import co.dailybook.keep.screen.attendance.AttendanceMarkBottomsheetFragment
import co.dailybook.keep.screen.calendar.fragment.OvertimeBottomSheetFragment
import co.dailybook.keep.screen.calendar.utils.Constants
import co.dailybook.keep.screen.calendar.utils.ObserverUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceCalendarItemAdapter : ListAdapter<CalendarItem, AttendanceCalendarItemAdapter.ItemViewHolder>(
    ItemDiffCallback()
) {

    private var staffName: String = ""
    private var staffId: String = ""

    class ItemViewHolder(private val binding: ItemCalendarAttendanceBinding) : RecyclerView.ViewHolder(binding.root), KoinComponent  {

        private val fragmentNavigator: FragmentNavigator by inject()
        private val observerUtil: ObserverUtil by inject()
        private val analytics: Analytics by inject()
        private var isAttendanceMarked = false

        fun bind(calendarItem: CalendarItem, iStaffName: String, iStaffId: String) {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
            val currentDate = dateFormat.format(calendar.time)

            val providedDate = calendarItem.date.take(2)
            if(currentDate.toString() == providedDate) {
                binding.tvDate.hide()
                binding.tvCurrentDate.show()
                binding.tvCurrentDate.text = providedDate
                binding.tvDay.hide()
                binding.tvCurrentDay.show()
                binding.tvCurrentDay.text = calendarItem.day.take(3)
            } else {
                binding.tvCurrentDate.hide()
                binding.tvDate.show()
                binding.tvCurrentDay.hide()
                binding.tvDay.show()
                binding.tvDay.text = calendarItem.day.take(3)
                binding.tvDate.text = providedDate
            }
            setAdvance(calendarItem)
            setAttendance(calendarItem)
            binding.ivMore.setOnClickListener {
                fragmentNavigator.start(AttendanceMarkBottomsheetFragment.newInstance(iStaffId, iStaffName, calendarItem.date, calendarItem.attendanceStatus ?: "", calendarItem.otMinutes ?: 0.0, calendarItem.otPerHour ?: 0.0))
                analytics.logEvent(
                    ConstantEventNames.VIEW_MORE_ATTENDANCE_OPTIONS,
                    Analytics.CLICK,
                    listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                    hashMapOf(Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.MORE)))
            }

            binding.ivAttendanceOne.setOnClickListener {
                if (!isAttendanceMarked) {
                    // If no attendance is marked, clicking first button marks absent
                    observerUtil.onDailyAttendanceMarkListener?.invoke(
                        AttendanceItem(
                            calendarItem.date,
                            Constants.ATTENDANCE_STATUS_ABSENT
                        )
                    )
                    // Update UI immediately to hide OT pill and show attendance
                    val hasOt = (calendarItem.otTotalAmount ?: 0.0) > 0 || (calendarItem.otMinutes ?: 0.0) > 0 || (calendarItem.otPerHour ?: 0.0) > 0
                    if (hasOt) {
                        binding.apply {
                            ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_absent_filled))
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        }
                    }
                    analytics.logEvent(
                        ConstantEventNames.MARK_ATTENDANCE,
                        Analytics.CLICK,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMapOf(
                            Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.ATTENDANCE_BUTTON),
                            Pair(ConstantEventAttributes.STATUS, Constants.ATTENDANCE_STATUS_ABSENT),))
                } else {
                    // If attendance is marked, clicking first button opens attendance bottom sheet
                    fragmentNavigator.start(AttendanceMarkBottomsheetFragment.newInstance(iStaffId, iStaffName, calendarItem.date, calendarItem.attendanceStatus ?: "", calendarItem.otMinutes ?: 0.0, calendarItem.otPerHour ?: 0.0))
                    analytics.logEvent(
                        ConstantEventNames.EDIT_ATTENDANCE,
                        Analytics.CLICK,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMapOf(Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.ATTENDANCE_BUTTON)))
                }
            }

            binding.ivAttendanceTwo.setOnClickListener {
                val hasOt = (calendarItem.otTotalAmount ?: 0.0) > 0 || (calendarItem.otMinutes ?: 0.0) > 0 || (calendarItem.otPerHour ?: 0.0) > 0
                
                if (!isAttendanceMarked) {
                    // If no attendance is marked, clicking second button marks present
                    observerUtil.onDailyAttendanceMarkListener?.invoke(
                        AttendanceItem(
                            calendarItem.date,
                            Constants.ATTENDANCE_STATUS_PRESENT
                        )
                    )
                    // Update UI immediately to hide OT pill and show attendance
                    if (hasOt) {
                        binding.apply {
                            ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_present_filled))
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        }
                    }
                    analytics.logEvent(
                        ConstantEventNames.MARK_ATTENDANCE,
                        Analytics.CLICK,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMapOf(
                            Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.ATTENDANCE_BUTTON),
                            Pair(ConstantEventAttributes.STATUS, Constants.ATTENDANCE_STATUS_PRESENT),))
                } else {
                    // If attendance is marked, clicking second button opens OT bottom sheet (empty or filled)
                    if (hasOt) {
                        // If OT exists, open OT bottom sheet to edit
                        fragmentNavigator.start(OvertimeBottomSheetFragment.newInstance(iStaffId, calendarItem.date, calendarItem.otMinutes ?: 0.0, calendarItem.otPerHour ?: 0.0))
                        analytics.logEvent(
                            ConstantEventNames.EDIT_ATTENDANCE,
                            Analytics.CLICK,
                            listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                            hashMapOf(Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.OT_BUTTON)))
                    } else {
                        // If no OT exists, open OT bottom sheet to add new OT
                        fragmentNavigator.start(OvertimeBottomSheetFragment.newInstance(iStaffId, calendarItem.date))
                        analytics.logEvent(
                            ConstantEventNames.MARK_ATTENDANCE,
                            Analytics.CLICK,
                            listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                            hashMapOf(Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.OT_BUTTON)))
                    }
                }
            }

            // Add click listener for OT pill
            binding.ivOtPill.setOnClickListener {
                val hasOt = (calendarItem.otTotalAmount ?: 0.0) > 0 || (calendarItem.otMinutes ?: 0.0) > 0 || (calendarItem.otPerHour ?: 0.0) > 0
                if (hasOt) {
                    // If OT exists, open OT bottom sheet to edit
                    fragmentNavigator.start(OvertimeBottomSheetFragment.newInstance(iStaffId, calendarItem.date, calendarItem.otMinutes ?: 0.0, calendarItem.otPerHour ?: 0.0))
                    analytics.logEvent(
                        ConstantEventNames.EDIT_ATTENDANCE,
                        Analytics.CLICK,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMapOf(Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.OT_BUTTON)))
                } else {
                    // If no OT exists, open OT bottom sheet to add new OT
                    fragmentNavigator.start(OvertimeBottomSheetFragment.newInstance(iStaffId, calendarItem.date))
                    analytics.logEvent(
                        ConstantEventNames.MARK_ATTENDANCE,
                        Analytics.CLICK,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMapOf(Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.OT_BUTTON)))
                }
            }

            binding.tvAddAdvance.setOnClickListener {
                val hasAdvance = !(calendarItem.advance.isNullOrEmpty() || calendarItem.advance == "0")
                val hasNotes = !calendarItem.reason.isNullOrBlank()
                if (hasAdvance || hasNotes) {
                    fragmentNavigator.start(
                        AdvanceDetailsBottomsheetFragment.newInstance(
                            id = iStaffId,
                            name = iStaffName,
                            date = calendarItem.date,
                            advance = calendarItem.advance ?: "",
                            reason = calendarItem.reason ?: "",
                            attendanceStatus = calendarItem.attendanceStatus ?: "",
                            otMinutes = calendarItem.otMinutes ?: 0.0,
                            otPerHour = calendarItem.otPerHour ?: 0.0,
                            otTotalAmount = calendarItem.otTotalAmount ?: 0.0,
                            paymentMethod = calendarItem.advancePaymentMethod
                        )
                    )
                } else {
                    fragmentNavigator.start(
                        PayAdvanceBottomsheetFragment.newInstance(
                            iStaffId,
                            iStaffName,
                            calendarItem.date,
                            calendarItem.advance?:"",
                            calendarItem.reason?:""
                        )
                    )
                }
                analytics.logEvent(
                    ConstantEventNames.OPEN_ADD_ADVANCE_BS,
                    Analytics.CLICK,
                    listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE))
            }

            binding.ivEdit.setOnClickListener {
                val hasAdvance = !(calendarItem.advance.isNullOrEmpty() || calendarItem.advance == "0")
                val hasNotes = !calendarItem.reason.isNullOrBlank()
                if (hasAdvance || hasNotes) {
                    fragmentNavigator.start(
                        AdvanceDetailsBottomsheetFragment.newInstance(
                            id = iStaffId,
                            name = iStaffName,
                            date = calendarItem.date,
                            advance = calendarItem.advance ?: "",
                            reason = calendarItem.reason ?: "",
                            attendanceStatus = calendarItem.attendanceStatus ?: "",
                            otMinutes = calendarItem.otMinutes ?: 0.0,
                            otPerHour = calendarItem.otPerHour ?: 0.0,
                            otTotalAmount = calendarItem.otTotalAmount ?: 0.0,
                            paymentMethod = calendarItem.advancePaymentMethod
                        )
                    )
                } else {
                    fragmentNavigator.start(
                        PayAdvanceBottomsheetFragment.newInstance(
                            iStaffId,
                            iStaffName,
                            calendarItem.date,
                            calendarItem.advance?:"",
                            calendarItem.reason?:""
                        )
                    )
                }
                analytics.logEvent(
                    ConstantEventNames.OPEN_ADD_ADVANCE_BS,
                    Analytics.CLICK,
                    listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE))
            }
        }

        private fun setAdvance(calendarItem: CalendarItem) {
            val hasAdvanceAmount = !(calendarItem.advance.isNullOrEmpty() || calendarItem.advance == "0")
            val hasNotes = !calendarItem.reason.isNullOrBlank()

            binding.tvAddAdvance.maxLines = 1
            binding.tvAddAdvance.ellipsize = TextUtils.TruncateAt.END

            when {
                hasAdvanceAmount -> {
                    // Show amount with higher priority on calendar row.
                    binding.tvAddAdvance.setTextColor(binding.tvAddAdvance.context.resources.getColor(co.dailybook.boilerplate.uikit.R.color.absent))
                    binding.tvAddAdvance.text = binding.tvAddAdvance.context.getString(R.string.rupee)
                        .plus(" ")
                        .plus(calendarItem.advance)
                }

                hasNotes -> {
                    // If only notes exist, show one-line preview in the table.
                    binding.tvAddAdvance.setTextColor(binding.tvAddAdvance.context.resources.getColor(co.dailybook.boilerplate.uikit.R.color.absent))
                    binding.tvAddAdvance.text = calendarItem.reason?.trim().orEmpty()
                }

                else -> {
                    binding.tvAddAdvance.setTextColor(binding.tvAddAdvance.context.resources.getColor(co.dailybook.boilerplate.uikit.R.color.hint_text_color))
                    binding.tvAddAdvance.text = binding.tvAddAdvance.context.getString(R.string.add_advance)
                }
            }
        }
        


        private fun setAttendance(calendarItem: CalendarItem) {
            val hasOt = (calendarItem.otTotalAmount ?: 0.0) > 0 || (calendarItem.otMinutes ?: 0.0) > 0 || (calendarItem.otPerHour ?: 0.0) > 0
            
            when (calendarItem.attendanceStatus) {
                Constants.ATTENDANCE_STATUS_ABSENT -> {
                    isAttendanceMarked = true
                    binding.apply {
                        ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_absent_filled))
                        ivAttendanceOne.show()
                        if (hasOt) {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        } else {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill_unfilled))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        }
                    }
                }

                Constants.ATTENDANCE_STATUS_PRESENT -> {
                    isAttendanceMarked = true
                    binding.apply {
                        ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_present_filled))
                        ivAttendanceOne.show()
                        if (hasOt) {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        } else {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill_unfilled))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        }
                    }
                }

                Constants.ATTENDANCE_STATUS_HALF_PRESENT -> {
                    isAttendanceMarked = true
                    binding.apply {
                        ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_present_half_filled))
                        ivAttendanceOne.show()
                        if (hasOt) {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        } else {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill_unfilled))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        }
                    }
                }

                Constants.ATTENDANCE_STATUS_ONE_AND_HALF_PRESENT -> {
                    isAttendanceMarked = true
                    binding.apply {
                        ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_present_one_and_half_filled))
                        ivAttendanceOne.show()
                        if (hasOt) {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        } else {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill_unfilled))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        }
                    }
                }

                Constants.ATTENDANCE_STATUS_DOUBLE_PRESENT -> {
                    isAttendanceMarked = true
                    binding.apply {
                        ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_double_present_filled))
                        ivAttendanceOne.show()
                        if (hasOt) {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        } else {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill_unfilled))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        }
                    }
                }

                Constants.ATTENDANCE_STATUS_PAID_LEAVE -> {
                    isAttendanceMarked = true
                    binding.apply {
                        ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_paid_leave_filled))
                        ivAttendanceOne.show()
                        if (hasOt) {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        } else {
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_ot_pill_unfilled))
                            ivAttendanceTwo.show()
                            ivOtPill.visibility = android.view.View.GONE
                        }
                    }
                }

                Constants.ATTENDANCE_STATUS_NULL -> {
                    isAttendanceMarked = false
                    if (hasOt) {
                        // Only OT is present, show OT in third button and keep first two buttons for attendance
                        binding.apply {
                            ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_absent_empty))
                            ivAttendanceOne.show()
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_present_empty))
                            ivAttendanceTwo.show()
                            ivOtPill.setImageDrawable(getDrawable(binding.ivOtPill.context, R.drawable.ic_ot_pill))
                            ivOtPill.visibility = android.view.View.VISIBLE
                        }
                    } else {
                        // No attendance or OT, show empty OT button for discovery
                        binding.apply {
                            ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_absent_empty))
                            ivAttendanceOne.show()
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_present_empty))
                            ivAttendanceTwo.show()
                            ivOtPill.setImageDrawable(getDrawable(binding.ivOtPill.context, R.drawable.ic_ot_pill_unfilled))
                            ivOtPill.visibility = android.view.View.VISIBLE
                        }
                    }
                }
                else -> {
                    isAttendanceMarked = false
                    if (hasOt) {
                        // Only OT is present, show OT in third button and keep first two buttons for attendance
                        binding.apply {
                            ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_absent_empty))
                            ivAttendanceOne.show()
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_present_empty))
                            ivAttendanceTwo.show()
                            ivOtPill.setImageDrawable(getDrawable(binding.ivOtPill.context, R.drawable.ic_ot_pill))
                            ivOtPill.visibility = android.view.View.VISIBLE
                        }
                    } else {
                        // No attendance or OT, show empty OT button for discovery
                        binding.apply {
                            ivAttendanceOne.setImageDrawable(getDrawable(binding.ivAttendanceOne.context, R.drawable.ic_absent_empty))
                            ivAttendanceOne.show()
                            ivAttendanceTwo.setImageDrawable(getDrawable(binding.ivAttendanceTwo.context, R.drawable.ic_present_empty))
                            ivAttendanceTwo.show()
                            ivOtPill.setImageDrawable(getDrawable(binding.ivOtPill.context, R.drawable.ic_ot_pill_unfilled))
                            ivOtPill.visibility = android.view.View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCalendarAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), staffName, staffId)
    }

    fun setStaffName(name: String) {
        this.staffName = name
    }

    fun setStaffId(staffId: String) {
        this.staffId = staffId
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<CalendarItem>() {
        override fun areItemsTheSame(oldItem: CalendarItem, newItem: CalendarItem): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: CalendarItem, newItem: CalendarItem): Boolean {
            return (oldItem.attendanceStatus == newItem.attendanceStatus && 
                    oldItem.advance == newItem.advance && 
                    oldItem.reason == newItem.reason &&
                    oldItem.otTotalAmount == newItem.otTotalAmount &&
                    oldItem.advancePaymentMethod == newItem.advancePaymentMethod)
        }
    }
}