package com.dailybook.keep.model

import com.google.gson.annotations.SerializedName

data class AdvanceEntryResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("date") val date: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("reason") val reason: String?,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("manager_id") val managerId: String,
    @SerializedName("created_at") val createdAt: String?
)
