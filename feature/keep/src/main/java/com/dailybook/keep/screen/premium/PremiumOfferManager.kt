package com.dailybook.keep.screen.premium

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.keep.model.subscription.SubscriptionDetails
import com.dailybook.keep.model.subscription.UserSubscription
import com.dailybook.keep.utils.SubscriptionsFeatureFlag
import kotlinx.coroutines.flow.first

class PremiumOfferManager(
    private val dataStoreManager: DataStoreManager
) {
    companion object {
        // Show offer every time app opens (can be changed to show after X days or X app opens)
        private const val SHOW_AFTER_DAYS = 0 // 0 means show every time
        private const val MAX_SHOW_COUNT = 100 // Maximum times to show the offer
    }

    /**
     * Check if we should show the Pro offer dialog
     */
    suspend fun shouldShowPremiumOffer(): Boolean {
        // First check if subscriptions feature is enabled via Remote Config
        val remoteConfig = Firebase.remoteConfig
        if (!SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
            return false
        }
        
        // Don't show if user is already Pro
        val isPro = dataStoreManager.read(DataStoreManager.PRO_STATUS, false).first()
        if (isPro) {
            return false
        }

        // Check show count
        val showCount = dataStoreManager.read(DataStoreManager.PREMIUM_OFFER_SHOW_COUNT, 0).first()
        if (showCount >= MAX_SHOW_COUNT) {
            return false
        }

        // Check last shown timestamp
        val lastShownTimestamp = dataStoreManager.read(DataStoreManager.PREMIUM_OFFER_LAST_SHOWN, "0").first().toLongOrNull() ?: 0L
        val currentTimestamp = System.currentTimeMillis()
        val daysSinceLastShown = (currentTimestamp - lastShownTimestamp) / (1000 * 60 * 60 * 24)

        return daysSinceLastShown >= SHOW_AFTER_DAYS
    }

    /**
     * Show the Pro offer dialog
     */
    suspend fun showPremiumOfferDialog(activity: FragmentActivity) {
        if (!shouldShowPremiumOffer()) {
            return
        }

        // Update show count and timestamp
        val showCount = dataStoreManager.read(DataStoreManager.PREMIUM_OFFER_SHOW_COUNT, 0).first()
        dataStoreManager.write(DataStoreManager.PREMIUM_OFFER_SHOW_COUNT, showCount + 1)
        dataStoreManager.write(DataStoreManager.PREMIUM_OFFER_LAST_SHOWN, System.currentTimeMillis().toString())

        // Show dialog
        val dialog = PremiumOfferDialogFragment.newInstance()
        dialog.show(activity.supportFragmentManager, PremiumOfferDialogFragment.TAG)
    }

    /**
     * Mark user as Pro (call this after successful payment)
     */
    suspend fun markUserAsPremium() {
        dataStoreManager.write(DataStoreManager.PRO_STATUS, true)
    }

    /**
     * Check if user is Pro
     */
    suspend fun isPremiumUser(): Boolean {
        return dataStoreManager.read(DataStoreManager.PRO_STATUS, false).first()
    }

    /**
     * Reset Pro status (for testing purposes)
     */
    suspend fun resetPremiumStatus() {
        dataStoreManager.write(DataStoreManager.PRO_STATUS, false)
        dataStoreManager.write(DataStoreManager.PREMIUM_OFFER_SHOW_COUNT, 0)
        dataStoreManager.write(DataStoreManager.PREMIUM_OFFER_LAST_SHOWN, "0")
    }
    
    /**
     * Update subscription status from API response
     */
    suspend fun updateSubscriptionStatus(userSubscription: UserSubscription) {
        // Check subscription tier (case-insensitive to handle any variations)
        val isPro = userSubscription.subscriptionTier.equals("PRO", ignoreCase = true)
        dataStoreManager.write(DataStoreManager.PRO_STATUS, isPro)
        
        // Debug: Log the subscription status update
        com.dailybook.base.Logger.d("Subscription status updated: tier=${userSubscription.subscriptionTier}, isPro=$isPro")
        
        // Cache subscription details if available
        userSubscription.subscription?.let { subscription ->
            cacheSubscriptionData(subscription)
        }
    }
    
    /**
     * Cache subscription details for offline access
     */
    private suspend fun cacheSubscriptionData(subscription: SubscriptionDetails) {
        // Store subscription details in DataStore for offline access
        dataStoreManager.write(
            DataStoreManager.PREMIUM_SUBSCRIPTION_ID,
            subscription.id
        )
        dataStoreManager.write(
            DataStoreManager.PREMIUM_PLAN_NAME,
            subscription.planName
        )
        dataStoreManager.write(
            DataStoreManager.PREMIUM_SUBSCRIPTION_STATUS,
            subscription.status
        )
        dataStoreManager.write(
            DataStoreManager.PREMIUM_END_DATE,
            subscription.endAt
        )
    }
}

