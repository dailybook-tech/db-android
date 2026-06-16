package com.dailybook.keep.network

import com.boilerplate.network.NetworkHandler
import com.boilerplate.network.model.NetworkResult
import com.dailybook.base.BaseConstants
import com.dailybook.keep.database.AppDatabase
import com.dailybook.keep.model.AddAdvanceRequestBody
import com.dailybook.keep.model.AdvanceEntryRequestBody
import com.dailybook.keep.model.AdvanceEntryResponse
import com.dailybook.keep.model.AssignWorkerToTeamRequestBody
import com.dailybook.keep.model.CreateTeamRequestBody
import com.dailybook.keep.model.TeamResponse
import com.dailybook.keep.model.UpdateTeamRequestBody
import com.dailybook.keep.model.AddStaffUserRequestBody
import com.dailybook.keep.model.AddStaffUserResponse
import com.dailybook.keep.model.AddStaffUsersRequestBody
import com.dailybook.keep.model.GetUserResponse
import com.dailybook.keep.model.MarkBulkAttendanceRequestBody
import com.dailybook.keep.model.MarkSingleAttendanceRequestBody
import com.dailybook.keep.model.OvertimeRequestBody
import com.dailybook.keep.model.SalaryData
import com.dailybook.keep.model.StaffAttendanceResponse
import com.dailybook.keep.model.StaffUserResponseModel
import com.dailybook.keep.model.UpdateUserNameRequestBody
import com.dailybook.keep.model.AttendanceUser
import com.dailybook.keep.model.CalendarItem
import com.dailybook.keep.screen.calendar.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.dailybook.keep.model.CurrentSalaryResponse

class KeepNetworkModule(val db: AppDatabase) {
    private val baseUrl =
        if (BaseConstants.DEBUG) BaseConstants.BASE_URL_SBOX else BaseConstants.BASE_URL
    private val networkHandler = NetworkHandler.getInstance()
    private val api: KeepApi = networkHandler.getApiClient<KeepApi>(baseUrl)

    suspend fun getStaffUsers(id: String): Flow<NetworkResult<StaffUserResponseModel?>> {
        return networkHandler.getCachedData(
            remoteFetch = { api.getStaffUsers(id) },
            localFetch = { listOf(StaffUserResponseModel(db.staffUserDao().getAllStaffUsers())) },
            localStore = { db.staffUserDao().insertStaffUsers(it.users) },
            localDelete = { db.staffUserDao().deleteAllStaffs() }
        )
    }

    suspend fun addStaffUsers(staffUsers: AddStaffUsersRequestBody): Flow<NetworkResult<String?>> {
        return networkHandler.getData {
            api.addStaffUsers(staffUsers)
        }
    }

    suspend fun addStaffUser(staffUser: AddStaffUserRequestBody): Flow<NetworkResult<AddStaffUserResponse?>> {
        return networkHandler.getData {
            api.addStaffUser(staffUser)
        }
    }

    suspend fun deleteStaffUser(id: String): Flow<NetworkResult<String?>> {
        return networkHandler.getData {
            api.deleteStaffUser(id)
        }
    }

    suspend fun getUserAttendances(
        id: String,
        month: String,
        year: String
    ): Flow<NetworkResult<StaffAttendanceResponse?>> {
        return networkHandler.getCachedData(
            remoteFetch = { api.getUserAttendance(id, month, year) },
            localFetch = {
                val calendarItems = db.calendarItemDao().getAllByIdMonthYear(id, month, year)
                val cachedUser = kotlin.runCatching { db.attendanceUserDao().getUserById(id) }.getOrNull()
                val user = if (calendarItems.isNotEmpty() && cachedUser != null) {
                    buildAttendanceUserFromCalendarItems(cachedUser, calendarItems, month)
                } else {
                    cachedUser
                }
                listOf(
                    StaffAttendanceResponse(
                        user,
                        calendarItems
                    )
                )
            },
            localStore = {
                val MAX_ENTRIES = 20000
                it.user?.id = id
                it.user?.let { user -> db.attendanceUserDao().insert(user) }
                it.attendance?.forEach { item ->
                    item.id = id
                    item.month = month
                    item.year = year
                }
                val currentCount = db.calendarItemDao().getCount()
                if (currentCount >= MAX_ENTRIES) {
                    db.calendarItemDao().deleteOldest(currentCount - MAX_ENTRIES + 1)
                }
                it.attendance?.let { attendance -> db.calendarItemDao().insertAll(attendance) }
            },
            localDelete = {
                db.attendanceUserDao().deleteByUserId(id)
                db.calendarItemDao().deleteByUserIdMonthYear(id, month, year)
            }
        )
    }

