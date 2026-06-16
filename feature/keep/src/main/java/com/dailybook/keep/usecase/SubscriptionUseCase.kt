package com.dailybook.keep.usecase

import com.dailybook.keep.model.subscription.CancelSubscriptionResponse
import com.dailybook.keep.model.subscription.CreateSubscriptionResponse
import com.dailybook.keep.model.subscription.SubscriptionPlan
import com.dailybook.keep.model.subscription.UserSubscription
import com.dailybook.keep.model.subscription.VerifySubscriptionRequest

interface SubscriptionUseCase {
    
    /**
     * Get all available subscription plans
     */
    suspend fun getAvailablePlans(userId: String): Result<List<SubscriptionPlan>>
    
    /**
     * Check user's subscription status
     */
    suspend fun checkSubscriptionStatus(userId: String): Result<UserSubscription>
    
    /**
     * Initiate a new subscription.
     * @param hasTrial When true, request body includes addon for UPI Mandate (amount 5 INR, refunded so UI shows 0).
     */
    suspend fun initiateSubscription(
        userId: String,
        planId: String,
        upiId: String,
        hasTrial: Boolean = false
    ): Result<CreateSubscriptionResponse>
    
    /**
     * Verify payment after Razorpay callback
     */
    suspend fun verifyPayment(
        subscriptionId: String,
        paymentData: VerifySubscriptionRequest
    ): Result<Boolean>
    
    /**
     * Cancel subscription (will cancel at end of current billing cycle)
     */
    suspend fun cancelSubscription(subscriptionId: String): Result<CancelSubscriptionResponse>
}
