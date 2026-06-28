package co.dailybook.base.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreManager(context: Context) {

    private val Context.userPreferencesDataStore by preferencesDataStore(name = "app_datastore")

    private val dataStore = context.userPreferencesDataStore

    suspend fun <T> write(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    /**
     * Records that an interstitial was shown today (UTC).
     * Used with [INTERSTITIAL_COUNT_TODAY] and [LAST_INTERSTITIAL_EPOCH_DAY] for remote-configurable max N/day.
     */
    suspend fun recordInterstitialShown(todayEpochDay: Int) {
        dataStore.edit { preferences ->
            val lastDay = preferences[LAST_INTERSTITIAL_EPOCH_DAY] ?: -1
            val count = preferences[INTERSTITIAL_COUNT_TODAY] ?: 0
            preferences[LAST_INTERSTITIAL_EPOCH_DAY] = todayEpochDay
            preferences[INTERSTITIAL_COUNT_TODAY] = if (lastDay == todayEpochDay) count + 1 else 1
        }
    }

    fun <T> read(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_TYPE = stringPreferencesKey("user_type")
        val MOBILE_NUMBER = stringPreferencesKey("mobile_number")
        val COMPANY_ID = stringPreferencesKey("company_id")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val INT_KEY = intPreferencesKey("int_key")
        val LANGUAGE_KEY = stringPreferencesKey("language_key")
        val FIRST_TIME_APP_OPEN = booleanPreferencesKey("first_time_app_open")
        val INTERACTED_WITH_APP_FEATURES = booleanPreferencesKey("interacted_with_app_features")
        /** Epoch millis when the local paywall offer should end (fallback when RC is not configured). */
        val PREMIUM_OFFER_LOCAL_END_EPOCH_MS = longPreferencesKey("premium_offer_local_end_epoch_ms")
        val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val GOOGLE_ADS_ENABLED = booleanPreferencesKey("google_ads_enabled")
        val HOME_PAGE_ADS_ENABLED = booleanPreferencesKey("home_page_ads_enabled")
        val PRIVACY_MODE_ENABLED = booleanPreferencesKey("privacy_mode_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LAST_AUTH_TIME = stringPreferencesKey("last_auth_time")
        // Epoch day (UTC) and count for interstitial frequency (max N/day from remote config).
        val LAST_INTERSTITIAL_EPOCH_DAY = intPreferencesKey("last_interstitial_epoch_day")
        val INTERSTITIAL_COUNT_TODAY = intPreferencesKey("interstitial_count_today")
        val REFER_FRIEND_BOTTOM_SHEET_SHOWN = booleanPreferencesKey("refer_friend_bottom_sheet_shown")
        /** True after user has landed on home screen at least once (used for first-time home Meta event). */
        val HAS_SEEN_HOME_SCREEN = booleanPreferencesKey("has_seen_home_screen")
        /** Install source: "meta_ads" or "organic". Set once on first launch from Meta Install Referrer. */
        val INSTALL_SOURCE = stringPreferencesKey("install_source")
        /** Raw Meta install referrer payload (for backend decryption later). Set once when source is meta_ads. */
        val INSTALL_REFERRER_RAW = stringPreferencesKey("install_referrer_raw")
        
        // Pro subscription keys
        val PRO_STATUS = booleanPreferencesKey("pro_status")
        val PREMIUM_OFFER_LAST_SHOWN = stringPreferencesKey("premium_offer_last_shown")
        val PREMIUM_OFFER_SHOW_COUNT = intPreferencesKey("premium_offer_show_count")
        val PREMIUM_SUBSCRIPTION_ID = stringPreferencesKey("premium_subscription_id")
        val PREMIUM_PLAN_NAME = stringPreferencesKey("premium_plan_name")
        val PREMIUM_SUBSCRIPTION_STATUS = stringPreferencesKey("premium_subscription_status")
        val PREMIUM_END_DATE = stringPreferencesKey("premium_end_date")
        // Add more keys as needed
    }
}