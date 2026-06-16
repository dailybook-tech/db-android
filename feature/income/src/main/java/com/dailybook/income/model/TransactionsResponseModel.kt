package com.dailybook.income.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionsResponseModel(
    val transactions: List<Transaction>,
    @SerializedName("is_last_page")
    val isLastPage: Boolean,
)

@Parcelize
data class Transaction(
    val id: String,
    val reason: String,
    val date: String,
    @SerializedName("date_str")
    val dateStr: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("transaction_mode")
    val transactionMode: String,
    @SerializedName("payment_method")
    val paymentMethod: String? = null,
    @SerializedName("type")
    val type: String,
) : Parcelable {
    // Function to get only the formatted date without time
    val formattedDate: String
        get() = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val dateObj = sdf.parse(date)
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dateObj ?: Date())
        } catch (e: Exception) {
            dateStr // Fallback to the original string in case of an error
        }
}
