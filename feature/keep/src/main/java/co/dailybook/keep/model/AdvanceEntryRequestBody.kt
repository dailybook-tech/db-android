package co.dailybook.keep.model

import com.google.gson.annotations.SerializedName

data class AdvanceEntryRequestBody(
    @SerializedName("date") val date: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("manager_id") val managerId: String
)
