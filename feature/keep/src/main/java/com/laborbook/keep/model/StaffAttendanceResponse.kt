package com.laborbook.keep.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

data class StaffAttendanceResponse(val user: AttendanceUser?, val attendance: List<CalendarItem>?)

@Entity(tableName = "attendance_user")
data class AttendanceUser(
    @PrimaryKey var id: String,
    val name: String,
    @SerializedName("total_present")
    val totalPresent: Double,
    @SerializedName("total_absent")
    val totalAbsent: Double,
    @SerializedName("total_advance")
    val totalAdvance: Double,
    val month: String,
    @SerializedName("total_ot")
    val totalOt: Double? = 0.0,
    @SerializedName("total_pp")
    val totalPp: Double? = 0.0,
    @SerializedName("total_ph")
    val totalPh: Double? = 0.0,
    @SerializedName("total_h")
    val totalH: Double? = 0.0,
)

@Entity(tableName = "calendar_item", primaryKeys = ["id", "date", "month", "year"])
data class CalendarItem(
    var id: String,
    var month: String,
    var year: String,
    val date: String,
    val day: String,
    @SerializedName("attendance_status")
    var attendanceStatus: String? = "",
    var advance: String? = "",
    @SerializedName("advance_reason")
    var reason: String? = "",
    @SerializedName("ot_minutes")
    var otMinutes: Double? = 0.0,
    @SerializedName("ot_per_hour")
    var otPerHour: Double? = 0.0,
    @SerializedName("ot_total_amount")
    var otTotalAmount: Double? = 0.0,
    @SerializedName("advance_payment_method")
    var advancePaymentMethod: String? = null,
    @SerializedName("shift_type")
    var shiftType: String? = "day",
    @SerializedName("remarks")
    var remarks: String? = null,
)