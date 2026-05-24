package com.laborbook.keep.screen.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.boilerplate.network.model.NetworkResultStatus
import com.laborbook.base.BaseViewModel
import com.laborbook.base.Logger
import com.laborbook.keep.model.UpdateUserNameRequestBody
import com.laborbook.keep.screen.profile.uistate.UserUiState
import com.laborbook.keep.usecase.KeepUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserProfileViewModel(val keepUseCase: KeepUseCase): BaseViewModel<UserUiState>() {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logger.e("Caught exception: ${exception.localizedMessage}")
        uiState.value = UserUiState.Error(exception.localizedMessage)
    }

    fun updateUserName(id: String, updateUserNameRequestBody: UpdateUserNameRequestBody) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        keepUseCase.updateUserName(id, updateUserNameRequestBody).collect(collector = {
            withContext(Dispatchers.Main) {
                when (it.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = UserUiState.UpdateUserNameSuccess(it.data?.name ?: "")
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value = UserUiState.Error(it.message ?: "")
                    }

                    NetworkResultStatus.LOADING -> {
                        uiState.value = UserUiState.Loading
                    }
                }
            }
        })
    }

    fun getUser(id: String) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        keepUseCase.getUser(id).collect(collector = {
            withContext(Dispatchers.Main) {
                when (it.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = UserUiState.GetUserNameSucess(it.data?.name ?: "")
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value = UserUiState.Error(it.message ?: "")
                    }

                    NetworkResultStatus.LOADING -> {
                        uiState.value = UserUiState.Loading
                    }
                }
            }
        })
    }

    fun triggerUpdateUserNameUiState() {
        uiState.value = UserUiState.RefreshUserNameSuccess
    }
}