package com.dailybook.keep.screen.deletestaff.viewmodel

import androidx.lifecycle.viewModelScope
import com.boilerplate.network.model.NetworkResultStatus
import com.dailybook.base.BaseViewModel
import com.dailybook.base.Logger
import com.dailybook.keep.screen.deletestaff.uistate.DeleteStaffUiState
import com.dailybook.keep.usecase.KeepUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeleteStaffViewModel(val keepUseCase: KeepUseCase): BaseViewModel<DeleteStaffUiState>() {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logger.e("Caught exception: ${exception.localizedMessage}")
        uiState.value = DeleteStaffUiState.Error(exception.localizedMessage)
    }

    fun deleteStaffUser(id: String) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        keepUseCase.deleteStaffUser(id).collect(collector = {
            withContext(Dispatchers.Main) {
                when (it.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = it.data?.let { message -> DeleteStaffUiState.Success(message) }
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value = DeleteStaffUiState.Error(it.message ?: "")
                    }

                    NetworkResultStatus.LOADING -> {
                        uiState.value = DeleteStaffUiState.Loading
                    }
                }
            }
        })
    }
}