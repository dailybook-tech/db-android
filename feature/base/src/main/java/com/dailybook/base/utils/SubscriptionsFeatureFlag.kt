package com.dailybook.base.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

/**
 * Utility class to check if subscriptions feature is enabled via Remote Config.
 * Remote Config handles percentage-based gradual rollout automatically.
 */
object SubscriptionsFeatureFlag {
    
    /**
     * Check if subscriptions feature is enabled from Remote Config.
     * Remote Config will automatically handle percentage rollout based on Firebase Console settings.
     * 
     * @param remoteConfig Firebase Remote Config instance
     * @return true if subscriptions are enabled for this user, false otherwise
     */
    fun isSubscriptionsEnabled(remoteConfig: FirebaseRemoteConfig): Boolean {
        return remoteConfig.getBoolean("subscriptions_enabled")
    }

    /**
     * Maximum number of staff (labors) visible for free users. Staff beyond this limit are locked.
     * Configured via Remote Config key "free_user_max_staff_count" (default 1).
     *
     * @param remoteConfig Firebase Remote Config instance
     * @return max staff count for free users, clamped to 1..100
     */
    fun getFreeUserMaxStaffCount(remoteConfig: FirebaseRemoteConfig): Int {
        return remoteConfig.getLong("free_user_max_staff_count").toInt().coerceIn(1, 100)
    }
}
