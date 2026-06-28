package co.dailybook.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import co.dailybook.boilerplate.analytics.AnalyticsPlatforms
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import co.dailybook.base.analytics.Analytics
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.base.navigator.FragmentNavigator
import co.dailybook.base.navigator.ModuleNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class BaseBottomsheetFragment<VB : ViewBinding> : BottomSheetDialogFragment() {

    var binding: VB? = null
    abstract val screenName: String
    val analytics: Analytics by inject()
    val dataStoreManager: DataStoreManager by inject()
    val fragmentNavigator: FragmentNavigator by inject()
    val moduleNavigator: ModuleNavigator by inject()

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
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        triggerImpressionEvent()
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
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
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
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
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
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
    }
}