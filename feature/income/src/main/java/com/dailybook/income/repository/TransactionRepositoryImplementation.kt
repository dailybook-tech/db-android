package com.dailybook.income.repository

import com.boilerplate.network.model.NetworkResult
import com.dailybook.income.model.DeleteTransactionResponseModel
import com.dailybook.income.model.Transaction
import com.dailybook.income.model.TransactionRequest
import com.dailybook.income.model.TransactionSummaryResponseModel
import com.dailybook.income.model.TransactionsResponseModel
import com.dailybook.income.network.TransactionNetworkModule
import kotlinx.coroutines.flow.Flow

class TransactionRepositoryImplementation(private val transactionNetworkModule: TransactionNetworkModule) :
    TransactionRepository {
    override suspend fun getTransactions(
        userId: String,
        month: String,
        year: String,
        pageNo: Int,
    ): Flow<NetworkResult<TransactionsResponseModel?>> {
        return transactionNetworkModule.getExpenses(userId, month, year, pageNo)
    }

    override suspend fun getTransactionSummary(
        userId: String,
        month: String,
        year: String,
    ): Flow<NetworkResult<TransactionSummaryResponseModel?>> {
        return transactionNetworkModule.getExpenseSummary(userId, month, year)
    }

    override suspend fun createTransaction(
        userId: String,
        transactionRequest: TransactionRequest,
    ): Flow<NetworkResult<Transaction?>> {
        return transactionNetworkModule.createExpense(userId, transactionRequest)
    }

    override suspend fun updateTransaction(
        userId: String,
        id: String,
        transactionRequest: TransactionRequest,
    ): Flow<NetworkResult<Transaction?>> {
        return transactionNetworkModule.updateExpense(userId, id, transactionRequest)
    }

    override suspend fun deleteTransaction(userId: String, id: String): Flow<NetworkResult<DeleteTransactionResponseModel?>> {
        return transactionNetworkModule.deleteExpense(userId, id)
    }
}