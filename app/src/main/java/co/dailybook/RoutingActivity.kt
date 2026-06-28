package co.dailybook

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import co.dailybook.boilerplate.analytics.AnalyticsPlatforms
import co.dailybook.boilerplate.network.NetworkHandler
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import co.dailybook.base.BaseConstants
import co.dailybook.base.Headers
import co.dailybook.base.Logger
import co.dailybook.base.analytics.Analytics
import co.dailybook.base.analytics.ConstantEventAttributes
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.base.languagemanager.LanguageManager
import co.dailybook.base.navigator.ActivitiesNameEnum
import co.dailybook.base.navigator.ActivitiesNameEnum.LoginActivityEnum
import co.dailybook.base.navigator.ModuleNavigator
import co.dailybook.reminder.AlarmScheduler
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class RoutingActivity : AppCompatActivity() {

    private val moduleNavigator: ModuleNavigator by inject()
    private val dataStoreManager: DataStoreManager by inject()
    private val languageManager: LanguageManager by inject()
    private val analytics: Analytics by inject()
    private lateinit var remoteConfig: FirebaseRemoteConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routing)

        // Record App Open Event
        recordAppOpenEvent()

        // Initialize Firebase Remote Config
        initRemoteConfig()

        // Perform necessary operations in lifecycleScope
        lifecycleScope.launch {
            handleAppLaunch()
        }
    }

    private suspend fun handleAppLaunch() {
        setupAlarms()
        updateAppOpenCount()
        ensureLocalOfferEndTimestamp()
        fetchFCMToken()
        loadUserLanguage()
        delay(500)
        if (isUserLoggedIn()) {
            initializeNetworkHeaders()
            initializeAnalytics()
            navigateToHomeScreen()
        } else {
            navigateToLoginScreen()
        }
    }

    private fun initRemoteConfig() {
        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BaseConstants.DEBUG) 0 else 43200
        }
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val googleAdsEnabled = remoteConfig.getBoolean("google_ads_enabled")
                    val homePageAdsEnabled = remoteConfig.getBoolean("home_page_ads_enabled")
                    updateGoogleAdsConfig(googleAdsEnabled, homePageAdsEnabled)
                } else {
                    Logger.d("Config params updated: failed")
                }
            }

        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                if (configUpdate.updatedKeys.contains("google_ads_enabled") || 
                    configUpdate.updatedKeys.contains("home_page_ads_enabled") ||
                    configUpdate.updatedKeys.contains("custom_ad_enabled") ||
                    configUpdate.updatedKeys.contains("custom_ad_image_url") ||
                    configUpdate.updatedKeys.contains("custom_ad_redirect_url") ||
                    configUpdate.updatedKeys.contains("custom_ad_title") ||
                    configUpdate.updatedKeys.contains("subscriptions_enabled")) {
                    remoteConfig.activate().addOnCompleteListener {
                        val googleAdsEnabled = remoteConfig.getBoolean("google_ads_enabled")
                        val homePageAdsEnabled = remoteConfig.getBoolean("home_page_ads_enabled")
                        updateGoogleAdsConfig(googleAdsEnabled, homePageAdsEnabled)
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
            }
        })
    }

    private fun updateGoogleAdsConfig(googleAdsEnabled: Boolean, homePageAdsEnabled: Boolean) {
        // Store the value of "google_ads_enabled" in DataStore
        lifecycleScope.launch(Dispatchers.IO) {
            dataStoreManager.write(DataStoreManager.GOOGLE_ADS_ENABLED, googleAdsEnabled)
            dataStoreManager.write(DataStoreManager.HOME_PAGE_ADS_ENABLED, homePageAdsEnabled)
        }
    }

    private fun setupAlarms() {
        AlarmScheduler.scheduleDailyAttendanceReminder(this@RoutingActivity)
    }

    private suspend fun updateAppOpenCount() {
        val currentCount = dataStoreManager.read(DataStoreManager.APP_OPEN_COUNT, 0).first()
        dataStoreManager.write(DataStoreManager.APP_OPEN_COUNT, currentCount + 1)
    }

    /**
     * Initializes a local fallback end timestamp for the premium offer (now + 2 days)
     * the first time the app is opened on this install, so that the paywall timer
     * always has either a Remote Config value or a sane local default.
     */
    private suspend fun ensureLocalOfferEndTimestamp() {
        val existing = dataStoreManager.read(DataStoreManager.PREMIUM_OFFER_LOCAL_END_EPOCH_MS, 0L).first()
        if (existing <= 0L) {
            val now = System.currentTimeMillis()
            val twoDaysMs = 2L * 24L * 60L * 60L * 1000L
            dataStoreManager.write(DataStoreManager.PREMIUM_OFFER_LOCAL_END_EPOCH_MS, now + twoDaysMs)
        }
    }

    private fun fetchFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            val token = task.result
            CoroutineScope(Dispatchers.IO).launch {
                dataStoreManager.write(DataStoreManager.FCM_TOKEN, token)
            }
            Logger.i(token)
        })
    }

    private suspend fun loadUserLanguage() {
        languageManager.loadLocale(this@RoutingActivity)
    }

    private suspend fun isUserLoggedIn(): Boolean {
        return dataStoreManager.read(DataStoreManager.IS_LOGGED_IN, false).first()
    }

    private suspend fun initializeNetworkHeaders() {
        val headers = hashMapOf(
            Headers.COMPANY_ID to dataStoreManager.read(DataStoreManager.COMPANY_ID, "").first(),
            Headers.AUTHORIZATION to Headers.BEARER.plus(" ").plus(dataStoreManager.read(DataStoreManager.ACCESS_TOKEN, "").first()),
            Headers.USER_ID to dataStoreManager.read(DataStoreManager.USER_ID, "").first(),
            Headers.GENERIC_USER_ID to dataStoreManager.read(DataStoreManager.USER_ID, "").first()
        )
        NetworkHandler.getInstance().setAdditionalHeaders(headers)
    }

    private suspend fun initializeAnalytics() {
        val mixpanel = MixpanelAPI.getInstance(this@RoutingActivity, BaseConstants.MIXPANEL_TOKEN, true)
        val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
        mixpanel.identify(userId)
        mixpanel.people.set(ConstantEventAttributes.USER_ID, userId)
        mixpanel.people.set(ConstantEventAttributes.USER_MOBILE_NUMBER, dataStoreManager.read(DataStoreManager.MOBILE_NUMBER, "").first())
        mixpanel.people.set(ConstantEventAttributes.USER_NAME, dataStoreManager.read(DataStoreManager.USER_NAME, "").first())
        mixpanel.people.set(ConstantEventAttributes.USER_TYPE, dataStoreManager.read(DataStoreManager.USER_TYPE, "").first())
    }

    private fun navigateToHomeScreen() {
        moduleNavigator.startActivity(this, ActivitiesNameEnum.BookKeepActivityEnum)
        finish()
    }

    private fun navigateToLoginScreen() {
        moduleNavigator.startActivity(this, LoginActivityEnum)
        finish()
    }

    private fun recordAppOpenEvent() {
        analytics.logEvent(
            ConstantEventNames.APP_OPEN,
            Analytics.IMPRESSION,
            listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE)
        )
    }
}