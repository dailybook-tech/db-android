package com.dailybook.keep.screen

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.boilerplate.network.NetworkHandler
import com.boilerplate.navigator.MultipleStackNavigator
import com.boilerplate.navigator.Navigator
import com.boilerplate.navigator.NavigatorConfiguration
import com.boilerplate.navigator.transaction.NavigatorTransaction
import com.boilerplate.navigator.transitionanimation.TransitionAnimationType
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.navigation.NavigationBarView
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.dailybook.base.BaseActivity
import com.dailybook.base.Headers
import com.dailybook.base.analytics.ConstantEventAttributes
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.base.analytics.ConstantEventSources
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.base.datastore.shouldShowHomePageAds
import com.dailybook.base.datastore.shouldShowGoogleAds
import com.dailybook.base.AdUnitConstants
import com.dailybook.base.analytics.FacebookPaymentEvents
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.dailybook.base.navigator.FragmentNavigator
import com.dailybook.expense.util.ExpenseObserverUtil
import com.dailybook.income.util.IncomeObserverUtil
import com.dailybook.keep.R
import com.dailybook.keep.databinding.ActivityBookKeepBinding
import com.dailybook.keep.screen.calendar.fragment.LaborMonthlyCalendarFragment
import com.dailybook.keep.screen.calendar.utils.ObserverUtil
import com.dailybook.keep.screen.home.fragment.CashbookFragment
import com.dailybook.keep.screen.home.fragment.ReferFriendBottomSheetFragment
import com.dailybook.keep.screen.home.fragment.SettingsFragment
import com.dailybook.keep.screen.home.fragment.StaffListFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.dailybook.keep.screen.premium.PremiumOfferManager
import com.dailybook.keep.screen.premium.PremiumOfferDialogFragment
import com.dailybook.keep.screen.premium.viewmodel.SubscriptionViewModel
import com.dailybook.keep.utils.SubscriptionsFeatureFlag
// RAZORPAY DISABLED: SDK imports commented out
// import com.razorpay.PaymentData
// import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

// RAZORPAY DISABLED: Was PaymentResultWithDataListener — re-add when re-enabling payments
// class BookKeepActivity : BaseActivity(), Navigator.NavigatorListener, PaymentResultWithDataListener {
class BookKeepActivity : BaseActivity(), Navigator.NavigatorListener {

    private lateinit var binding: ActivityBookKeepBinding
    private val fragmentNavigator: FragmentNavigator by inject()
    private val observerUtil: ObserverUtil by inject()
    private val expenseObserverUtil: ExpenseObserverUtil by inject()
    private val incomeObserverUtil: IncomeObserverUtil by inject()
    val dataStoreManager: DataStoreManager by inject()
    private val premiumOfferManager: PremiumOfferManager by inject()
    private val subscriptionViewModel: SubscriptionViewModel by viewModel()
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var appUpdateLauncher: ActivityResultLauncher<Intent>
    private lateinit var appLockLauncher: ActivityResultLauncher<Intent>

    // Interstitial policy: show on exit from Calendar, max N/day (UTC) from remote config
    private var lastDestinationClassName: String? = null
    private var calendarExitInterstitial: InterstitialAd? = null
    private var calendarExitInterstitialLoading: Boolean = false
    private var calendarExitInterstitialShownThisSession: Boolean = false
    private var referFriendBottomSheetCheckedThisSession: Boolean = false
    private var subscriptionStatusCheckedThisSession: Boolean = false

