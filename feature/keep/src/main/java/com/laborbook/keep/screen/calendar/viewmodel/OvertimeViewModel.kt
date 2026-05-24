package com.laborbook.keep.screen.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laborbook.keep.repository.KeepRepository
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.lifecycle.MutableLiveData
import com.boilerplate.network.model.NetworkResultStatus
import kotlinx.coroutines.flow.collect
import com.laborbook.base.datastore.DataStoreManager
import org.koin.core.component.inject

class OvertimeViewModel(
    private val repository: KeepRepository
) : ViewModel(), KoinComponent {

    private val dataStoreManager: DataStoreManager by inject()
    val otResult = MutableLiveData<Result<String>>()

    fun addOvertime(
        userId: String,
        date: String,
        otMinutes: Double,
        otPerHour: Double,
        managerId: String
    ) {
        viewModelScope.launch {
            try {
                repository.addOvertime(userId, date, otMinutes, otPerHour, managerId).collect {
                    when (it.status) {
                        NetworkResultStatus.SUCCESS -> otResult.postValue(Result.success(it.data ?: "OT added successfully"))
                        NetworkResultStatus.ERROR -> otResult.postValue(Result.failure(Throwable(it.message ?: "Unknown error")))
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                otResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteOvertime(
        userId: String,
        date: String,
        managerId: String
    ) {
        viewModelScope.launch {
            try {
                repository.deleteOvertime(userId, date, managerId).collect {
                    when (it.status) {
                        NetworkResultStatus.SUCCESS -> otResult.postValue(Result.success(it.data ?: "OT removed successfully"))
                        NetworkResultStatus.ERROR -> otResult.postValue(Result.failure(Throwable(it.message ?: "Unknown error")))
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                otResult.postValue(Result.failure(e))
            }
        }
    }
}
