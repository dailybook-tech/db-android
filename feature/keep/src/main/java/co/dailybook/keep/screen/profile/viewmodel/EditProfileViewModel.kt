package co.dailybook.keep.screen.profile.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.dailybook.keep.model.AddOrUpdateSalaryRequestBody
import co.dailybook.keep.model.UpdateUserNameRequestBody
import co.dailybook.keep.repository.KeepRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileViewModel(
    private val keepRepository: KeepRepository
) : ViewModel(), KoinComponent {

    val updateResult = MutableLiveData<Result<Unit>>()
    val nameUpdateResult = MutableLiveData<Result<Unit>>()
    val salaryUpdateResult = MutableLiveData<Result<Unit>>()

    fun updateStaffName(staffId: String, newName: String) {
        viewModelScope.launch {
            try {
                keepRepository.updateUserName(staffId, UpdateUserNameRequestBody(newName)).collect {
                    // handle result if needed
                }
                nameUpdateResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                nameUpdateResult.postValue(Result.failure(e))
            }
        }
    }

    fun updateStaffSalary(staffId: String, salaryType: String, salary: Double) {
        viewModelScope.launch {
            try {
                val today = getCurrentDateAsString()
                val body = AddOrUpdateSalaryRequestBody(
                    salary_type = salaryType,
                    salary = salary,
                    start_date = today
                )
                keepRepository.addOrUpdateSalary(staffId, body).collect {
                    // handle result if needed
                }
                salaryUpdateResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                salaryUpdateResult.postValue(Result.failure(e))
            }
        }
    }

    fun updateStaffProfile(
        staffId: String,
        newName: String,
        newMobileNumber: String? = null,
        salaryType: String?,
        salary: Double?,
        salaryChanged: Boolean,
        bonus: Double? = null
    ) {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val jobs = mutableListOf<kotlinx.coroutines.Deferred<Result<Unit>>>()

                    // Update name (and optionally mobile number)
                    jobs += async {
                        keepRepository.updateUserName(staffId, UpdateUserNameRequestBody(newName, newMobileNumber)).collect {
                            // handle result if needed
                        }
                        Result.success(Unit)
                    }

                    // Add/Update salary if changed and not null
                    if (salaryChanged && (salary != null || bonus != null) && salaryType != null) {
                        val today = getCurrentDateAsString()
                        val body = AddOrUpdateSalaryRequestBody(
                            salary_type = salaryType,
                            salary = salary ?: 0.0,
                            start_date = today,
                            bonus = bonus
                        )
                        jobs += async {
                            keepRepository.addOrUpdateSalary(staffId, body).collect {
                                // handle result if needed
                            }
                            Result.success(Unit)
                        }
                    }

                    jobs.awaitAll()
                    updateResult.postValue(Result.success(Unit))
                }
            } catch (e: Exception) {
                updateResult.postValue(Result.failure(e))
            }
        }
    }

    /**
     * Get current date as string in yyyy-MM-dd format
     * Compatible with all Android versions
     */
    private fun getCurrentDateAsString(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
} 