    private val rootFragmentProvider: ArrayList<() -> Fragment> = arrayListOf(
        { StaffListFragment.newInstance() },
        { CashbookFragment.newInstance() },
        { SettingsFragment.newInstance() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookKeepBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setupNavigator(savedInstanceState)
        setOnItemSelectedListener()
        setOnDestinationChangeListener()
        checkForNotificationPermission()
        ensureNetworkHeaders()
        recordFirstTimeHomeScreenEventIfNeeded()

        try {
            appUpdateManager = AppUpdateManagerFactory.create(this)
            appUpdateLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                }
            appLockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK) {
                    // Authentication failed or was canceled, check again
                    lifecycleScope.launch {
                        checkAppLockIfNeeded()
                    }
                }
            }
            checkForUpdate()
        }catch (e: Exception){}
        
        // Observe Pro status changes to hide ads immediately when user upgrades
        observeProStatusChanges()
        
        // Also observe subscription state changes to hide ads when subscription becomes active
        observeSubscriptionStateChanges()
    }
    
    /**
     * Observe Pro status changes and hide ads when user becomes Pro
     */
    private fun observeProStatusChanges() {
        dataStoreManager.read(DataStoreManager.PRO_STATUS, false)
            .onEach { isPro ->
                // Only hide ads if subscriptions feature is enabled
                // If subscriptions are disabled, show ads to everyone (old version behavior)
                val remoteConfig = Firebase.remoteConfig
                if (isPro && SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
                    // User became Pro and subscriptions are enabled - hide all ads immediately
                    hideAllAds()
                }
            }
            .launchIn(lifecycleScope)
    }
    
    /**
     * Observe subscription state changes to hide ads when subscription becomes PRO
     * Note: We rely on DataStore for ad visibility, but this observer provides immediate
     * feedback when subscription status is updated from API
     */
    private fun observeSubscriptionStateChanges() {
        subscriptionViewModel.subscriptionState.observe(this) { state ->
            when (state) {
                is SubscriptionViewModel.SubscriptionState.UserSubscriptionLoaded -> {
                    // API response received - updateSubscriptionStatus() will update DataStore
                    // The DataStore observer (observeProStatusChanges) will handle hiding ads
                    // This is just for immediate feedback - we trust DataStore as source of truth
                    lifecycleScope.launch {
                        // Give a moment for DataStore to update, then check
                        delay(100)
                        // Only hide ads if subscriptions feature is enabled
                        val remoteConfig = Firebase.remoteConfig
                        if (SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
                            val isPro = dataStoreManager.read(DataStoreManager.PRO_STATUS, false).first()
                            if (isPro) {
                                hideAllAds()
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Fires Meta (Facebook) event when user lands on home screen for the first time.
     * Only runs once per user; subsequent visits do not log the event.
     */
    private fun recordFirstTimeHomeScreenEventIfNeeded() {
        lifecycleScope.launch {
            val hasSeenHome = dataStoreManager.read(DataStoreManager.HAS_SEEN_HOME_SCREEN, false).first()
            if (!hasSeenHome) {
                val installSource = dataStoreManager.read(DataStoreManager.INSTALL_SOURCE, "organic").first()
                FacebookPaymentEvents.logFirstTimeHomeScreen(this@BookKeepActivity, installSource)
                dataStoreManager.write(DataStoreManager.HAS_SEEN_HOME_SCREEN, true)
            }
        }
    }

    /**
     * Ensures API headers (x-lb-companyID, Authorization, User-ID) are set from DataStore.
     * Required when app is restored after process death without going through RoutingActivity.
     */
    private fun ensureNetworkHeaders() {
        lifecycleScope.launch {
            val headers = hashMapOf(
                Headers.COMPANY_ID to dataStoreManager.read(DataStoreManager.COMPANY_ID, "").first(),
                Headers.AUTHORIZATION to Headers.BEARER.plus(" ").plus(dataStoreManager.read(DataStoreManager.ACCESS_TOKEN, "").first()),
                Headers.USER_ID to dataStoreManager.read(DataStoreManager.USER_ID, "").first(),
                Headers.GENERIC_USER_ID to dataStoreManager.read(DataStoreManager.USER_ID, "").first()
            )
            NetworkHandler.getInstance().setAdditionalHeaders(headers)
        }
    }
    
    /**
     * Hide all ads (banner and custom ads) when user becomes Pro
     */
    private fun hideAllAds() {
        binding?.apply {
            // Hide Google banner ad
            adView.hide()
            adsShadow?.hide()
            
            // Hide custom ad
            customAdView?.hide()
        }
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val root = binding.root
        val nav = binding.bottomNav
        val adContainer = binding.adContainer   // the FrameLayout wrapper
        val ad = binding.adView
        val customAd = binding.customAdView

        // status bar only for root
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = status.top)
            insets
        }

        // bottom inset handler
        val applyBottomInset: (WindowInsetsCompat) -> Unit = { insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomInset = navBars.bottom

            val hasAd = (ad.visibility == View.VISIBLE) || (customAd.visibility == View.VISIBLE)

            if (hasAd) {
                // Give the ad container a bottom margin so the AdView keeps its measured size.
                binding?.adContainer?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottomInset
                }
                // Keep bottom nav sitting above ad (no extra padding)
                nav.updatePadding(bottom = 0)
            } else {
                // No ad: remove adContainer margin and pad the bottom nav itself
                adContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = 0
                }
                nav.updatePadding(bottom = bottomInset)
            }
        }

        // Apply when nav insets change
        ViewCompat.setOnApplyWindowInsetsListener(nav) { _, insets ->
            applyBottomInset(insets)
            insets
        }

        // Utility to reapply current insets (call after changing ad visibility)
        val reapplyInsets = let@{
            val rootInsets = ViewCompat.getRootWindowInsets(root) ?: return@let
            applyBottomInset(rootInsets)
        }

        // Hook these into your ad callbacks
        // onAdLoaded -> ad.visibility = VISIBLE; reapplyInsets()
        // onAdFailedToLoad -> ad.visibility = GONE; reapplyInsets()
        // similarly for customAdView

        WindowInsetsControllerCompat(window, root).isAppearanceLightStatusBars = true
    }

    private fun checkForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API level 33
            when (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted
                }

                PackageManager.PERMISSION_DENIED -> {
                    // Permission is denied, request the permission
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun setupNavigator(savedInstanceState: Bundle?) {
        fragmentNavigator.initialize(
            MultipleStackNavigator(
                supportFragmentManager,
                R.id.container,
                rootFragmentProvider,
                navigatorListener = this,
                navigatorConfiguration = NavigatorConfiguration(
                    0,
                    true,
                    NavigatorTransaction.SHOW_HIDE
                ),
                context = this,
                transitionAnimationType = TransitionAnimationType.RIGHT_TO_LEFT
            ), savedInstanceState
        )
    }

    override fun onBackPressed() {
        if (fragmentNavigator.canGoBack() == true) {
            fragmentNavigator.goBack()
        } else {
            showExitConfirmationDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_app))
            .setMessage(getString(R.string.are_you_sure_you_want_to_exit))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                finishAffinity()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onTabChanged(tabIndex: Int) {
        when (tabIndex) {
            0 -> {
                binding?.bottomNav?.selectedItemId = R.id.navigation_staff
            }
            1 -> {
                binding?.bottomNav?.selectedItemId = R.id.navigation_cashbook
            }
            2 -> {
                binding?.bottomNav?.selectedItemId = R.id.navigation_settings
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        fragmentNavigator?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun setOnItemSelectedListener() {
        val mOnNavigationItemSelectedListener = NavigationBarView.OnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_staff -> {
                    fragmentNavigator?.switchTab(0)
                    return@OnItemSelectedListener true
                }

                R.id.navigation_cashbook -> {
                    fragmentNavigator?.switchTab(1)
                    return@OnItemSelectedListener true
                }

                R.id.navigation_settings -> {
                    fragmentNavigator?.switchTab(2)
                    return@OnItemSelectedListener true
                }
            }
            false
        }
        binding?.bottomNav?.setOnItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    private fun setOnDestinationChangeListener() {
        fragmentNavigator?.multipleStackNavigator?.observeDestinationChangesWithPopBack(this) { it, isFromPopBack ->
            val previousDestination = lastDestinationClassName
            lastDestinationClassName = it::class.java.name

            // Preload when entering Calendar so it's ready when user returns back.
            if (it is LaborMonthlyCalendarFragment) {
                preloadCalendarExitInterstitialIfEligible()
            }

            // Show when coming back from Calendar (max N/day from remote config).
            if (isFromPopBack && previousDestination == LaborMonthlyCalendarFragment::class.java.name) {
                lifecycleScope.launch {
                    if (canShowDailyCalendarExitInterstitial()) {
                        showCalendarExitInterstitialIfReady()
                    }
                }
            }

            when (it) {
                is StaffListFragment -> {
                    binding?.bottomNav?.show()
                    observerUtil.clearSearchText?.invoke(true)
                    if(isFromPopBack){
                        observerUtil.refreshStaffs?.invoke(true)
                    }
                }

                is CashbookFragment -> {
                    binding?.bottomNav?.show()
                    expenseObserverUtil.clearExpenseSearchText?.invoke(true)
                    incomeObserverUtil.clearIncomeSearchText?.invoke(true)
                }

                is SettingsFragment -> {
                    binding?.bottomNav?.show()
                }

                is LaborMonthlyCalendarFragment -> {
                    binding?.bottomNav?.hide()
                    if(isFromPopBack) {
                        observerUtil.refreshCalendar?.invoke(true, false, "",0)
                    }
                }

                else -> {
                    binding?.bottomNav?.hide()
                }
            }
            if (isFromPopBack) { }
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            recordClickEvent(ConstantEventNames.NOTIFICATION_PERMISSION_GRANTED, hashMapOf(Pair(ConstantEventAttributes.STATUS, ConstantEventSources.YES)))
        } else {
            recordClickEvent(ConstantEventNames.NOTIFICATION_PERMISSION_GRANTED, hashMapOf(Pair(ConstantEventAttributes.STATUS, ConstantEventSources.NO)))
        }
    }

    private fun checkForUpdate() {
        triggerSystemEvent(ConstantEventNames.CHECK_FOR_UPDATE)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Step 4: Check the type of update and request the update
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    triggerSystemEvent(ConstantEventNames.UPDATE_AVAILABLE, hashMapOf(Pair(ConstantEventAttributes.UPDATE_TYPE, "IMMEDIATE")))
                    startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    triggerSystemEvent(ConstantEventNames.UPDATE_AVAILABLE, hashMapOf(Pair(ConstantEventAttributes.UPDATE_TYPE, "FLEXIBLE")))
                    startUpdate(appUpdateInfo, AppUpdateType.FLEXIBLE)
                }
            }
        }
    }

    private fun startUpdate(appUpdateInfo: AppUpdateInfo, updateType: Int) {
        try {
            val options = AppUpdateOptions.newBuilder(updateType).build()

            // Use startUpdateFlow with AppUpdateOptions
            appUpdateManager.startUpdateFlow(appUpdateInfo, this, options)
            if(updateType == AppUpdateType.IMMEDIATE){
                triggerSystemEvent(ConstantEventNames.START_UPDATE, hashMapOf(Pair(ConstantEventAttributes.UPDATE_TYPE, "IMMEDIATE")))
            } else {
                triggerSystemEvent(ConstantEventNames.START_UPDATE, hashMapOf(Pair(ConstantEventAttributes.UPDATE_TYPE, "FLEXIBLE")))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting update flow: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        ensureNetworkHeaders()
        lifecycleScope.launch {
            checkAppLockIfNeeded()
            
            // IMPORTANT: Refresh subscription status BEFORE requesting ads
            // This ensures Pro status is up-to-date before ads are loaded
            if (!subscriptionStatusCheckedThisSession) {
                refreshSubscriptionStatus()
            }
            
            // Now request ads (after status is checked/updated)
            requestGoogleAds()
        }
        
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                }
            }
        
        // Check and show refer friend bottom sheet if needed (only once per session)
        if (!referFriendBottomSheetCheckedThisSession) {
            lifecycleScope.launch {
                checkAndShowReferFriendBottomSheet()
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    private suspend fun checkAppLockIfNeeded(): Boolean {
        val isAppLockEnabled = dataStoreManager.read(DataStoreManager.APP_LOCK_ENABLED, false).first()
        if (!isAppLockEnabled) return false

        val lastAuthTimeStr = dataStoreManager.read(DataStoreManager.LAST_AUTH_TIME, "0").first()
        val lastAuthTime = lastAuthTimeStr.toLongOrNull() ?: 0L
        val currentTime = System.currentTimeMillis()
        val timeSinceAuth = currentTime - lastAuthTime

        // Require authentication if more than 30 seconds have passed since last auth
        // or if this is the first time (lastAuthTime is 0)
        if (lastAuthTime == 0L || timeSinceAuth > 30000) {
            val intent = Intent(this@BookKeepActivity, AppLockActivity::class.java)
            appLockLauncher.launch(intent)
            return true
        }
        return false
    }

    private fun todayEpochDayUtc(): Int {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()).toInt()
    }

    private suspend fun canShowDailyCalendarExitInterstitial(): Boolean {
        if (calendarExitInterstitialShownThisSession) return false
        // Only show interstitial ads if user is not Pro and ads are enabled
        if (!dataStoreManager.shouldShowGoogleAds()) return false

        val maxPerDay = Firebase.remoteConfig.getLong("interstitial_ads_per_day").toInt().coerceIn(0, 100)
        if (maxPerDay == 0) return false

        val today = todayEpochDayUtc()
        val lastShownDay = dataStoreManager.read(DataStoreManager.LAST_INTERSTITIAL_EPOCH_DAY, -1).first()
        val countStored = dataStoreManager.read(DataStoreManager.INTERSTITIAL_COUNT_TODAY, 0).first()
        val countToday = if (lastShownDay != today) 0 else countStored
        return countToday < maxPerDay
    }

    private fun preloadCalendarExitInterstitialIfEligible() {
        if (calendarExitInterstitial != null || calendarExitInterstitialLoading) return

        lifecycleScope.launch {
            if (!canShowDailyCalendarExitInterstitial()) return@launch

            calendarExitInterstitialLoading = true
            val request = AdRequest.Builder().build()

            InterstitialAd.load(
                this@BookKeepActivity,
                AdUnitConstants.InterstitialAds.APP_OPEN,
                request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        calendarExitInterstitialLoading = false
                        calendarExitInterstitial = null
                    }

                    override fun onAdLoaded(ad: InterstitialAd) {
                        calendarExitInterstitialLoading = false
                        calendarExitInterstitial = ad
                    }
                }
            )
        }
    }

    private fun showCalendarExitInterstitialIfReady() {
        val ad = calendarExitInterstitial ?: return
        if (isFinishing || isDestroyed) {
            calendarExitInterstitial = null
            return
        }

        // Prevent double-shows on fast repeated callbacks.
        calendarExitInterstitial = null

        val today = todayEpochDayUtc()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                calendarExitInterstitialShownThisSession = true
                lifecycleScope.launch {
                    dataStoreManager.recordInterstitialShown(today)
                }
            }

            override fun onAdDismissedFullScreenContent() {
                // No-op: navigation already happened.
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // No-op
            }
        }

        ad.show(this)
    }

    /**
     * Called when paywall is shown (free user). Preload interstitial so it's ready on exit.
     */
    fun onPaywallShown() {
        preloadCalendarExitInterstitialIfEligible()
    }

    /**
     * Called when paywall is dismissed (free user). Show interstitial up to N/day (remote config) if ready.
     */
    fun onPaywallDismissed() {
        lifecycleScope.launch {
            if (canShowDailyCalendarExitInterstitial()) {
                showCalendarExitInterstitialIfReady()
            }
        }
    }

    private fun requestGoogleAds() {
        lifecycleScope.launch {
            // Double-check Pro status right before requesting ads
            // This ensures we have the latest status even if it was just updated
            val isPro = dataStoreManager.read(DataStoreManager.PRO_STATUS, false).first()
            val homePageAdsEnabled = dataStoreManager.read(DataStoreManager.HOME_PAGE_ADS_ENABLED, true).first()
            val shouldShowAds = !isPro && homePageAdsEnabled
            
            // Only show ads if user is not Pro and home page ads are enabled
            if (shouldShowAds) {
                MobileAds.initialize(this@BookKeepActivity)
                val adRequest: AdRequest = AdRequest.Builder().build()
                // Set the AdListener to track ad load callbacks
                binding?.adView?.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        // Double-check Pro status before showing ad (in case status changed after ad was requested)
                        lifecycleScope.launch {
                            val shouldShow = dataStoreManager.shouldShowHomePageAds()
                            if (shouldShow) {
                                binding?.adsShadow?.show()
                                binding?.adView?.show()
                                triggerSystemEvent(
                                    ConstantEventNames.GOOGLE_BANNER_AD,
                                    hashMapOf(
                                        Pair(ConstantEventAttributes.STATUS, "onAdLoaded"),
                                        Pair(ConstantEventAttributes.SOURCE, "Home")
                                    )
                                )
                            } else {
                                // User is Pro - hide the ad
                                binding?.adsShadow?.hide()
                                binding?.adView?.hide()
                            }
                        }
                    }

                    override fun onAdFailedToLoad(errorCode: LoadAdError) {
                        binding?.adsShadow?.hide()
                        binding?.adView?.hide()
                        triggerSystemEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdFailedToLoad"),
                                Pair(ConstantEventAttributes.SOURCE, "Home")
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
                                Pair(ConstantEventAttributes.SOURCE, "Home")
                            )
                        )
                    }

                    override fun onAdClicked() {
                        recordClickEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD_CLICK,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdClicked"),
                                Pair(ConstantEventAttributes.SOURCE, "Home")
                            )
                        )
                    }

                    override fun onAdClosed() {
                        recordClickEvent(
                            ConstantEventNames.GOOGLE_BANNER_AD_CLOSE,
                            hashMapOf(
                                Pair(ConstantEventAttributes.STATUS, "onAdClosed"),
                                Pair(ConstantEventAttributes.SOURCE, "Home")
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
                // Double-check Pro status before loading custom ad
                val shouldShowAds = dataStoreManager.shouldShowHomePageAds()
                if (!shouldShowAds) {
                    // User is Pro - don't show custom ad
                    binding?.customAdView?.hide()
                    binding?.adsShadow?.hide()
                    return@launch
                }
                
                val customAdManager: com.dailybook.base.ads.CustomAdManager by inject()
                val customAdData = customAdManager.getCustomAdDataSync()

                if (customAdData.isValid()) {
                    binding?.customAdView?.setAnalytics(analytics)
                    binding?.customAdView?.loadAd(customAdData)
                    binding?.adsShadow?.show()
                } else {
                    binding?.customAdView?.hide()
                    binding?.adsShadow?.hide()
                }
            } catch (e: Exception) {
                binding?.customAdView?.hide()
                binding?.adsShadow?.hide()
            }
        }
    }

    private suspend fun refreshSubscriptionStatus() {
        // Only check once per session
        if (subscriptionStatusCheckedThisSession) return
        subscriptionStatusCheckedThisSession = true

        // Skip subscription status refresh if subscriptions feature is disabled
        val remoteConfig = Firebase.remoteConfig
        if (!SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
            return
        }

        // Check if user is logged in
        val isLoggedIn = dataStoreManager.read(DataStoreManager.IS_LOGGED_IN, false).first()
        if (!isLoggedIn) return

        // Get user ID
        val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
        if (userId.isEmpty()) return

        // Refresh subscription status in background
        // Note: This is async, but we check status again in requestGoogleAds() before loading ads
        try {
            subscriptionViewModel.checkUserSubscriptionStatus(userId)
            // Give a small delay for the status to be written to DataStore
            delay(300)
        } catch (e: Exception) {
            // Silent failure - don't show any error to user
            // Local data will remain unchanged
        }
    }

    private suspend fun checkAndShowReferFriendBottomSheet() {
        // Only check once per session
        if (referFriendBottomSheetCheckedThisSession) return
        referFriendBottomSheetCheckedThisSession = true

        // Check if user is logged in
        val isLoggedIn = dataStoreManager.read(DataStoreManager.IS_LOGGED_IN, false).first()
        if (!isLoggedIn) return

        // Check if bottom sheet has been shown before
        val hasBeenShown = dataStoreManager.read(DataStoreManager.REFER_FRIEND_BOTTOM_SHEET_SHOWN, false).first()
        if (hasBeenShown) return

        // Add a small delay to ensure UI is fully ready (increased delay since Pro offer shows first)
        delay(1500)

        // Show the bottom sheet
        if (!isFinishing && !isDestroyed) {
            runOnUiThread {
                try {
                    ReferFriendBottomSheetFragment.newInstance()
                        .show(supportFragmentManager, ReferFriendBottomSheetFragment.TAG)
                    
                    // Mark as shown
                    lifecycleScope.launch {
                        dataStoreManager.write(DataStoreManager.REFER_FRIEND_BOTTOM_SHEET_SHOWN, true)
                    }
                } catch (e: Exception) {
                    // Handle any exceptions (e.g., if fragment manager is not ready)
                    referFriendBottomSheetCheckedThisSession = false
                }
            }
        } else {
            referFriendBottomSheetCheckedThisSession = false
        }
    }

    // RAZORPAY DISABLED: Payment callbacks commented out — re-enable when restoring payments
    // override fun onPaymentSuccess(razorpayPaymentId: String, paymentData: PaymentData?) {
    //     val fragment = supportFragmentManager.findFragmentByTag(PremiumOfferDialogFragment.TAG)
    //     if (fragment is PremiumOfferDialogFragment) {
    //         fragment.onPaymentSuccess(razorpayPaymentId, paymentData)
    //     }
    // }
    //
    // override fun onPaymentError(errorCode: Int, errorMessage: String, paymentData: PaymentData?) {
    //     val fragment = supportFragmentManager.findFragmentByTag(PremiumOfferDialogFragment.TAG)
    //     if (fragment is PremiumOfferDialogFragment) {
    //         fragment.onPaymentError(errorCode, errorMessage, paymentData)
    //     }
    // }
    //
    // @Deprecated("Deprecated in Java")
    // override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    //     super.onActivityResult(requestCode, resultCode, data)
    //     val fragment = supportFragmentManager.findFragmentByTag(PremiumOfferDialogFragment.TAG)
    //     if (fragment is PremiumOfferDialogFragment) {
    //         fragment.onActivityResultForRazorpay(requestCode, resultCode, data)
    //     }
    // }
}