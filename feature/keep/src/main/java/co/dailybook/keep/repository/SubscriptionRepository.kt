package co.dailybook.keep.repository

import co.dailybook.keep.model.subscription.CancelSubscriptionResponse
import co.dailybook.keep.model.subscription.CreateSubscriptionRequest
import co.dailybook.keep.model.subscription.CreateSubscriptionResponse
import co.dailybook.keep.model.subscription.SubscriptionPlan
import co.dailybook.keep.model.subscription.UserSubscription
import co.dailybook.keep.model.subscription.VerifySubscriptionRequest
import co.dailybook.keep.model.subscription.VerifySubscriptionResponse

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
