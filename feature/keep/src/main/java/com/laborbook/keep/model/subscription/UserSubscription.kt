package com.laborbook.keep.model.subscription

import com.google.gson.annotations.SerializedName

data class UserSubscription(
    @SerializedName("subscription_tier")
    val subscriptionTier: String, // "FREE" or "PRO"
    
    @SerializedName("subscription")
    val subscription: SubscriptionDetails?,
    
    @SerializedName("features")
    val features: List<Feature>
)

data class SubscriptionDetails(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("plan_id")
    val planId: String,
    
    @SerializedName("plan_name")
    val planName: String,
    
    @SerializedName("pg_subscription_id")
    val pgSubscriptionId: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("start_at")
    val startAt: String,
    
    @SerializedName("end_at")
    val endAt: String,
    
    @SerializedName("payment_method")
    val paymentMethod: String,
    
    @SerializedName("amount_paid")
    val amount: Int? = null,  // Amount in rupees (optional, for backward compatibility)
    
    @SerializedName("interval")
    val interval: String? = null  // "monthly" or "yearly" (optional)
)

data class Feature(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("has_access")
    val hasAccess: Boolean
)
