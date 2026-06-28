package co.dailybook.keep.model.subscription

import com.google.gson.annotations.SerializedName

data class VerifySubscriptionRequest(
    @SerializedName("razorpay_subscription_id")
    val razorpaySubscriptionId: String,
    
    @SerializedName("razorpay_payment_id")
    val razorpayPaymentId: String,
    
    @SerializedName("razorpay_signature")
    val razorpaySignature: String
)

data class VerifySubscriptionResponse(
    @SerializedName("subscription_id")
    val subscriptionId: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("verified")
    val verified: Boolean,
    
    @SerializedName("message")
    val message: String
)
