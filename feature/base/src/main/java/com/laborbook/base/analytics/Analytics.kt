package com.laborbook.base.analytics

import com.boilerplate.analytics.AnalyticsManager
import com.boilerplate.analytics.AnalyticsPlatforms
import com.laborbook.base.BaseConstants
import com.laborbook.base.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class Analytics(val analyticsManager: AnalyticsManager, val dataStoreManager: DataStoreManager) {

    companion object {
        //Sub type
        const val SYSTEM = "system"
        const val IMPRESSION = "impression"
        const val CLICK = "click"

        /**
         * Only these events are sent to Mixpanel (with _v2 suffix).
         * Supports: DAU, MAU, meta/organic installs, onboarding funnel, subscription funnel.
         */
        private val MIXPANEL_EVENT_TO_V2: Map<String, String> = mapOf(
            ConstantEventNames.APP_OPEN to "app_open_v2",
            ConstantEventNames.MOBILE_OTP_TRUECALLER to "mobile_otp_truecaller_v2",
            ConstantEventNames.ADD_LABOR_MANUAL to "added_labor_v2",
            ConstantEventNames.ADD_LABOR_FROM_CONTACTS to "added_labor_v2",
            ConstantEventNames.LABOR_REPORTS_TAP to "labor_reports_tap_v2",
            ConstantEventNames.SAVE_EXPENSE to "expense_added_v2",
            ConstantEventNames.SAVE_INCOME to "income_added_v2",
            ConstantEventNames.START_TRIAL_CLICK to "trial_v2",
            ConstantEventNames.SUBSCRIPTION_ACTIVATED to "subscribe_v2",
            "subscription_activated" to "subscribe_v2"
        )
    }

    /**
     * Logs event to Firebase (all events) and to Mixpanel only for allowed events (with _v2 suffix).
     * Old/other events are never sent to Mixpanel — only the events in [MIXPANEL_EVENT_TO_V2].
     */
    fun logEvent(eventName: String, eventType: String, eventPlatforms: List<String>, properties: HashMap<String, Any>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mergedProperties = mergeSuperProperties(eventType, properties)
                // Firebase: all events with original name
                val firebasePlatforms = if (eventPlatforms.contains(AnalyticsPlatforms.FIREBASE)) listOf(AnalyticsPlatforms.FIREBASE) else emptyList()
                if (firebasePlatforms.isNotEmpty()) {
                    analyticsManager.logEvent(eventName, mergedProperties, firebasePlatforms)
                }
                // Mixpanel: only allowed events, with _v2 suffix; all other events are not sent
                val mixpanelV2Name = MIXPANEL_EVENT_TO_V2[eventName]
                if (eventPlatforms.contains(AnalyticsPlatforms.MIXPANEL) && mixpanelV2Name != null) {
                    analyticsManager.logEvent(mixpanelV2Name, mergedProperties, listOf(AnalyticsPlatforms.MIXPANEL))
                }
            } catch (ex: Exception) {
                // Silently handle
            }
        }
    }

    // Function to merge super properties with the passed hashmap
    private suspend fun mergeSuperProperties(eventType: String, hashMap: HashMap<String, Any>?): HashMap<String, Any> {
        // Define your super properties, including eventType
        val superProperties = hashMapOf(
            ConstantEventAttributes.USER_ID to dataStoreManager.read(DataStoreManager.USER_ID, "").first(),
            ConstantEventAttributes.USER_NAME to dataStoreManager.read(DataStoreManager.USER_NAME, "").first(),
            ConstantEventAttributes.USER_MOBILE_NUMBER to dataStoreManager.read(DataStoreManager.MOBILE_NUMBER, "").first(),
            ConstantEventAttributes.USER_TYPE to dataStoreManager.read(DataStoreManager.USER_TYPE, "").first(),
            ConstantEventAttributes.APP_OPEN_COUNT to dataStoreManager.read(DataStoreManager.APP_OPEN_COUNT, 1).first(),
            ConstantEventAttributes.SELECTED_LANGUAGE to dataStoreManager.read(DataStoreManager.LANGUAGE_KEY, "en").first(),
            ConstantEventAttributes.APP_VERSION to BaseConstants.APP_VERSION,
            ConstantEventAttributes.EVENT_TYPE to eventType,
            ConstantEventAttributes.INSTALL_SOURCE to dataStoreManager.read(DataStoreManager.INSTALL_SOURCE, "organic").first()
        )

        // Merge superProperties with hashMap if hashMap is not null
        return if (hashMap != null) {
            superProperties + hashMap
        } else {
            superProperties
        } as HashMap<String, Any> // Cast the result back to HashMap
    }
}