    /**
     * Builds AttendanceUser with totals computed from calendar items for the selected month.
     * Used when serving from cache so that switching to a previous month shows correct
     * totalAdvance and other aggregates in the calendar UI and reports.
     */
    private fun buildAttendanceUserFromCalendarItems(
        cachedUser: AttendanceUser,
        calendarItems: List<CalendarItem>,
        month: String
    ): AttendanceUser {
        var totalAdvance = 0.0
        var totalPresent = 0.0
        var totalAbsent = 0.0
        var totalH = 0.0
        var totalPh = 0.0
        var totalPp = 0.0
        var totalOt = 0.0

        for (item in calendarItems) {
            totalAdvance += (item.advance?.toIntOrNull() ?: 0).toDouble()
            when (item.attendanceStatus) {
                Constants.ATTENDANCE_STATUS_PRESENT, Constants.ATTENDANCE_STATUS_PAID_LEAVE -> {
                    totalPresent += 1.0
                }
                Constants.ATTENDANCE_STATUS_ABSENT -> totalAbsent += 1.0
                Constants.ATTENDANCE_STATUS_HALF_PRESENT -> {
                    totalH += 1.0
                    totalPresent += 0.5
                }
                Constants.ATTENDANCE_STATUS_ONE_AND_HALF_PRESENT -> {
                    totalPh += 1.0
                    totalPresent += 1.5
                }
                Constants.ATTENDANCE_STATUS_DOUBLE_PRESENT -> {
                    totalPp += 1.0
                    totalPresent += 2.0
                }
            }
            totalOt += (item.otMinutes ?: 0.0) / 60.0
        }

        return AttendanceUser(
            id = cachedUser.id,
            name = cachedUser.name,
            totalPresent = totalPresent,
            totalAbsent = totalAbsent,
            totalAdvance = totalAdvance,
            month = month,
            totalOt = totalOt,
            totalPp = totalPp,
            totalPh = totalPh,
            totalH = totalH
        )
    }

    suspend fun markBulkAttendance(
        id: String,
        markAttendanceBody: MarkBulkAttendanceRequestBody,
    ): Flow<NetworkResult<String?>> {
        return networkHandler.getData {
            api.markBulkAttendance(id, markAttendanceBody)
        }
    }

    suspend fun markSingleAttendance(
        id: String,
        markAttendanceBody: MarkSingleAttendanceRequestBody,
    ): Flow<NetworkResult<String?>> {
        val (month, year) = parseMonthYearFromAttendanceDate(
            markAttendanceBody.attendance.attendanceDate
        )
        return networkHandler.getData {
            api.markSingleAttendance(id, month, year, markAttendanceBody)
        }
    }

    /**
     * Parses month and year from attendance_date string (format dd-MM-yyyy).
     * Returns current month and year if parsing fails.
     */
    private fun parseMonthYearFromAttendanceDate(attendanceDate: String?): Pair<String, String> {
        if (!attendanceDate.isNullOrBlank()) {
            val parts = attendanceDate.trim().split("-")
            if (parts.size == 3) {
                val day = parts.getOrNull(0)?.toIntOrNull()
                val month = parts.getOrNull(1)?.toIntOrNull()
                val year = parts.getOrNull(2)?.toIntOrNull()
                if (month != null && year != null && day != null) {
                    return month.toString() to year.toString()
                }
            }
        }
        val cal = java.util.Calendar.getInstance()
        return (cal.get(java.util.Calendar.MONTH) + 1).toString() to cal.get(java.util.Calendar.YEAR).toString()
    }

    suspend fun updateUserName(
        id: String,
        updateUserNameRequestBody: UpdateUserNameRequestBody
    ): Flow<NetworkResult<GetUserResponse?>> {
        return networkHandler.getData {
            api.updateUserName(id, updateUserNameRequestBody)
        }
    }

    suspend fun saveOvertime(
        userId: String,
        date: String,
        otMinutes: Double,
        otPerHour: Double,
        managerId: String
    ): Flow<NetworkResult<String?>> {
        return networkHandler.getData {
            api.addOvertime(
                userId,
                OvertimeRequestBody(date, otMinutes, otPerHour, managerId)
            )
        }
    }

