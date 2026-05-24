package com.laborbook.income.screen.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.boilerplate.network.model.NetworkResultStatus
import com.laborbook.base.BaseViewModel
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.income.model.TransactionSummaryResponseModel
import com.laborbook.income.screen.home.uistate.TransactionUiState
import com.laborbook.income.usecase.GetTransactionSummaryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TransactionSummaryViewModel(private val getTransactionSummaryUseCase: GetTransactionSummaryUseCase, private val dataStoreManager: DataStoreManager) :
    BaseViewModel<TransactionUiState<TransactionSummaryResponseModel?>>() {

    fun getTransactionSummary(month: String, year: String) {
        viewModelScope.launch {
            getTransactionSummaryUseCase.invoke(dataStoreManager.read(DataStoreManager.USER_ID, "").first(), month, year).collect {
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
}