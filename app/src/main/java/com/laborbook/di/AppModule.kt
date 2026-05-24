package com.laborbook.di

import com.boilerplate.analytics.AnalyticsManager
import com.boilerplate.analytics.AnalyticsManagerImpl
import com.laborbook.AddressGenerator.generateAddressList
import com.laborbook.ads.FirebaseCustomAdProvider
import com.laborbook.base.ads.CustomAdManager
import com.laborbook.base.ads.CustomAdProvider
import com.laborbook.base.analytics.Analytics
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.base.languagemanager.LanguageManager
import com.laborbook.base.navigator.FragmentNavigator
import com.laborbook.base.navigator.ModuleNavigator
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