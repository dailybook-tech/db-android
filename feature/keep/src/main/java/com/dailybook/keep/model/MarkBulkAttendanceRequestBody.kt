package com.dailybook.keep.model

import com.google.gson.annotations.SerializedName

data class MarkSingleAttendanceRequestBody(
    val attendance: AttendanceItem,
    @SerializedName("manager_id")
    val managerId: String,
)

data class MarkBulkAttendanceRequestBody(
    val attendances: List<AttendanceItem>,
    @SerializedName("manager_id")
    val managerId: String,
)

data class MarkBulkAttendanceResponse(
    val attendances: List<AttendanceItem>,
    @SerializedName("manager_id")
    val managerId: String,
)

data class AttendanceItem(
    @SerializedName("attendance_date")
    val attendanceDate: String? = "",
    @SerializedName("attendance_status")
    val attendanceStatus: String? = null,
    @SerializedName("advance")
    val advance: Int? = null,
    @SerializedName("shift_type")
    val shiftType: String? = "day",   // "day" | "night"
    @SerializedName("remarks")
    val remarks: String? = null,
    @SerializedName("clock_in")
    val clockIn: String? = null,      // RFC3339 timestamp
    @SerializedName("clock_out")
    val clockOut: String? = null,     // RFC3339 timestamp
)