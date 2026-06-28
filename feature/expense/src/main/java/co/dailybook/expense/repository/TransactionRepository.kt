package co.dailybook.expense.repository

import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.expense.model.DeleteTransactionResponseModel
import co.dailybook.expense.model.Transaction
import co.dailybook.expense.model.TransactionRequest
import co.dailybook.expense.model.TransactionSummaryResponseModel
import co.dailybook.expense.model.TransactionsResponseModel
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun getTransactions(userId: String, month: String, year: String, pageNo: Int): Flow<NetworkResult<TransactionsResponseModel?>>
    suspend fun getTransactionSummary(userId: String, month: String, year: String): Flow<NetworkResult<TransactionSummaryResponseModel?>>
    suspend fun createTransaction(userId: String, transactionRequest: TransactionRequest): Flow<NetworkResult<Transaction?>>
    suspend fun updateTransaction(userId: String, id: String, transactionRequest: TransactionRequest): Flow<NetworkResult<Transaction?>>
    suspend fun deleteTransaction(userId: String, id: String): Flow<NetworkResult<DeleteTransactionResponseModel?>>
}