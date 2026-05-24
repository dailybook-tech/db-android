package com.laborbook.keep.model.subscription

import com.google.gson.annotations.SerializedName

data class SubscriptionPlan(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("pg_plan_id")
    val pgPlanId: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("price")
    val price: Int,
    
    @SerializedName("discounted_price")
    val discountedPrice: Int,
    
    @SerializedName("currency")
    val currency: String,
    
    @SerializedName("interval")
    val interval: String,
    
    @SerializedName("interval_count")
    val intervalCount: Int,
    
    @SerializedName("trial_days")
    val trialDays: Int,
    
    @SerializedName("meta_data")
    val metaData: MetaData,
    
    @SerializedName("is_active")
    val isActive: Boolean
) {
    // Calculate discount percentage
    val discountPercent: Int
        get() = if (price > 0 && discountedPrice < price) {
            ((price - discountedPrice) * 100 / price)
        } else {
            0
        }
    
    // Check if there's a discount
    val hasDiscount: Boolean
        get() = discountedPrice < price
    
    // Check if there's a trial period
    val hasTrial: Boolean
        get() = trialDays > 0
}

data class MetaData(
    @SerializedName("features")
    val features: List<String>
)

data class SubscriptionPlansResponse(
    @SerializedName("plans")
    val plans: List<SubscriptionPlan>
)
