package com.laborbook.expense.repository

import com.boilerplate.network.model.NetworkResult
import com.laborbook.expense.model.DeleteTransactionResponseModel
import com.laborbook.expense.model.Transaction
import com.laborbook.expense.model.TransactionRequest
import com.laborbook.expense.model.TransactionSummaryResponseModel
import com.laborbook.expense.model.TransactionsResponseModel
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun getTransactions(userId: String, month: String, year: String, pageNo: Int): Flow<NetworkResult<TransactionsResponseModel?>>
    suspend fun getTransactionSummary(userId: String, month: String, year: String): Flow<NetworkResult<TransactionSummaryResponseModel?>>
    suspend fun createTransaction(userId: String, transactionRequest: TransactionRequest): Flow<NetworkResult<Transaction?>>
    suspend fun updateTransaction(userId: String, id: String, transactionRequest: TransactionRequest): Flow<NetworkResult<Transaction?>>
    suspend fun deleteTransaction(userId: String, id: String): Flow<NetworkResult<DeleteTransactionResponseModel?>>
}