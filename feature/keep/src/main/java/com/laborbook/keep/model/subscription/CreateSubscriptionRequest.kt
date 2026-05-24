package com.laborbook.keep.model.subscription

import com.google.gson.annotations.SerializedName

data class CreateSubscriptionRequest(
    @SerializedName("plan_id")
    val planId: String,
    
    @SerializedName("payment_method")
    val paymentMethod: String,
    
    @SerializedName("upi_id")
    val upiId: String
)

data class CreateSubscriptionResponse(
    @SerializedName("subscription_id")
    val subscriptionId: String,
    
    @SerializedName("pg_subscription_id")
    val pgSubscriptionId: String,
    
    @SerializedName("short_url")
    val shortUrl: String? = null,
    
    @SerializedName("payment_link")
    val paymentLink: String? = null,
    
    /** UPI deep link (upi://pay?...) — when present, app opens UPI app directly. */
    @SerializedName("upi_intent_uri")
    val upiIntentUri: String? = null,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("message")
    val message: String
)
