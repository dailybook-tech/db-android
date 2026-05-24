package com.laborbook.keep.screen.calendar.uistate

import com.laborbook.keep.model.MarkBulkAttendanceResponse
import com.laborbook.keep.model.StaffAttendanceResponse
import com.laborbook.keep.model.CurrentSalaryResponse

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data class GetStaffAttendanceSuccess(val staff: StaffAttendanceResponse) : CalendarUiState()
    data class MarkBulkAttendanceSuccess(val message: String) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
    data class GetUserSalarySuccess(val salary: Double) : CalendarUiState()
    data class GetUserSalaryError(val message: String) : CalendarUiState()
    data class SalaryLoading(val isLoading: Boolean = true) : CalendarUiState()
    data class ReportLoading(val isLoading: Boolean = true) : CalendarUiState()
    data class GetCurrentSalarySuccess(val currentSalary: CurrentSalaryResponse) : CalendarUiState()
    data class GetCurrentSalaryError(val message: String) : CalendarUiState()
    data class CurrentSalaryLoading(val isLoading: Boolean = true) : CalendarUiState()
}