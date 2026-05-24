package com.laborbook.keep.repository

import com.laborbook.keep.model.subscription.CancelSubscriptionResponse
import com.laborbook.keep.model.subscription.CreateSubscriptionRequest
import com.laborbook.keep.model.subscription.CreateSubscriptionResponse
import com.laborbook.keep.model.subscription.SubscriptionPlan
import com.laborbook.keep.model.subscription.UserSubscription
import com.laborbook.keep.model.subscription.VerifySubscriptionRequest
import com.laborbook.keep.model.subscription.VerifySubscriptionResponse

interface SubscriptionRepository {
    
    /**
     * Fetch all available subscription plans
     */
    suspend fun getSubscriptionPlans(userId: String): Result<List<SubscriptionPlan>>
    
    /**
     * Get user's current subscription status
     */
    suspend fun getUserSubscription(userId: String): Result<UserSubscription>
    
    /**
     * Create a new subscription
     */
    suspend fun createSubscription(userId: String, request: CreateSubscriptionRequest): Result<CreateSubscriptionResponse>
    
    /**
     * Verify subscription payment with Razorpay
     */
    suspend fun verifySubscription(
        subscriptionId: String,
        request: VerifySubscriptionRequest
    ): Result<VerifySubscriptionResponse>
    
    /**
     * Cancel subscription (will cancel at end of current billing cycle)
     */
    suspend fun cancelSubscription(subscriptionId: String): Result<CancelSubscriptionResponse>
}
