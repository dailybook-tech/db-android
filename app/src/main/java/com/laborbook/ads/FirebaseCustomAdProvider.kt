package com.laborbook.ads

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.laborbook.base.ads.CustomAdData
import com.laborbook.base.ads.CustomAdProvider

/**
 * Firebase Remote Config implementation of CustomAdProvider
 */
class FirebaseCustomAdProvider : CustomAdProvider {
    
    private val remoteConfig = Firebase.remoteConfig
    
    override fun getCustomAdData(): CustomAdData {
        return try {
            CustomAdData(
                isEnabled = remoteConfig.getBoolean("custom_ad_enabled"),
                imageUrl = remoteConfig.getString("custom_ad_image_url"),
                redirectUrl = remoteConfig.getString("custom_ad_redirect_url"),
                title = remoteConfig.getString("custom_ad_title")
            )
        } catch (e: Exception) {
            // Return default values if there's an error
            CustomAdData(
                isEnabled = false,
                imageUrl = "",
                redirectUrl = "",
                title = ""
            )
        }
    }
}
