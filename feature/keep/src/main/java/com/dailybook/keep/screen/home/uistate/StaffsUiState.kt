package com.dailybook.keep.screen.home.uistate

import com.dailybook.keep.model.StaffUser

sealed class StaffsUiState {
    data object Loading : StaffsUiState()
    data class Success(val staffs: List<StaffUser>) : StaffsUiState()
    data class Error(val message: String) : StaffsUiState()
}