    suspend fun deleteOvertime(
        userId: String,
        date: String,
        managerId: String
    ): Flow<NetworkResult<String?>> {
        return networkHandler.getData {
            api.addOvertime(
                userId,
                OvertimeRequestBody(date, null, null, managerId)
            )
        }
    }

    suspend fun getUser(id: String): Flow<NetworkResult<GetUserResponse?>> {
        return networkHandler.getData {
            api.getUser(id)
        }
    }

    suspend fun addAdvance(
        id: String,
        addAdvanceRequestBody: AddAdvanceRequestBody,
    ): Flow<NetworkResult<String?>> {
        return networkHandler.getData {
            api.addAdvance(id, addAdvanceRequestBody)
        }
    }

    suspend fun addAdvanceEntry(
        userId: String,
        body: AdvanceEntryRequestBody
    ): Flow<NetworkResult<AdvanceEntryResponse?>> {
        return networkHandler.getData { api.addAdvanceEntry(userId, body) }
    }

    suspend fun listAdvanceEntries(
        userId: String,
        month: Int,
        year: Int
    ): Flow<NetworkResult<List<AdvanceEntryResponse>?>> {
        return networkHandler.getData { api.listAdvanceEntries(userId, month, year) }
    }

    suspend fun deleteAdvanceEntry(advanceId: String): Flow<NetworkResult<String?>> {
        return networkHandler.getData { api.deleteAdvanceEntry(advanceId) }
    }

    suspend fun addOrUpdateSalary(
        userId: String,
        body: com.dailybook.keep.model.AddOrUpdateSalaryRequestBody
    ): Flow<NetworkResult<String?>> {
        return networkHandler.getData {
            api.addOrUpdateSalary(userId, body)
        }
    }

    suspend fun getUserSalary(
        userId: String,
        month: Int,
        year: Int
    ): Flow<NetworkResult<SalaryData?>> {
        return networkHandler.getData { api.getUserSalary(userId, month, year) }
    }

    suspend fun getCurrentSalary(
        userId: String
    ): Flow<NetworkResult<CurrentSalaryResponse?>> {
        return networkHandler.getData { api.getCurrentSalary(userId) }
    }
    
    suspend fun createTeam(body: CreateTeamRequestBody): Flow<NetworkResult<TeamResponse?>> =
        networkHandler.getData { api.createTeam(body) }

    suspend fun listTeams(): Flow<NetworkResult<List<TeamResponse>?>> =
        networkHandler.getData { api.listTeams() }

    suspend fun updateTeam(teamId: String, body: UpdateTeamRequestBody): Flow<NetworkResult<TeamResponse?>> =
        networkHandler.getData { api.updateTeam(teamId, body) }

    suspend fun deleteTeam(teamId: String): Flow<NetworkResult<String?>> =
        networkHandler.getData { api.deleteTeam(teamId) }

    suspend fun assignWorkerToTeam(userId: String, body: AssignWorkerToTeamRequestBody): Flow<NetworkResult<String?>> =
        networkHandler.getData { api.assignWorkerToTeam(userId, body) }

    // Subscription Methods
    suspend fun getSubscriptionPlans(userId: String): Flow<NetworkResult<com.dailybook.keep.model.subscription.SubscriptionPlansResponse?>> {
        return networkHandler.getData { api.getSubscriptionPlans(userId) }
    }
    
    suspend fun getUserSubscription(userId: String): Flow<NetworkResult<com.dailybook.keep.model.subscription.UserSubscription?>> {
        return networkHandler.getData { api.getUserSubscription(userId) }
    }
    
    suspend fun createSubscription(
        userId: String,
        request: com.dailybook.keep.model.subscription.CreateSubscriptionRequest
    ): Flow<NetworkResult<com.dailybook.keep.model.subscription.CreateSubscriptionResponse?>> {
        return networkHandler.getData { api.createSubscription(userId, request) }
    }
    
    suspend fun verifySubscription(
        subscriptionId: String,
        request: com.dailybook.keep.model.subscription.VerifySubscriptionRequest
    ): Flow<NetworkResult<com.dailybook.keep.model.subscription.VerifySubscriptionResponse?>> {
        return networkHandler.getData { api.verifySubscription(subscriptionId, request) }
    }
    
    suspend fun cancelSubscription(
        subscriptionId: String
    ): Flow<NetworkResult<com.dailybook.keep.model.subscription.CancelSubscriptionResponse?>> {
        return networkHandler.getData { api.cancelSubscription(subscriptionId) }
    }
}