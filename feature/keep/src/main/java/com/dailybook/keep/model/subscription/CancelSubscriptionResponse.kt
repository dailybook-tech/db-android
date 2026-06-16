package com.dailybook.keep.model.subscription

import com.google.gson.annotations.SerializedName

data class CancelSubscriptionResponse(
    @SerializedName("subscription_id")
    val subscriptionId: String,
    
    @SerializedName("pg_subscription_id")
    val pgSubscriptionId: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("cancel_at_cycle_end")
    val cancelAtCycleEnd: Boolean
)
