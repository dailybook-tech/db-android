package co.dailybook.base.datastore

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import co.dailybook.base.datastore.DataStoreManager.Companion.GOOGLE_ADS_ENABLED
import co.dailybook.base.datastore.DataStoreManager.Companion.HOME_PAGE_ADS_ENABLED
import co.dailybook.base.datastore.DataStoreManager.Companion.PRO_STATUS
import co.dailybook.base.utils.SubscriptionsFeatureFlag
import kotlinx.coroutines.flow.first

/**
 * Extension functions for DataStoreManager to check ad visibility based on Pro status
 */

/**
 * Check if Google ads should be shown
 * Ads are only shown if:
 * 1. User is NOT Pro (free user) - OR subscriptions feature is disabled (show ads to everyone)
 * 2. Google ads are enabled
 */
suspend fun DataStoreManager.shouldShowGoogleAds(): Boolean {
    val adsEnabled = read(GOOGLE_ADS_ENABLED, true).first()
    if (!adsEnabled) return false
    
    // If subscriptions feature is disabled, show ads to everyone (old version behavior)
    val remoteConfig = Firebase.remoteConfig
    if (!SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
        return true
    }
    
    // If subscriptions are enabled, check Pro status
    val isPro = read(PRO_STATUS, false).first()
    return !isPro
}

/**
 * Check if home page ads should be shown
 * Home page ads are only shown if:
 * 1. User is NOT Pro (free user) - OR subscriptions feature is disabled (show ads to everyone)
 * 2. Home page ads are enabled
 */
suspend fun DataStoreManager.shouldShowHomePageAds(): Boolean {
    val homePageAdsEnabled = read(HOME_PAGE_ADS_ENABLED, true).first()
    if (!homePageAdsEnabled) return false
    
    // If subscriptions feature is disabled, show ads to everyone (old version behavior)
    val remoteConfig = Firebase.remoteConfig
    if (!SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
        return true
    }
    
    // If subscriptions are enabled, check Pro status
    val isPro = read(PRO_STATUS, false).first()
    return !isPro
}
