package co.dailybook.keep.repository

import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.boilerplate.network.model.NetworkResultStatus
import co.dailybook.keep.model.subscription.CancelSubscriptionResponse
import co.dailybook.keep.model.subscription.CreateSubscriptionRequest
import co.dailybook.keep.model.subscription.CreateSubscriptionResponse
import co.dailybook.keep.model.subscription.SubscriptionPlan
import co.dailybook.keep.model.subscription.UserSubscription
import co.dailybook.keep.model.subscription.VerifySubscriptionRequest
import co.dailybook.keep.model.subscription.VerifySubscriptionResponse
import co.dailybook.keep.network.KeepNetworkModule

class SubscriptionRepositoryImpl(
    private val networkModule: KeepNetworkModule
) : SubscriptionRepository {
    
    override suspend fun getSubscriptionPlans(userId: String): Result<List<SubscriptionPlan>> {
        return try {
            var result: Result<List<SubscriptionPlan>>? = null
            networkModule.getSubscriptionPlans(userId).collect { networkResult ->
                when (networkResult.status) {
                    NetworkResultStatus.SUCCESS -> {
                        val plans = networkResult.data?.plans ?: emptyList()
                        result = Result.success(plans)
                    }
                    NetworkResultStatus.ERROR -> {
                        result = Result.failure(Exception(networkResult.message ?: "Failed to fetch subscription plans"))
                    }
                    NetworkResultStatus.LOADING -> {
                        // Continue collecting
                    }
                }
            }
            result ?: Result.failure(Exception("No response received"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUserSubscription(userId: String): Result<UserSubscription> {
        return try {
            var result: Result<UserSubscription>? = null
            networkModule.getUserSubscription(userId).collect { networkResult ->
                when (networkResult.status) {
                    NetworkResultStatus.SUCCESS -> {
                        val subscription = networkResult.data
                        result = if (subscription != null) {
                            Result.success(subscription)
                        } else {
                            Result.failure(Exception("Subscription data is null"))
                        }
                    }
                    NetworkResultStatus.ERROR -> {
                        result = Result.failure(Exception(networkResult.message ?: "Failed to fetch user subscription"))
                    }
                    NetworkResultStatus.LOADING -> {
                        // Continue collecting
                    }
                }
            }
            result ?: Result.failure(Exception("No response received"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createSubscription(userId: String, request: CreateSubscriptionRequest): Result<CreateSubscriptionResponse> {
        return try {
            var result: Result<CreateSubscriptionResponse>? = null
            networkModule.createSubscription(userId, request).collect { networkResult ->
                when (networkResult.status) {
                    NetworkResultStatus.SUCCESS -> {
                        val subscriptionResponse = networkResult.data
                        result = if (subscriptionResponse != null) {
                            Result.success(subscriptionResponse)
                        } else {
                            Result.failure(Exception("Subscription creation response is null"))
                        }
                    }
                    NetworkResultStatus.ERROR -> {
                        result = Result.failure(Exception(networkResult.message ?: "Failed to create subscription"))
                    }
                    NetworkResultStatus.LOADING -> {
                        // Continue collecting
                    }
                }
            }
            result ?: Result.failure(Exception("No response received"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun verifySubscription(
        subscriptionId: String,
        request: VerifySubscriptionRequest
    ): Result<VerifySubscriptionResponse> {
        return try {
            var result: Result<VerifySubscriptionResponse>? = null
            networkModule.verifySubscription(subscriptionId, request).collect { networkResult ->
                when (networkResult.status) {
                    NetworkResultStatus.SUCCESS -> {
                        val verificationResponse = networkResult.data
                        result = if (verificationResponse != null) {
                            Result.success(verificationResponse)
                        } else {
                            Result.failure(Exception("Verification response is null"))
                        }
                    }
                    NetworkResultStatus.ERROR -> {
                        result = Result.failure(Exception(networkResult.message ?: "Payment verification failed"))
                    }
                    NetworkResultStatus.LOADING -> {
                        // Continue collecting
                    }
                }
            }
            result ?: Result.failure(Exception("No response received"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelSubscription(subscriptionId: String): Result<CancelSubscriptionResponse> {
        return try {
            var result: Result<CancelSubscriptionResponse>? = null
            networkModule.cancelSubscription(subscriptionId).collect { networkResult ->
                when (networkResult.status) {
                    NetworkResultStatus.SUCCESS -> {
                        val cancelResponse = networkResult.data
                        result = if (cancelResponse != null) {
                            Result.success(cancelResponse)
                        } else {
                            Result.failure(Exception("Cancel subscription response is null"))
                        }
                    }
                    NetworkResultStatus.ERROR -> {
                        result = Result.failure(Exception(networkResult.message ?: "Failed to cancel subscription"))
                    }
                    NetworkResultStatus.LOADING -> {
                        // Continue collecting
                    }
                }
            }
            result ?: Result.failure(Exception("No response received"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
