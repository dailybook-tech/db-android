package co.dailybook.keep.model

import com.google.gson.annotations.SerializedName

data class AddAdvanceRequestBody(
    val advance: Advance,
    @SerializedName("manager_id")
    val managerId: String,
)

data class Advance(
    @SerializedName("advance_date")
    val advanceDate: String,
    @SerializedName("advance_amount")
    val advanceAmount: Int,
    @SerializedName("advance_reason")
    val advanceReason: String,
    @SerializedName("advance_payment_method")
    val advancePaymentMethod: String? = null,
)