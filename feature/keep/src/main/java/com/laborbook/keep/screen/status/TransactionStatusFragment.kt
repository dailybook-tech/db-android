package com.laborbook.keep.screen.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.boilerplate.uikit.views.hide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.laborbook.base.BaseFragment
import com.laborbook.base.analytics.ConstantEventAttributes
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.base.datastore.shouldShowGoogleAds
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import com.laborbook.keep.R
import com.laborbook.keep.databinding.FragmentTransactionStatusBinding
import com.laborbook.keep.screen.calendar.utils.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


private const val STAFF_NAME = "staff_name"
private const val TYPE = "type"
private const val AMOUNT = "amount"

class TransactionStatusFragment : BaseFragment<FragmentTransactionStatusBinding>() {

    override val screenName: String
        get() = ConstantEventNames.TRANSACTION_STATUS
    private var staffName: String? = ""
    private var type: String? = ""
    private var amount: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            staffName = it.getString(STAFF_NAME)
            type = it.getString(TYPE)
            amount = it.getString(AMOUNT)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentTransactionStatusBinding? {
        return FragmentTransactionStatusBinding.inflate(inflater, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerOnClickListeners()
        setUpViews()
        requestGoogleAds()
        
        // Observe Pro status changes to hide ads immediately when user upgrades
        observeProStatusChanges()
    }
    
    /**
     * Observe Pro status changes and hide banner ads when user becomes Pro
     */
    private fun observeProStatusChanges() {
        dataStoreManager.read(DataStoreManager.PRO_STATUS, false)
            .onEach { isPro ->
                if (isPro) {
                    // User became Pro - hide banner ad immediately
                    binding?.adView?.hide()
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun requestGoogleAds() {
        lifecycleScope.launch {
            // Only show ads if user is not Pro and ads are enabled
            if (dataStoreManager.shouldShowGoogleAds()) {
                MobileAds.initialize(requireContext())
                val adRequest: AdRequest = AdRequest.Builder().build()
                // Set the AdListener to track ad load callbacks
                binding?.adView?.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        triggerSystemEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdLoaded"),
                                Pair(ConstantEventAttributes.SOURCE, "Advance")
                            )
                        )
                    }

                    override fun onAdFailedToLoad(errorCode: LoadAdError) {
                        triggerSystemEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdFailedToLoad"),
                                Pair(ConstantEventAttributes.SOURCE, "Advance")
                            )
                        )
                        // Try to load custom ad as fallback
                        loadCustomAd()
                    }

                    override fun onAdOpened() {
                        recordClickEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD_OPEN,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdOpened"),
                                Pair(ConstantEventAttributes.SOURCE, "Advance")
                            )
                        )
                    }

                    override fun onAdClicked() {
                        recordClickEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD_CLICK,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdClicked"),
                                Pair(ConstantEventAttributes.SOURCE, "Advance")
                            )
                        )
                    }

                    override fun onAdClosed() {
                        recordClickEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD_CLOSE,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdClosed"),
                                Pair(ConstantEventAttributes.SOURCE, "Advance")
                            )
                        )
                    }
                }
                binding?.adView?.loadAd(adRequest)
            } else {
                // If Google ads are disabled, try custom ad
                loadCustomAd()
            }
        }
    }

    private fun loadCustomAd() {
        lifecycleScope.launch {
            try {
                val customAdManager: com.laborbook.base.ads.CustomAdManager by inject()
                val customAdData = customAdManager.getCustomAdDataSync()

                if (customAdData.isValid()) {
                    binding?.customAdView?.setAnalytics(analytics)
                    binding?.customAdView?.loadAd(customAdData)
                } else {
                    binding?.customAdView?.hide()
                }
            } catch (e: Exception) {
                binding?.customAdView?.hide()
            }
        }
    }

    private fun setUpViews() {
        binding?.apply {
            if (type.equals(Constants.TYPE_ADVANCE)) {
                if (amount == "0") {
                    tvStaffName.text = getString(R.string.advance_amount).plus(" ")
                        .plus(getString(R.string.removed)).plus("\n")
                        .plus(getString(R.string.on).plus(" ").plus(staffName))
                } else {
                    tvStaffName.text =
                        getString(R.string.advance_amount).plus(" ").plus(getString(R.string.rupee))
                            .plus(" ").plus(amount).plus(" ").plus(getString(R.string.added))
                            .plus("\n").plus(getString(R.string.on).plus(" ").plus(staffName))
                }
            } else if (type.equals(Constants.TYPE_ATTENDANCE)) {
                tvStaffName.text = getString(R.string.successfully_marked_attendance).plus("\n")
                    .plus(getString(R.string.on).plus(" ").plus(staffName))
            }
        }
    }

    private fun registerOnClickListeners() {
        binding?.apply {
            btnOk.setOnClickListener {
                fragmentNavigator.goBack()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(staffName: String, type: String, amount: String? = "") =
            TransactionStatusFragment().apply {
                arguments = Bundle().apply {
                    putString(STAFF_NAME, staffName)
                    putString(TYPE, type)
                    putString(AMOUNT, amount)
                }
            }
    }
}