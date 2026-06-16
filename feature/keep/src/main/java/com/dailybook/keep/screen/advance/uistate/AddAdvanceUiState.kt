package com.dailybook.keep.screen.advance.uistate

sealed class AddAdvanceUiState {
    data object Loading : AddAdvanceUiState()
    data class Success(val message: String) : AddAdvanceUiState()
    data class Error(val message: String) : AddAdvanceUiState()
    data class AdvanceEntered(val advanceEntered: Boolean) : AddAdvanceUiState()
}