package com.laborbook.keep.screen.calendar.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.boilerplate.network.model.NetworkResult
import com.boilerplate.network.model.NetworkResultStatus
import com.laborbook.base.BaseViewModel
import com.laborbook.base.Logger
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.keep.model.AttendanceItem
import com.laborbook.keep.model.CalendarItem
import com.laborbook.keep.model.MarkBulkAttendanceRequestBody
import com.laborbook.keep.model.MarkSingleAttendanceRequestBody
import com.laborbook.keep.model.CurrentSalaryResponse
import com.laborbook.keep.screen.calendar.uistate.CalendarUiState
import com.laborbook.keep.usecase.KeepUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarViewModel(val keepUseCase: KeepUseCase) : BaseViewModel<CalendarUiState>(), KoinComponent {

    private val attendanceItems = mutableListOf<AttendanceItem>()
    private val dataStoreManager: DataStoreManager by inject()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logger.e("Caught exception: ${exception.localizedMessage}")
        uiState.value = CalendarUiState.Error(exception.localizedMessage)
    }

    fun getAttendanceItems(): List<AttendanceItem> {
        return attendanceItems
    }

    fun cleaAttendanceItems() {
        attendanceItems.clear()
    }

    suspend fun createMarkAttendanceRequestBody() = MarkBulkAttendanceRequestBody(
        attendances = attendanceItems,
        managerId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
    )

    suspend fun createMarkSingleAttendanceRequestBody(attendance: AttendanceItem) = MarkSingleAttendanceRequestBody(
        attendance = attendance,
        managerId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
    )

    fun getStaffAttendances(id: String, month: String, year: String) =
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            keepUseCase.getUserAttendances(id, month, year).collect(collector = {
                withContext(Dispatchers.Main) {
                    when (it.status) {
                        NetworkResultStatus.SUCCESS -> {
                            uiState.value = it.data?.let { staffUser ->
                                CalendarUiState.GetStaffAttendanceSuccess(staffUser)
                            }
                        }

                        NetworkResultStatus.ERROR -> {
                            uiState.value = CalendarUiState.Error(it.message ?: "")
                        }

                        NetworkResultStatus.LOADING -> {
                            uiState.value = CalendarUiState.Loading
                        }
                    }
                }
            })
        }

    fun isAttendancesMarked(): Boolean {
        return (attendanceItems.size > 0)
    }

    fun markAttendance(attendanceItem: AttendanceItem) {
        val existingAttendanceItemForSameDate = attendanceItems.find {
            it.attendanceDate == attendanceItem.attendanceDate
        }

        if (existingAttendanceItemForSameDate == null) {
            attendanceItems.add(attendanceItem)
        } else if (existingAttendanceItemForSameDate != attendanceItem) {
            attendanceItems[attendanceItems.indexOf(existingAttendanceItemForSameDate)] =
                attendanceItem
        }
    }

    fun markBulkAttendance(id: String, markAttendanceBody: MarkBulkAttendanceRequestBody) =
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            keepUseCase.markBulkAttendance(id, markAttendanceBody).collect(collector = {
                withContext(Dispatchers.Main) {
                    when (it.status) {
                        NetworkResultStatus.SUCCESS -> {
                            uiState.value = it.data?.let { data ->
                                CalendarUiState.MarkBulkAttendanceSuccess(
                                    data
                                )
                            }
                        }

                        NetworkResultStatus.ERROR -> {
                            uiState.value = CalendarUiState.Error(it.message ?: "")
                        }

                        NetworkResultStatus.LOADING -> {
                            uiState.value = CalendarUiState.Loading
                        }
                    }
                }
            })
        }

    fun markSingleAttendance(id: String, markAttendanceBody: MarkSingleAttendanceRequestBody) =
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            keepUseCase.markSingleAttendance(id, markAttendanceBody).collect(collector = {
                withContext(Dispatchers.Main) {
                    when (it.status) {
                        NetworkResultStatus.SUCCESS -> {
                            uiState.value = it.data?.let { data ->
                                CalendarUiState.MarkBulkAttendanceSuccess(
                                    data
                                )
                            }
                        }

                        NetworkResultStatus.ERROR -> {
                            uiState.value = CalendarUiState.Error(it.message ?: "")
                        }

                        NetworkResultStatus.LOADING -> {
                            uiState.value = CalendarUiState.Loading
                        }
                    }
                }
            })
        }

    fun fetchUserSalary(userId: String, month: Int, year: Int) =
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            uiState.postValue(CalendarUiState.SalaryLoading(true))
            keepUseCase.getUserSalary(userId, month, year).collect { result ->
                withContext(Dispatchers.Main) {
                    when (result.status) {
                        NetworkResultStatus.SUCCESS -> {
                            result.data?.let {
                                uiState.value = CalendarUiState.GetUserSalarySuccess(it.total_salary)
                            }
                        }
                        NetworkResultStatus.ERROR -> {
                            val userFriendlyMessage = when {
                                result.message?.contains("Failed to fetch salary", ignoreCase = true) == true -> 
                                    "No salary data found for this month. Please add salary in the staff profile."
                                result.message?.contains("Failed to calculate total salary", ignoreCase = true) == true -> 
                                    "No salary data found for this month. Please add salary in the staff profile."
                                result.message?.contains("salary", ignoreCase = true) == true -> 
                                    "No salary data found for this month. Please add salary in the staff profile."
                                else -> result.message ?: "No salary data found for this month. Please add salary in the staff profile."
                            }
                            uiState.value = CalendarUiState.GetUserSalaryError(userFriendlyMessage)
                        }
                        NetworkResultStatus.LOADING -> {
                            // Do nothing for generic loading
                        }
                    }
                }
            }
        }

    fun startReportLoading() {
        uiState.value = CalendarUiState.ReportLoading(true)
    }

    fun stopReportLoading() {
        uiState.value = CalendarUiState.ReportLoading(false)
    }

    /**
     * For any CalendarItem whose day is Sunday and has no attendance status set,
     * pre-fill it with "H" (Holiday) so the UI shows it highlighted without requiring
     * the manager to mark it manually.
     */
    fun withSundaysMarked(items: List<CalendarItem>?): List<CalendarItem>? {
        if (items == null) return null
        val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return items.map { item ->
            if (item.attendanceStatus.isNullOrEmpty()) {
                try {
                    val cal = Calendar.getInstance().apply { time = fmt.parse(item.date)!! }
                    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        item.copy(attendanceStatus = "H")
                    } else item
                } catch (_: Exception) { item }
            } else item
        }
    }

    fun getCurrentSalary(userId: String) =
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            keepUseCase.getCurrentSalary(userId).collect { result ->
                withContext(Dispatchers.Main) {
                    when (result.status) {
                        NetworkResultStatus.SUCCESS -> {
                            result.data?.let {
                                uiState.value = CalendarUiState.GetCurrentSalarySuccess(it)
                            }
                        }
                        NetworkResultStatus.ERROR -> {
                            uiState.value = CalendarUiState.GetCurrentSalaryError(result.message ?: "Failed to fetch current salary")
                        }
                        NetworkResultStatus.LOADING -> {
                            uiState.value = CalendarUiState.CurrentSalaryLoading(true)
                        }
                    }
                }
            }
        }
}