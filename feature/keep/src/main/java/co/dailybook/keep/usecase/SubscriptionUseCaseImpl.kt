package co.dailybook.keep.usecase

import co.dailybook.keep.model.subscription.CancelSubscriptionResponse
import co.dailybook.keep.model.subscription.CreateSubscriptionRequest
import co.dailybook.keep.model.subscription.CreateSubscriptionResponse
import co.dailybook.keep.model.subscription.SubscriptionPlan
import co.dailybook.keep.model.subscription.UserSubscription
import co.dailybook.keep.model.subscription.VerifySubscriptionRequest
import co.dailybook.keep.repository.SubscriptionRepository

class SubscriptionUseCaseImpl(
    private val subscriptionRepository: SubscriptionRepository
) : SubscriptionUseCase {
    
    override suspend fun getAvailablePlans(userId: String): Result<List<SubscriptionPlan>> {
        return subscriptionRepository.getSubscriptionPlans(userId)
    }
    
    override suspend fun checkSubscriptionStatus(userId: String): Result<UserSubscription> {
        return subscriptionRepository.getUserSubscription(userId)
    }
    
    override suspend fun initiateSubscription(
        userId: String,
        planId: String,
        upiId: String,
        hasTrial: Boolean
    ): Result<CreateSubscriptionResponse> {
        val request = CreateSubscriptionRequest(
            planId = planId,
            paymentMethod = "upi",
            upiId = upiId
        )
        return subscriptionRepository.createSubscription(userId, request)
    }
    
    override suspend fun verifyPayment(
        subscriptionId: String,
        paymentData: VerifySubscriptionRequest
    ): Result<Boolean> {
        return try {
            val result = subscriptionRepository.verifySubscription(subscriptionId, paymentData)
            result.map { it.verified }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelSubscription(subscriptionId: String): Result<CancelSubscriptionResponse> {
        return subscriptionRepository.cancelSubscription(subscriptionId)
    }
}
