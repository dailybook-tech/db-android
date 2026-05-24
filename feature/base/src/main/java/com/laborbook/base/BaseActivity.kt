package com.laborbook.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.boilerplate.analytics.AnalyticsPlatforms
import com.laborbook.base.analytics.Analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

open class BaseActivity : AppCompatActivity() {

    val analytics: Analytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun recordClickEvent(eventName: String, hashMap: HashMap<String, Any>? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            analytics.logEvent(
                eventName,
                Analytics.CLICK,
                listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                hashMap
            )
        }
    }

    fun triggerSystemEvent(eventName: String, hashMap: HashMap<String, Any>? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            analytics.logEvent(
                eventName,
                Analytics.SYSTEM,
                listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                hashMap
            )
        }
    }
}