package com.dailybook.expense.network

import com.boilerplate.network.NetworkHandler
import com.boilerplate.network.model.NetworkResult
import com.dailybook.base.BaseConstants
import com.dailybook.expense.model.DeleteTransactionResponseModel
import com.dailybook.expense.model.Transaction
import com.dailybook.expense.model.TransactionRequest
import com.dailybook.expense.model.TransactionSummaryResponseModel
import com.dailybook.expense.model.TransactionsResponseModel
import com.dailybook.expense.util.Constants
import kotlinx.coroutines.flow.Flow

class TransactionNetworkModule {

    private val baseUrl =
        if (BaseConstants.DEBUG) BaseConstants.BASE_URL_SBOX else BaseConstants.BASE_URL
    private val networkHandler = NetworkHandler.getInstance()
    private val api: TransactionApi = networkHandler.getApiClient<TransactionApi>(baseUrl)

    suspend fun getExpenses(userId: String, month: String, year: String, pageNo: Int): Flow<NetworkResult<TransactionsResponseModel?>> {
        return networkHandler.getData {
            api.getTransactions(userId, month, year, pageNo, Constants.DEBIT)
        }
    }

    suspend fun getExpenseSummary(userId: String, month: String, year: String): Flow<NetworkResult<TransactionSummaryResponseModel?>> {
        return networkHandler.getData {
            api.getTransactionSummary(userId, month, year, Constants.DEBIT)
        }
    }

    suspend fun createExpense(userId: String, transactionRequest: TransactionRequest): Flow<NetworkResult<Transaction?>> {
        return networkHandler.getData {
            api.createTransaction(userId, transactionRequest)
        }
    }

    suspend fun updateExpense(userId: String, id: String, transactionRequest: TransactionRequest): Flow<NetworkResult<Transaction?>> {
        return networkHandler.getData {
            api.updateTransaction(userId, id, transactionRequest)
        }
    }

    suspend fun deleteExpense(userId: String, id: String): Flow<NetworkResult<DeleteTransactionResponseModel?>> {
        return networkHandler.getData {
            api.deleteTransaction(userId, id)
        }
    }
}