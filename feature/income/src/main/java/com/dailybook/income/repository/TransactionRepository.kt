package com.dailybook.income.repository

import com.boilerplate.network.model.NetworkResult
import com.dailybook.income.model.DeleteTransactionResponseModel
import com.dailybook.income.model.Transaction
import com.dailybook.income.model.TransactionRequest
import com.dailybook.income.model.TransactionSummaryResponseModel
import com.dailybook.income.model.TransactionsResponseModel
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun getTransactions(userId: String, month: String, year: String, pageNo: Int): Flow<NetworkResult<TransactionsResponseModel?>>
    suspend fun getTransactionSummary(userId: String, month: String, year: String): Flow<NetworkResult<TransactionSummaryResponseModel?>>
    suspend fun createTransaction(userId: String, transactionRequest: TransactionRequest): Flow<NetworkResult<Transaction?>>
    suspend fun updateTransaction(userId: String, id: String, transactionRequest: TransactionRequest): Flow<NetworkResult<Transaction?>>
    suspend fun deleteTransaction(userId: String, id: String): Flow<NetworkResult<DeleteTransactionResponseModel?>>
}