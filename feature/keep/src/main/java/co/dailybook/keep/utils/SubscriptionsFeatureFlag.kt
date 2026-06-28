package co.dailybook.keep.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import co.dailybook.base.utils.SubscriptionsFeatureFlag as BaseSubscriptionsFeatureFlag

/**
 * Utility class to check if subscriptions feature is enabled via Remote Config.
 * Remote Config handles percentage-based gradual rollout automatically.
 * 
 * @deprecated Use co.dailybook.base.utils.SubscriptionsFeatureFlag instead
 * This file is kept for backward compatibility and will delegate to the base module version.
 */
@Deprecated("Use co.dailybook.base.utils.SubscriptionsFeatureFlag instead", ReplaceWith("BaseSubscriptionsFeatureFlag"))
object SubscriptionsFeatureFlag {
    
    /**
     * Check if subscriptions feature is enabled from Remote Config.
     * Remote Config will automatically handle percentage rollout based on Firebase Console settings.
     * 
     * @param remoteConfig Firebase Remote Config instance
     * @return true if subscriptions are enabled for this user, false otherwise
     */
    fun isSubscriptionsEnabled(remoteConfig: FirebaseRemoteConfig): Boolean {
        return BaseSubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)
    }

    /**
     * Maximum number of staff (labors) visible for free users. Staff beyond this limit are locked.
     * Delegates to [BaseSubscriptionsFeatureFlag.getFreeUserMaxStaffCount].
     */
    fun getFreeUserMaxStaffCount(remoteConfig: FirebaseRemoteConfig): Int {
        return BaseSubscriptionsFeatureFlag.getFreeUserMaxStaffCount(remoteConfig)
    }
}
