package com.dailybook.di

import com.boilerplate.analytics.AnalyticsManager
import com.boilerplate.analytics.AnalyticsManagerImpl
import com.dailybook.AddressGenerator.generateAddressList
import com.dailybook.ads.FirebaseCustomAdProvider
import com.dailybook.base.ads.CustomAdManager
import com.dailybook.base.ads.CustomAdProvider
import com.dailybook.base.analytics.Analytics
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.base.languagemanager.LanguageManager
import com.dailybook.base.navigator.FragmentNavigator
import com.dailybook.base.navigator.ModuleNavigator
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