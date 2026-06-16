package com.dailybook.keep.screen.calendar.utils

import com.dailybook.keep.model.AttendanceItem
import com.dailybook.keep.model.Staff

class ObserverUtil {
    var onDailyAttendanceMarkListener : ((AttendanceItem) -> Unit)? = null
    var onStaffUserAddedListener : ((Staff) -> Unit)? = null
    var addedStaff : ((Boolean) -> Unit)? = null
    var refreshStaffs : ((Boolean) -> Unit)? = null
    var refreshCalendar : ((shouldRefresh: Boolean, isAdvanceTransactionSuccess: Boolean, advance: String, accessedDate: Int) -> Unit)? = null
    var goBackFromCalendar : ((shouldGoBack: Boolean) -> Unit)? = null
    var clearSearchText : ((shouldClear: Boolean) -> Unit)? = null
    var showReviewBottomSheet : ((showReviewBottomSheet: Boolean) -> Unit)? = null
}