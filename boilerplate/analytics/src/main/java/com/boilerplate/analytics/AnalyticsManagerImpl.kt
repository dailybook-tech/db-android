package com.boilerplate.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.clevertap.android.sdk.CleverTapAPI
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.appsflyer.AppsFlyerLib
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.json.JSONObject

class AnalyticsManagerImpl private constructor(
    private val context: Context
) : AnalyticsManager {

    private var cleverTapAPI: CleverTapAPI? = null
    private var mixpanelAPI: MixpanelAPI? = null
    private var appsFlyerLib: AppsFlyerLib? = null
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private val enabledPlatforms = mutableSetOf<String>()

    companion object {
        @Volatile
        private var INSTANCE: AnalyticsManagerImpl? = null

        fun getInstance(context: Context): AnalyticsManagerImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalyticsManagerImpl(context).also { INSTANCE = it }
            }
        }
    }

    override fun setCleverTapInstance(instance: CleverTapAPI) {
        this.cleverTapAPI = instance
    }

    override fun setMixpanelInstance(instance: MixpanelAPI) {
        this.mixpanelAPI = instance
    }

    override fun setAppsFlyerInstance(instance: AppsFlyerLib) {
        this.appsFlyerLib = instance
    }

    override fun setFirebaseAnalyticsInstance(instance: FirebaseAnalytics) {
        this.firebaseAnalytics = instance
    }

    override fun configurePlatforms(enabledPlatforms: List<String>) {
        this.enabledPlatforms.clear()
        this.enabledPlatforms.addAll(enabledPlatforms)
    }

    override fun logEvent(eventName: String, properties: HashMap<String, Any>, eventPlatforms: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            eventPlatforms.map { platform ->
                async {
                    if (!enabledPlatforms.contains(platform)) return@async

                    when (platform) {
                        AnalyticsPlatforms.CLEVERTAP -> cleverTapAPI?.let {
                            it.pushEvent(eventName, properties)
                            logEventSuccess(platform, eventName, properties)
                        } ?: logEventFailure(platform, eventName)
                        AnalyticsPlatforms.MIXPANEL -> mixpanelAPI?.let {
                            val mixpanelProps = JSONObject(properties as Map<*, *>)
                            it.track(eventName, mixpanelProps)
                            logEventSuccess(platform, eventName, properties)
                        } ?: logEventFailure(platform, eventName)
                        AnalyticsPlatforms.APPSFLYER -> appsFlyerLib?.let {
                            it.logEvent(context, eventName, properties)
                            logEventSuccess(platform, eventName, properties)
                        } ?: logEventFailure(platform, eventName)
                        AnalyticsPlatforms.FIREBASE -> firebaseAnalytics?.let {
                            val bundle = Bundle().apply {
                                for ((key, value) in properties) {
                                    when (value) {
                                        is String -> putString(key, value)
                                        is Int -> putInt(key, value)
                                        is Double -> putDouble(key, value)
                                        is Boolean -> putBoolean(key, value)
                                        is Bundle -> putBundle(key, value)
                                    }
                                }
                            }
                            it.logEvent(eventName, bundle)
                            logEventSuccess(platform, eventName, properties)
                        } ?: logEventFailure(platform, eventName)
                    }
                }
            }.awaitAll()
        }
    }

    override fun setUserProperties(userProperties: Map<String, Any>, userPropertyPlatforms: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            userPropertyPlatforms.map { platform ->
                async {
                    if (!enabledPlatforms.contains(platform)) return@async

                    when (platform) {
                        AnalyticsPlatforms.CLEVERTAP -> cleverTapAPI?.let {
                            it.pushProfile(userProperties)
                            logUserPropertiesSuccess(platform, userProperties)
                        } ?: logUserPropertiesFailure(platform)
                        AnalyticsPlatforms.MIXPANEL -> mixpanelAPI?.let {
                            val mixpanelProps = JSONObject(userProperties as Map<*, *>)
                            it.people.set(mixpanelProps)
                            logUserPropertiesSuccess(platform, userProperties)
                        } ?: logUserPropertiesFailure(platform)
                        AnalyticsPlatforms.APPSFLYER -> appsFlyerLib?.let {
                            it.setAdditionalData(userProperties)
                            logUserPropertiesSuccess(platform, userProperties)
                        } ?: logUserPropertiesFailure(platform)
                        AnalyticsPlatforms.FIREBASE -> firebaseAnalytics?.let {
                            userProperties.forEach { (key, value) ->
                                it.setUserProperty(key, value.toString())
                            }
                            logUserPropertiesSuccess(platform, userProperties)
                        } ?: logUserPropertiesFailure(platform)
                    }
                }
            }.awaitAll()
        }
    }

    private fun logEventSuccess(platform: String, eventName: String, properties: Map<String, Any>) {
        Log.d("AnalyticsManager", "Event $eventName successfully sent to $platform with properties: $properties")
    }

    private fun logEventFailure(platform: String, eventName: String) {
        Log.w("AnalyticsManager", "$platform instance not set or platform not enabled, skipping event $eventName")
    }

    private fun logUserPropertiesSuccess(platform: String, userProperties: Map<String, Any>) {
        Log.d("AnalyticsManager", "User properties successfully sent to $platform with properties: $userProperties")
    }

    private fun logUserPropertiesFailure(platform: String) {
        Log.w("AnalyticsManager", "$platform instance not set or platform not enabled, skipping user properties update")
    }
}