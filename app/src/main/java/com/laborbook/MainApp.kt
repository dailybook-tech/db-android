package com.laborbook

import android.app.Application
import android.provider.Settings
import com.boilerplate.analytics.AnalyticsManager
import com.boilerplate.analytics.AnalyticsPlatforms
import com.boilerplate.network.NetworkHandler
import com.boilerplate.uikit.ThemeUtils
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.inmobi.sdk.InMobiSdk
import com.inmobi.sdk.SdkInitializationListener
import com.laborbook.GooglePlayInstallReferrerReader
import com.laborbook.auth.di.authModule
import com.laborbook.base.BaseConstants
import com.laborbook.base.Logger
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.di.appModule
import com.laborbook.expense.di.expenseModule
import com.laborbook.income.di.incomeModule
import com.laborbook.keep.di.keepModule
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApp : Application() {

    private val analyticsManager: AnalyticsManager by inject()
    private val dataStoreManager: DataStoreManager by inject()

    override fun onCreate() {
        super.onCreate()
        ThemeUtils.applyTheme()
        startKoin {
            androidContext(this@MainApp)
            modules(appModule)
            modules(authModule)
            modules(keepModule)
            modules(expenseModule)
            modules(incomeModule)
        }
        FirebaseApp.initializeApp(this@MainApp)
        MobileAds.initialize(this)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)
        analyticsManager.setFirebaseAnalyticsInstance(firebaseAnalytics)

        val mixpanelAPI = MixpanelAPI.getInstance(this, BaseConstants.MIXPANEL_TOKEN, true)
        analyticsManager.setMixpanelInstance(mixpanelAPI)

        analyticsManager.configurePlatforms(
            listOf(
                AnalyticsPlatforms.MIXPANEL,
                AnalyticsPlatforms.FIREBASE
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            val networkHandler = NetworkHandler.getInstance()
            networkHandler.initialize(
                deviceId = "",
                systemId = Settings.Secure.getString(
                    this@MainApp.contentResolver,
                    Settings.Secure.ANDROID_ID
                ),
                appVersion = BaseConstants.APP_VERSION
            )
            networkHandler.enableDebugMode(BaseConstants.DEBUG)
        }

        initialiseFacebookSdk()
        initialiseInMobiAds()
        setInstallSourceOnce()
    }

    /** Set install_source and raw referrer URL once from Google Play Install Referrer, then send at login. */
    private fun setInstallSourceOnce() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existing = dataStoreManager.read(DataStoreManager.INSTALL_SOURCE, "").first()
                if (existing.isNotEmpty()) return@launch
                val result = GooglePlayInstallReferrerReader.getInstallReferrer(this@MainApp)
                dataStoreManager.write(DataStoreManager.INSTALL_SOURCE, result.source)
                result.referrerUrl?.let { dataStoreManager.write(DataStoreManager.INSTALL_REFERRER_RAW, it) }
            } catch (e: Exception) {
                Logger.e("setInstallSourceOnce failed: ${e.message}")
            }
        }
    }

    private fun initialiseFacebookSdk() {
        try {
            // Initialize Facebook SDK
            FacebookSdk.sdkInitialize(this)
            FacebookSdk.setAutoInitEnabled(true)
            FacebookSdk.fullyInitialize()
            
            // Enable automatic event logging for install tracking
            AppEventsLogger.activateApp(this)
            
            Logger.d("Facebook SDK initialized successfully")
        } catch (e: Exception) {
            Logger.e("Facebook SDK initialization failed: ${e.message}")
        }
    }

    private fun initialiseInMobiAds() {
        val consentObject = JSONObject()
        try {
            // Provide correct consent value to sdk which is obtained by User
            consentObject.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
            // Provide 0 if GDPR is not applicable and 1 if applicable
            consentObject.put("gdpr", "0")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        InMobiSdk.init(
            this, BaseConstants.IN_MOBI_ACCOUNT_ID, consentObject,
            object : SdkInitializationListener {
                override fun onInitializationComplete(error: java.lang.Error?) {
                    if (null != error) {
                        Logger.e("InMobi Init failed -" + error.message)
                    } else {
                        Logger.d("InMobi Init Successful")
                    }
                }
            })
    }
}