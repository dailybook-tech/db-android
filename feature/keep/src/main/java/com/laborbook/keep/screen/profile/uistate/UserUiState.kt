package com.laborbook.keep.screen.profile.uistate

sealed class UserUiState {
    data object Loading : UserUiState()
    data class UpdateUserNameSuccess(val name: String) : UserUiState()
    data class Error(val message: String) : UserUiState()
    data object RefreshUserNameSuccess : UserUiState()
    data class GetUserNameSucess(val name: String) : UserUiState()
}