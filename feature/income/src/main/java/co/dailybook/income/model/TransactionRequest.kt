package co.dailybook.income.model

import com.google.gson.annotations.SerializedName

data class TransactionRequest(
    val reason: String,
    @SerializedName("date_time")
    val date: String,
    val amount: Double,
    val type: String,
    @SerializedName("transaction_mode")
    val transactionMode: String,
    @SerializedName("payment_method")
    val paymentMethod: String,
)
