package com.boilerplate.analytics

import com.appsflyer.AppsFlyerLib
import com.clevertap.android.sdk.CleverTapAPI
import com.google.firebase.analytics.FirebaseAnalytics
import com.mixpanel.android.mpmetrics.MixpanelAPI

interface AnalyticsManager {
    fun configurePlatforms(enabledPlatforms: List<String>)
    fun logEvent(eventName: String, properties: HashMap<String, Any>, eventPlatforms: List<String>)
    fun setCleverTapInstance(instance: CleverTapAPI)
    fun setMixpanelInstance(instance: MixpanelAPI)
    fun setAppsFlyerInstance(instance: AppsFlyerLib)
    fun setFirebaseAnalyticsInstance(instance: FirebaseAnalytics)
    fun setUserProperties(userProperties: Map<String, Any>, userPropertyPlatforms: List<String>)
}