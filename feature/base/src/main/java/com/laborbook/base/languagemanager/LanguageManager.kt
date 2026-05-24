package com.laborbook.base.languagemanager

import android.content.Context
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.base.datastore.DataStoreManager.Companion.LANGUAGE_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class LanguageManager : KoinComponent {

    // Injecting DataStoreManager
    private val dataStoreManager: DataStoreManager by inject()

    // Function to set the language and store it using DataStoreManager
    suspend fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // Save the language preference using DataStoreManager's write method
        dataStoreManager.write(LANGUAGE_KEY, languageCode)
    }

    // Function to get the stored language using DataStoreManager's read method
    fun getSavedLanguage(): Flow<String> {
        return dataStoreManager.read(LANGUAGE_KEY, Locale.getDefault().language)
    }

    // Function to apply the saved language on startup
    suspend fun loadLocale(context: Context) {
        val languageCode = getSavedLanguage().first() // Read the language code once
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}