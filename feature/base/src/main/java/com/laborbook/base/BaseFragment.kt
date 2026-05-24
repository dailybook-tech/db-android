package com.laborbook.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.boilerplate.analytics.AnalyticsPlatforms
import com.laborbook.base.analytics.Analytics
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.base.navigator.FragmentNavigator
import com.laborbook.base.navigator.ModuleNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    var binding: VB? = null
    var isRefresh: Boolean = false
    var isFirstTime: Boolean = false
    val analytics: Analytics by inject()
    val dataStoreManager: DataStoreManager by inject()
    val fragmentNavigator: FragmentNavigator by inject()
    val moduleNavigator: ModuleNavigator by inject()
    abstract val screenName: String

    abstract fun getViewBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): VB?

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        binding = getViewBinding(inflater, container, savedInstanceState)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        triggerImpressionEvent()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun triggerImpressionEvent(hashMap: HashMap<String, Any>? = null) {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    analytics.logEvent(
                        screenName,
                        Analytics.IMPRESSION,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMap
                    )
                }catch (e: Exception) {}
            }
        }catch (e: Exception) {}
    }

    fun triggerImpressionEvent(eventName: String, hashMap: HashMap<String, Any>? = null) {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    analytics.logEvent(
                        eventName,
                        Analytics.IMPRESSION,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMap
                    )
                }catch (e: Exception) {}
            }
        }catch (e: Exception) {}
    }

    fun recordClickEvent(eventName: String, hashMap: HashMap<String, Any>? = null) {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    analytics.logEvent(
                        eventName,
                        Analytics.CLICK,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMap
                    )
                }catch (e: Exception){}
            }
        }catch (e: Exception){}
    }

    fun triggerSystemEvent(eventName: String, hashMap: HashMap<String, Any>? = null) {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    analytics.logEvent(
                        eventName,
                        Analytics.SYSTEM,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMap
                    )
                }catch (e: Exception){}
            }
        }catch (e: Exception){}
    }

    /**
     * Opens the URL in the in-app WebView activity (toolbar uses primary color, not white).
     * Use for Privacy Policy, Terms & Conditions, etc. instead of Custom Chrome Tabs.
     */
    fun openUrlInCustomTab(context: Context, url: String) {
        WebViewActivity.start(context, url)
    }

    /**
     * Opens the URL in the in-app WebView activity with a custom title.
     */
    fun openUrlInWebView(context: Context, url: String, title: String?) {
        WebViewActivity.start(context, url, title)
    }
}