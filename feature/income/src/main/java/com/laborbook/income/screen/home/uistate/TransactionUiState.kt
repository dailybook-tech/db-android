package com.laborbook.income.screen.home.uistate

sealed class TransactionUiState<out T> {
    data object LOADING : TransactionUiState<Nothing>()
    data class SUCCESS<out T>(val data: T, val isFromServer: Boolean = true) : TransactionUiState<T>()
    data class ERROR(val message: String) : TransactionUiState<Nothing>()
}