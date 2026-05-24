package com.laborbook.expense.screen.cashentry.uistate

sealed class CashInOutUiState<out T> {
    data object LOADING : CashInOutUiState<Nothing>()
    data class CREATE_SUCCESS<out T>(val data: T, val isFromServer: Boolean = true) : CashInOutUiState<T>()
    data class UPDATE_SUCCESS<out T>(val data: T, val isFromServer: Boolean = true) : CashInOutUiState<T>()
    data class DELETE_SUCCESS<out T>(val data: T, val isFromServer: Boolean = true) : CashInOutUiState<T>()
    data class ERROR(val message: String) : CashInOutUiState<Nothing>()
    data class ExpenseEntered(val expenseEntered: Boolean) : CashInOutUiState<Nothing>()
}