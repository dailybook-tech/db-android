package co.dailybook

import android.app.Application
import android.provider.Settings
import co.dailybook.boilerplate.analytics.AnalyticsManager
import co.dailybook.boilerplate.analytics.AnalyticsPlatforms
import co.dailybook.boilerplate.network.NetworkHandler
import co.dailybook.boilerplate.uikit.ThemeUtils
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import co.dailybook.GooglePlayInstallReferrerReader
import co.dailybook.auth.di.authModule
import co.dailybook.base.BaseConstants
import co.dailybook.base.Logger
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.di.appModule
import co.dailybook.expense.di.expenseModule
import co.dailybook.income.di.incomeModule
import co.dailybook.keep.di.keepModule
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
            FacebookSdk.sdkInitialize(this)
            FacebookSdk.setAutoInitEnabled(true)
            FacebookSdk.fullyInitialize()
            AppEventsLogger.activateApp(this)
            Logger.d("Facebook SDK initialized successfully")
        } catch (e: Exception) {
            Logger.e("Facebook SDK initialization failed: ${e.message}")
        }
    }

}
