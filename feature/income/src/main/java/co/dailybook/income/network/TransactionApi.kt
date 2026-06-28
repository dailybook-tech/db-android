package co.dailybook.income.network

import co.dailybook.boilerplate.network.model.DataResponse
import co.dailybook.income.model.DeleteTransactionResponseModel
import co.dailybook.income.model.Transaction
import co.dailybook.income.model.TransactionRequest
import co.dailybook.income.model.TransactionSummaryResponseModel
import co.dailybook.income.model.TransactionsResponseModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionApi {
    companion object {
        const val API_VERSION = "api/v1"
        const val TRANSACTIONS = "/users/{user_id}/transactions"
        const val TRANSACTION_SUMMARY = "/users/{user_id}/transactions/summary"
        const val CREATE_TRANSACTION = "/users/{user_id}/transactions"
        const val UPDATE_TRANSACTION = "/users/{user_id}/transactions/{id}"
        const val DELETE_TRANSACTION = "/users/{user_id}/transactions/{id}"
    }

    @GET(API_VERSION + TRANSACTIONS)
    suspend fun getTransactions(
        @Path("user_id") userId: String,
        @Query("month") month: String,
        @Query("year") year: String,
        @Query("page_no") pageNo: Int,
        @Query("type") type: String
    ): Response<DataResponse<TransactionsResponseModel>>

    @GET(API_VERSION + TRANSACTION_SUMMARY)
    suspend fun getTransactionSummary(
        @Path("user_id") userId: String,
        @Query("month") month: String,
        @Query("year") year: String,
        @Query("type") type: String
    ): Response<DataResponse<TransactionSummaryResponseModel>>

    @POST(API_VERSION + CREATE_TRANSACTION)
    suspend fun createTransaction(
        @Path("user_id") userId: String,
        @Body() transactionRequest: TransactionRequest
    ): Response<DataResponse<Transaction>>

    @PUT(API_VERSION + UPDATE_TRANSACTION)
    suspend fun updateTransaction(
        @Path("user_id") userId: String,
        @Path("id") id: String,
        @Body() transactionRequest: TransactionRequest
    ): Response<DataResponse<Transaction>>

    @DELETE(API_VERSION + DELETE_TRANSACTION)
    suspend fun deleteTransaction(
        @Path("user_id") userId: String,
        @Path("id") id: String
    ): Response<DataResponse<DeleteTransactionResponseModel>>
}