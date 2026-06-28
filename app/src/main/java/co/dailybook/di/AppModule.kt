package co.dailybook.di

import co.dailybook.boilerplate.analytics.AnalyticsManager
import co.dailybook.boilerplate.analytics.AnalyticsManagerImpl
import co.dailybook.AddressGenerator.generateAddressList
import co.dailybook.ads.FirebaseCustomAdProvider
import co.dailybook.base.ads.CustomAdManager
import co.dailybook.base.ads.CustomAdProvider
import co.dailybook.base.analytics.Analytics
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.base.languagemanager.LanguageManager
import co.dailybook.base.navigator.FragmentNavigator
import co.dailybook.base.navigator.ModuleNavigator
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

    //Navigators
    single { LanguageManager() }
    single { ModuleNavigator(generateAddressList()) }
    single { FragmentNavigator() }
    single { DataStoreManager(androidContext()) }
    single<AnalyticsManager> { AnalyticsManagerImpl.getInstance(get()) }
    single { Analytics(get(), get()) }
    
    // Custom Ad Provider
    single<CustomAdProvider> { FirebaseCustomAdProvider() }
    single { CustomAdManager(get()) }
}