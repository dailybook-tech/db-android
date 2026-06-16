package com.dailybook.keep.screen.deletestaff.uistate

sealed class DeleteStaffUiState {
    data object Loading : DeleteStaffUiState()
    data class Success(val message: String) : DeleteStaffUiState()
    data class Error(val message: String) : DeleteStaffUiState()
}