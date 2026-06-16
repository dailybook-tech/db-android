package com.dailybook.keep.screen.premium.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.keep.model.subscription.CancelSubscriptionResponse
import com.dailybook.keep.model.subscription.CreateSubscriptionResponse
import com.dailybook.keep.model.subscription.SubscriptionPlan
import com.dailybook.keep.model.subscription.UserSubscription
import com.dailybook.keep.model.subscription.VerifySubscriptionRequest
import com.dailybook.keep.screen.premium.PremiumOfferManager
import com.dailybook.keep.usecase.SubscriptionUseCase
import com.dailybook.keep.utils.SubscriptionsFeatureFlag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val subscriptionUseCase: SubscriptionUseCase,
    private val premiumOfferManager: PremiumOfferManager,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    
    private val _subscriptionState = MutableLiveData<SubscriptionState>()
    val subscriptionState: LiveData<SubscriptionState> = _subscriptionState
    
    private val _subscriptionPlans = MutableLiveData<List<SubscriptionPlan>>()
    val subscriptionPlans: LiveData<List<SubscriptionPlan>> = _subscriptionPlans
    
    private val _userSubscription = MutableLiveData<UserSubscription?>()
    val userSubscription: LiveData<UserSubscription?> = _userSubscription
    
    sealed class SubscriptionState {
        object Idle : SubscriptionState()
        object Loading : SubscriptionState()
        data class PlansLoaded(val plans: List<SubscriptionPlan>) : SubscriptionState()
        data class SubscriptionCreated(val response: CreateSubscriptionResponse) : SubscriptionState()
        data class SubscriptionVerified(val isActive: Boolean) : SubscriptionState()
        data class SubscriptionCancelled(val response: CancelSubscriptionResponse) : SubscriptionState()
        data class Error(val message: String) : SubscriptionState()
        data class UserSubscriptionLoaded(val subscription: UserSubscription) : SubscriptionState()
    }
    
    /**
     * Load all available subscription plans
     */
    fun loadSubscriptionPlans(userId: String) {
        viewModelScope.launch {
            // Check if subscriptions feature is enabled via Remote Config
            val remoteConfig = Firebase.remoteConfig
            if (!SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
                _subscriptionState.value = SubscriptionState.Error("Subscriptions feature is not available")
                return@launch
            }
            
            _subscriptionState.value = SubscriptionState.Loading
            
            subscriptionUseCase.getAvailablePlans(userId)
                .onSuccess { plans ->
                    _subscriptionPlans.value = plans
                    _subscriptionState.value = SubscriptionState.PlansLoaded(plans)
                }
                .onFailure { error ->
                    _subscriptionState.value = SubscriptionState.Error(
                        error.message ?: "Failed to load subscription plans"
                    )
                }
        }
    }
    
    /**
     * Check user's subscription status
     */
    fun checkUserSubscriptionStatus(userId: String) {
        viewModelScope.launch {
            _subscriptionState.value = SubscriptionState.Loading
            
            subscriptionUseCase.checkSubscriptionStatus(userId)
                .onSuccess { subscription ->
                    _userSubscription.value = subscription
                    _subscriptionState.value = SubscriptionState.UserSubscriptionLoaded(subscription)
                    
                    // Update Pro status in DataStore
                    premiumOfferManager.updateSubscriptionStatus(subscription)
                }
                .onFailure { error ->
                    _subscriptionState.value = SubscriptionState.Error(
                        error.message ?: "Failed to check subscription status"
                    )
                }
        }
    }
    
    /**
     * Create a new subscription.
     */
    fun createSubscription(userId: String, planId: String, upiId: String, hasTrial: Boolean = false) {
        viewModelScope.launch {
            // Check if subscriptions feature is enabled via Remote Config
            val remoteConfig = Firebase.remoteConfig
            if (!SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
                _subscriptionState.value = SubscriptionState.Error("Subscriptions feature is not available")
                return@launch
            }
            
            _subscriptionState.value = SubscriptionState.Loading
            
            subscriptionUseCase.initiateSubscription(userId, planId, upiId, hasTrial)
                .onSuccess { response ->
                    _subscriptionState.value = SubscriptionState.SubscriptionCreated(response)
                }
                .onFailure { error ->
                    _subscriptionState.value = SubscriptionState.Error(
                        error.message ?: "Failed to create subscription"
                    )
                }
        }
    }
    
    /**
     * Verify subscription payment
     */
    fun verifySubscription(subscriptionId: String, razorpayData: VerifySubscriptionRequest) {
        viewModelScope.launch {
            _subscriptionState.value = SubscriptionState.Loading
            
            subscriptionUseCase.verifyPayment(subscriptionId, razorpayData)
                .onSuccess { isVerified ->
                    if (isVerified) {
                        // Mark user as Pro locally
                        premiumOfferManager.markUserAsPremium()
                        
                        // Don't refresh subscription status immediately - let the success dialog handle it
                        // This avoids race condition where API might not have processed subscription yet
                    }
                    _subscriptionState.value = SubscriptionState.SubscriptionVerified(isVerified)
                }
                .onFailure { error ->
                    _subscriptionState.value = SubscriptionState.Error(
                        error.message ?: "Payment verification failed"
                    )
                }
        }
    }
    
    /**
     * Cancel subscription (will cancel at end of current billing cycle)
     */
    fun cancelSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _subscriptionState.value = SubscriptionState.Loading
            
            subscriptionUseCase.cancelSubscription(subscriptionId)
                .onSuccess { response ->
                    _subscriptionState.value = SubscriptionState.SubscriptionCancelled(response)
                    
                    // Refresh user subscription status to get updated status
                    val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
                    if (userId.isNotEmpty()) {
                        checkUserSubscriptionStatus(userId)
                    }
                }
                .onFailure { error ->
                    _subscriptionState.value = SubscriptionState.Error(
                        error.message ?: "Failed to cancel subscription"
                    )
                }
        }
    }
    
    /**
     * Reset state to idle
     */
    fun resetState() {
        _subscriptionState.value = SubscriptionState.Idle
    }
}
