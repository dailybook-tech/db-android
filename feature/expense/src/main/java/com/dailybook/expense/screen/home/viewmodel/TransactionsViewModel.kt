package com.dailybook.expense.screen.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.boilerplate.network.model.NetworkResultStatus
import com.dailybook.base.BaseViewModel
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.expense.model.Transaction
import com.dailybook.expense.model.TransactionsResponseModel
import com.dailybook.expense.screen.home.uistate.TransactionUiState
import com.dailybook.expense.usecase.GetTransactionsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TransactionsViewModel(private val getTransactionsUseCase: GetTransactionsUseCase, private val dataStoreManager: DataStoreManager) :
    BaseViewModel<TransactionUiState<TransactionsResponseModel?>>() {

    fun getTransactions(month: String, year: String, pageNo: Int) {
        viewModelScope.launch {
            getTransactionsUseCase.invoke(dataStoreManager.read(DataStoreManager.USER_ID, "").first(), month, year, pageNo).collect {
                when (it.status) {
                    NetworkResultStatus.LOADING -> {
                        uiState.value = TransactionUiState.LOADING
                    }

                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = TransactionUiState.SUCCESS(it.data)
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value =
                            it.message?.let { errorMessage -> TransactionUiState.ERROR(errorMessage) }
                    }
                }
            }
        }
    }

    fun clearState() {
        uiState.value = TransactionUiState.LOADING
    }
}