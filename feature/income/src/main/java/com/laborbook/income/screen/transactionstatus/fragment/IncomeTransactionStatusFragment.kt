package com.laborbook.income.screen.transactionstatus.fragment

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
import com.laborbook.income.R
import com.laborbook.income.databinding.FragmentIncomeTransactionStatusBinding
import com.laborbook.income.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TYPE = "type"
private const val AMOUNT = "amount"
private const val IS_UPDATE = "is_update"

class IncomeTransactionStatusFragment : BaseFragment<FragmentIncomeTransactionStatusBinding>() {

    override val screenName: String
        get() = ConstantEventNames.INCOME_TRANSACTION_STATUS
    private var type: String? = ""
    private var amount: String? = ""
    private var isUpdate: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = it.getString(TYPE)
            amount = it.getString(AMOUNT)
            isUpdate = it.getBoolean(IS_UPDATE)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentIncomeTransactionStatusBinding? {
        return FragmentIncomeTransactionStatusBinding.inflate(inflater,container,false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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
                                Pair(ConstantEventAttributes.SOURCE, "Expense")
                            )
                        )
                    }

                    override fun onAdFailedToLoad(errorCode: LoadAdError) {
                        triggerSystemEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdFailedToLoad"),
                                Pair(ConstantEventAttributes.SOURCE, "Income")
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
                                Pair(ConstantEventAttributes.SOURCE, "Expense")
                            )
                        )
                    }

                    override fun onAdClicked() {
                        recordClickEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD_CLICK,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdClicked"),
                                Pair(ConstantEventAttributes.SOURCE, "Expense")
                            )
                        )
                    }

                    override fun onAdClosed() {
                        recordClickEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD_CLOSE,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdClosed"),
                                Pair(ConstantEventAttributes.SOURCE, "Expense")
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
            if(isUpdate == true) {
                tvAmountAdded.text = getString(R.string.updated_successfully, amount, if (type == Constants.CREDIT) getString(R.string.cash_in) else getString(R.string.cash_out))
            } else {
                tvAmountAdded.text = getString(R.string.added_successfully, amount, if (type == Constants.CREDIT) getString(R.string.cash_in) else getString(R.string.cash_out))
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
        fun newInstance(type: String, amount: String? = "", isUpdate: Boolean) = IncomeTransactionStatusFragment().apply {
            arguments = Bundle().apply {
                putString(TYPE , type)
                putString(AMOUNT , amount)
                putBoolean(IS_UPDATE , isUpdate)
            }
        }
    }
}