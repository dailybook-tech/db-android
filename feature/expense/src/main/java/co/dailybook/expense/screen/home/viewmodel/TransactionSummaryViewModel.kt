package co.dailybook.expense.screen.home.viewmodel

import androidx.lifecycle.viewModelScope
import co.dailybook.boilerplate.network.model.NetworkResultStatus
import co.dailybook.base.BaseViewModel
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.expense.model.TransactionSummaryResponseModel
import co.dailybook.expense.screen.home.uistate.TransactionUiState
import co.dailybook.expense.usecase.GetTransactionSummaryUseCase
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