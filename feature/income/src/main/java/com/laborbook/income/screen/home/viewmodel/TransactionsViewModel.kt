package com.laborbook.income.screen.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.boilerplate.network.model.NetworkResultStatus
import com.laborbook.base.BaseViewModel
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.income.model.TransactionsResponseModel
import com.laborbook.income.screen.home.uistate.TransactionUiState
import com.laborbook.income.usecase.GetTransactionsUseCase
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