package com.dailybook.income.screen.cashentry.viewmodel

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import com.boilerplate.network.model.NetworkResultStatus
import com.dailybook.base.BaseViewModel
import com.dailybook.base.Logger
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.income.model.TransactionRequest
import com.dailybook.income.screen.cashentry.uistate.CashInOutUiState
import com.dailybook.income.usecase.CreateTransactionUseCase
import com.dailybook.income.usecase.DeleteTransactionUseCase
import com.dailybook.income.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CashInOutViewModel(
    private val dataStoreManager: DataStoreManager,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
) : BaseViewModel<CashInOutUiState<*>>() {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logger.e("Caught exception: ${exception.localizedMessage}")
        uiState.value = exception.localizedMessage?.let { CashInOutUiState.ERROR(it) }
    }

    fun attachExpenseTextWatcher(textView: TextView) {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                uiState.value = CashInOutUiState.ExpenseEntered(
                    s.toString() != "0" && s.toString()
                        .isNotEmpty()
                )
            }

        }
        textView.addTextChangedListener(textWatcher)
    }

    fun createExpense(transactionRequest: TransactionRequest) {
        viewModelScope.launch(exceptionHandler) {
            createTransactionUseCase.invoke(dataStoreManager.read(DataStoreManager.USER_ID, "").first(), transactionRequest).collect {
                when (it.status) {
                    NetworkResultStatus.LOADING -> {
                        uiState.value = CashInOutUiState.LOADING
                    }
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = CashInOutUiState.CREATE_SUCCESS(it.data)
                    }
                    NetworkResultStatus.ERROR -> {
                        uiState.value =
                            it.message?.let { errorMessage -> CashInOutUiState.ERROR(errorMessage) }
                    }
                }
            }
        }
    }

    fun updateExpense(id: String, transactionRequest: TransactionRequest) {
        viewModelScope.launch(exceptionHandler) {
            updateTransactionUseCase.invoke(dataStoreManager.read(DataStoreManager.USER_ID, "").first(), id, transactionRequest).collect {
                when (it.status) {
                    NetworkResultStatus.LOADING -> {
                        uiState.value = CashInOutUiState.LOADING
                    }
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = CashInOutUiState.UPDATE_SUCCESS(it.data)
                    }
                    NetworkResultStatus.ERROR -> {
                        uiState.value =
                            it.message?.let { errorMessage -> CashInOutUiState.ERROR(errorMessage) }
                    }
                }
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch(exceptionHandler) {
            deleteTransactionUseCase.invoke(dataStoreManager.read(DataStoreManager.USER_ID, "").first(), id).collect {
                when (it.status) {
                    NetworkResultStatus.LOADING -> {
                        uiState.value = CashInOutUiState.LOADING
                    }
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = CashInOutUiState.DELETE_SUCCESS(it.data)
                    }
                    NetworkResultStatus.ERROR -> {
                        uiState.value =
                            it.message?.let { errorMessage -> CashInOutUiState.ERROR(errorMessage) }
                    }
                }
            }
        }
    }
}