package com.dailybook.keep.screen.premium

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.SystemClock
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.boilerplate.analytics.AnalyticsPlatforms
import com.dailybook.base.analytics.Analytics
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.base.analytics.FacebookPaymentEvents
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.keep.R
import com.dailybook.keep.databinding.DialogPremiumOfferBinding
import com.dailybook.keep.model.subscription.VerifySubscriptionRequest
import com.dailybook.keep.screen.BookKeepActivity
import com.dailybook.keep.screen.premium.viewmodel.SubscriptionViewModel
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.recyclerview.widget.RecyclerView
// RAZORPAY DISABLED: SDK imports commented out
// import com.razorpay.PaymentData
// import com.razorpay.PaymentResultWithDataListener
// import com.razorpay.PaymentMethodsCallback
// import com.razorpay.Razorpay
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.ktx.Firebase
import com.dailybook.keep.databinding.ItemSubscriptionPlanBinding

// RAZORPAY DISABLED: Was PaymentResultWithDataListener — re-add when re-enabling payments
// class PremiumOfferDialogFragment : DialogFragment(), PaymentResultWithDataListener {
class PremiumOfferDialogFragment : DialogFragment() {

    private var _binding: DialogPremiumOfferBinding? = null
    private val binding get() = _binding
    private val analytics: Analytics by inject()
    private val dataStoreManager: DataStoreManager by inject()
    private val viewModel: SubscriptionViewModel by viewModel()
    private val remoteConfig: FirebaseRemoteConfig by lazy { Firebase.remoteConfig }
    
    private var selectedUpiApp: InstalledUpiApp? = null
    private var selectedPlan: com.dailybook.keep.model.subscription.SubscriptionPlan? = null
    private var currentSubscriptionId: String? = null
    private var installSource: String = "organic"
    private var planAdapter: SubscriptionPlanAdapter? = null
    private var installedUpiApps: List<InstalledUpiApp> = emptyList()
    private var isVerificationInProgress = false
    /** True from SubscriptionCreated until we show success or payment-failed. Prevents Error state from showing failure before user has paid. */
    private var paymentFlowInProgress = false
    private var verificationAttempts = 0
    private val maxVerificationAttempts = 6 // Poll for ~18 seconds so backend has time to activate after UPI return
    private var isInitialPlansLoading = true // Track if we're loading plans initially or creating subscription
    private var isTrialExpired = false // Track if user's trial has expired
    private var reviewsAutoScrollJob: kotlinx.coroutines.Job? = null // Job for auto-scrolling reviews
    private var offerTimerRunnable: Runnable? = null // Decrement offer timer every second
    private var offerTimerEndAtElapsedMs: Long? = null // SystemClock.elapsedRealtime() when timer ends

    // RAZORPAY DISABLED: Razorpay SDK dependency removed — payments turned off.
    // private var razorpayCustom: Razorpay? = null
    private var razorpayCustom: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        
        // Get trial expired status from arguments
        isTrialExpired = arguments?.getBoolean(ARG_IS_TRIAL_EXPIRED, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogPremiumOfferBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        triggerImpressionEvent(ConstantEventNames.PREMIUM_OFFER_DIALOG)

        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            installSource = dataStoreManager.read(DataStoreManager.INSTALL_SOURCE, "organic").first()
        }
        FacebookPaymentEvents.logViewContent(requireContext())

        setupViews()
        viewLifecycleOwner.lifecycleScope.launch {
            setupOfferTimerFromRemoteConfig()
        }
        registerClickListeners()
        observeViewModel()
        
        // Show loading before fetching plans
        showLoading(true)
        loadSubscriptionPlans()
    }

    override fun onStart() {
        super.onStart()
        // Preload interstitial when free user enters paywall (max N/day from remote config, same as calendar exit)
        (activity as? BookKeepActivity)?.onPaywallShown()
    }
    
    override fun onResume() {
        super.onResume()
        // Restart auto-scroll if it was stopped
        if (reviewsAutoScrollJob?.isActive != true && binding?.rvReviews != null) {
            startReviewsAutoScroll()
        }
        // Restart offer timer updates if needed
        maybeStartOfferCountdown()
    }
    
    override fun onPause() {
        super.onPause()
        // Pause auto-scroll when fragment is not visible
        reviewsAutoScrollJob?.cancel()
        // Stop ticking offer timer while not visible
        stopOfferCountdown()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Show interstitial when free user exits paywall (max N/day from remote config, same as calendar exit)
        (activity as? BookKeepActivity)?.onPaywallDismissed()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            
            // Clear any fullscreen flags to show status bar
            clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            
            // Set status bar color to white/background
            statusBarColor = resources.getColor(com.dailybook.keep.R.color.background, null)
            
            // Set status bar icons to dark (for light background)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        
        // Handle back button press
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                handleBackPress()
                true // Consume the event
            } else {
                false
            }
        }
        
        return dialog
    }
    
    private fun handleBackPress() {
        if (isVerificationInProgress) {
            // Don't allow back press during verification
            android.widget.Toast.makeText(
                requireContext(),
                getString(R.string.please_wait_verifying_payment),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else if (binding?.paymentWebview?.visibility == View.VISIBLE) {
            // Payment WebView visible: user may have paid in UPI app; poll status then hide
            onPaymentWebViewDismissed()
        } else {
            // Allow normal dismissal
            dismiss()
        }
    }

    /**
     * WebViewClient that loads Razorpay payment_link and opens UPI/intent URLs in external apps
     * so the UPI app opens directly when user taps "Pay with PhonePe" etc.
     */
    private fun createPaymentWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return handlePaymentUrl(view, url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                return handlePaymentUrl(view, url)
            }

            private fun handlePaymentUrl(view: WebView?, url: String): Boolean {
                // Allow Razorpay (https) to load in WebView — do not treat any URL as success here.
                // For intent flow we rely only on Razorpay onPaymentSuccess/onPaymentError; the first
                // Razorpay page often contains "callback"/"razorpay" and was wrongly triggering verification.
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                // UPI / intent / other schemes: open in external app so UPI app opens directly
                try {
                    val intent = if (url.startsWith("intent://", ignoreCase = true)) {
                        Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    } else {
                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // No app for this scheme
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Do not detect success from URL here — intent flow uses onPaymentSuccess/onPaymentError only.
            }
        }
    }

    /** Called when payment WebView is closed (back) or when success URL is detected. */
    private fun onPaymentWebViewDismissed() {
        if (currentSubscriptionId != null) {
            showVerificationScreen(getString(R.string.checking_payment_status))
            startStatusPolling(currentSubscriptionId!!)
        }
        hidePaymentWebView()
    }

    /** Called when WebView loads a URL that indicates payment success/callback. */
    private fun onPaymentSuccessUrlLoaded() {
        if (currentSubscriptionId != null) {
            showVerificationScreen(getString(R.string.verifying_payment))
            startStatusPolling(currentSubscriptionId!!)
        }
        hidePaymentWebView()
    }

    private fun setupViews() {
        binding?.apply {
            // Payment WebView: we load payment_link ourselves and open UPI/intent URLs in external apps.
            // Do NOT call razorpay.setWebView() here — it overwrites our WebViewClient and the SDK then
            // loads its own page and never opens the UPI app.
            paymentWebview.visibility = View.GONE
            paymentWebview.settings.javaScriptEnabled = true
            paymentWebview.settings.domStorageEnabled = true
            paymentWebview.webViewClient = createPaymentWebViewClient()
            // RAZORPAY DISABLED: SDK init and payment method loading commented out
            // razorpayCustom = Razorpay(requireActivity(), com.dailybook.keep.BuildConfig.RAZORPAY_KEY_ID)
            // fetchPaymentMethods()
            // loadUpiAppsViaRazorpay()

            // Paywall CTA button: blue background per design
            btnStartTrial.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_primary))

            // Subscription explainer video: tap video to toggle pause/play.
            videoSubscriptionExplainer.setVideoURI(
                Uri.parse("android.resource://${requireContext().packageName}/${com.dailybook.base.R.raw.subscription_explainer_video}")
            )
            videoSubscriptionExplainer.setOnPreparedListener { mp ->
                mp.start()
            }
            videoSubscriptionExplainer.setOnCompletionListener {
                videoSubscriptionExplainer.start()
            }
            videoSubscriptionExplainer.setOnClickListener {
                if (videoSubscriptionExplainer.isPlaying) {
                    videoSubscriptionExplainer.pause()
                } else {
                    videoSubscriptionExplainer.start()
                }
            }

            // Setup reviews horizontal carousel (scrolls horizontally)
            rvReviews.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            rvReviews.adapter = PaywallReviewAdapter(getStaticReviews())
            rvReviews.isNestedScrollingEnabled = false
            
            // Pause auto-scroll when user touches the reviews (manual interaction)
            rvReviews.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                    when (e.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            // Pause auto-scroll when user touches
                            reviewsAutoScrollJob?.cancel()
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            // Resume auto-scroll after user stops touching (after 3 seconds)
                            rvReviews.postDelayed({
                                if (isAdded && binding != null) {
                                    startReviewsAutoScroll()
                                }
                            }, 3000)
                        }
                    }
                    return false // Don't consume the event, let RecyclerView handle scrolling
                }
            })
            
            // Start auto-scroll for reviews after a short delay to ensure layout is ready
            rvReviews.post {
                if (isAdded && binding != null) {
                    startReviewsAutoScroll()
                }
            }
            
            // UPI selector: show and open bottom sheet on click
            clUpiSelector.setOnClickListener { showUpiSelectionBottomSheet() }
            // Default UPI app is set when loadUpiAppsViaRazorpay() callback runs
            
            // Handle bottom insets for navigation bar
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(clBottomSection) { view, insets ->
                val navBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                view.setPadding(
                    view.paddingLeft,
                    view.paddingTop,
                    view.paddingRight,
                    view.paddingBottom + navBarInsets.bottom
                )
                insets
            }
        }
    }

    private fun registerClickListeners() {
        binding?.apply {
            // Close button
            ivClose.setOnClickListener {
                recordClickEvent(ConstantEventNames.PREMIUM_OFFER_CLOSE)
                dismiss()
            }

            // Start Trial button
            btnStartTrial.setOnClickListener {
                handleStartTrial()
            }
        }
    }

    private fun loadSubscriptionPlans() {
        lifecycleScope.launch {
            val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
            if (userId.isNotEmpty()) {
                viewModel.loadSubscriptionPlans(userId)
            } else {
                showErrorLoading(getString(R.string.user_id_not_found))
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.subscriptionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SubscriptionViewModel.SubscriptionState.Loading -> {
                    if (isInitialPlansLoading) {
                        // Initial plans loading - show progress bar
                        showLoading(true)
                    } else {
                        // Subscription creation - show spinner
                        showSpinnerLoading(true)
                    }
                }
                is SubscriptionViewModel.SubscriptionState.PlansLoaded -> {
                    isInitialPlansLoading = false // Plans loaded, future loading is subscription creation
                    showLoading(false)
                    handlePlansLoaded(state.plans)
                }
                is SubscriptionViewModel.SubscriptionState.SubscriptionCreated -> {
                    showSpinnerLoading(false)
                    paymentFlowInProgress = true
                    initiateRazorpayPayment(state.response)
                }
                is SubscriptionViewModel.SubscriptionState.SubscriptionVerified -> {
                    showSpinnerLoading(false)
                    if (state.isActive) {
                        // VERIFIED - Stop polling immediately
                        Timber.d("verification SUCCESS (SubscriptionVerified): isActive=true -> showSuccessDialog")
                        isVerificationInProgress = false
                        hideVerificationScreen()
                        lifecycleScope.launch(Dispatchers.IO) {
                            analytics.logEvent(ConstantEventNames.SUBSCRIPTION_ACTIVATED, Analytics.CLICK, listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE))
                        }
                        showSuccessDialog()
                    } else {
                        // NOT verified - Check if max attempts reached
                        if (verificationAttempts >= maxVerificationAttempts) {
                            Timber.e("verification FAILURE: SubscriptionVerified isActive=false maxAttempts=$verificationAttempts -> showPaymentFailedBottomSheet")
                            isVerificationInProgress = false
                            hideVerificationScreen()
                            showPaymentFailedBottomSheet(getString(R.string.payment_failed_try_again))
                        }
                        // Otherwise continue polling (don't stop)
                    }
                }
                is SubscriptionViewModel.SubscriptionState.UserSubscriptionLoaded -> {
                    showSpinnerLoading(false)
                    // Check if subscription is now active (case-insensitive check)
                    if (state.subscription.subscriptionTier.equals("PRO", ignoreCase = true)) {
                        // PRO - Stop polling immediately
                        Timber.d("verification SUCCESS (UserSubscriptionLoaded): tier=PRO -> showSuccessDialog")
                        isVerificationInProgress = false
                        hideVerificationScreen()
                        lifecycleScope.launch(Dispatchers.IO) {
                            analytics.logEvent(ConstantEventNames.SUBSCRIPTION_ACTIVATED, Analytics.CLICK, listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE))
                        }
                        showSuccessDialog()
                    } else {
                        // Still FREE - Check if max attempts reached
                        if (verificationAttempts >= maxVerificationAttempts) {
                            Timber.e("verification FAILURE: UserSubscriptionLoaded tier=${state.subscription.subscriptionTier} maxAttempts=$verificationAttempts -> showPaymentFailedBottomSheet")
                            isVerificationInProgress = false
                            hideVerificationScreen()
                            showPaymentFailedBottomSheet(getString(R.string.payment_failed_try_again))
                        }
                        // Otherwise continue polling (don't stop)
                    }
                }
                is SubscriptionViewModel.SubscriptionState.Error -> {
                    // Don't stop polling on error during verification - payment might still be processing
                    if (isVerificationInProgress) {
                        // Continue polling silently - DON'T show error or hide verification screen
                        // Error could be temporary (network issue, signature mismatch during processing)
                        // Only show error when max attempts reached
                        if (verificationAttempts >= maxVerificationAttempts) {
                            Timber.e("verification FAILURE: Error during verification maxAttempts=$verificationAttempts message=${state.message} -> showPaymentFailedBottomSheet")
                            isVerificationInProgress = false
                            hideVerificationScreen()
                            showPaymentFailedBottomSheet(getString(R.string.payment_failed_try_again))
                        }
                        // Otherwise: keep verification screen visible, continue polling
                    } else if (paymentFlowInProgress) {
                        // Payment flow started (Razorpay opened) but not yet in verification - e.g. user in UPI app.
                        // Error may be from unrelated API call (e.g. refreshSubscriptionStatus). Don't show failure yet.
                        Timber.d("subscription Error ignored during payment flow (waiting for Razorpay callback): ${state.message}")
                    } else {
                        // Error during initial load or subscription creation (not during verification)
                        Timber.e("subscription Error (not during verification): ${state.message} -> showPaymentFailedBottomSheet")
                        hideVerificationScreen()
                        showPaymentFailedBottomSheet(state.message)
                    }
                }
                else -> {
                    showLoading(false)
                    showSpinnerLoading(false)
                }
            }
        }
    }
    
    private fun handlePlansLoaded(plans: List<com.dailybook.keep.model.subscription.SubscriptionPlan>) {
        if (plans.isNotEmpty()) {
            // Filter active plans
            val activePlans = plans.filter { it.isActive }
            
            if (activePlans.isNotEmpty()) {
                // Select first plan by default
                selectedPlan = activePlans[0]
                updatePriceDisplay(activePlans[0])
                updateTrialHeadline(activePlans[0])

                binding?.apply {
                    llPlans.removeAllViews()
                    val inflater = LayoutInflater.from(root.context)

                    if (activePlans.size > 1) {
                        // Build non-scrolling list only when there are multiple plans
                        llPlans.visibility = View.VISIBLE
                        activePlans.forEachIndexed { index, plan ->
                            val itemBinding = ItemSubscriptionPlanBinding.inflate(inflater, llPlans, false)
                            bindPlanItem(itemBinding, plan, index == 0)
                            itemBinding.root.setOnClickListener {
                                selectedPlan = plan
                                updatePriceDisplay(plan)
                                updateTrialHeadline(plan)
                                populateFeaturesList(plan)

                                // Update selection visuals for all children
                                for (i in 0 until llPlans.childCount) {
                                    val child = llPlans.getChildAt(i)
                                    val childBinding = ItemSubscriptionPlanBinding.bind(child)
                                    val isSelectedChild = (i == index)
                                    updatePlanSelectionVisuals(childBinding, isSelectedChild)
                                }

                                recordClickEvent("subscription_plan_selected", hashMapOf(
                                    Pair("plan_id", plan.id),
                                    Pair("pg_plan_id", plan.pgPlanId),
                                    Pair("plan_name", plan.name),
                                    Pair("plan_interval", plan.interval),
                                    Pair("plan_price", plan.price),
                                    Pair("discounted_price", plan.discountedPrice),
                                    Pair("has_discount", plan.hasDiscount),
                                    Pair("trial_days", plan.trialDays)
                                ))
                            }
                            llPlans.addView(itemBinding.root)
                        }
                    } else {
                        // Single plan: hide selector; the user already sees the chosen plan in the price row.
                        llPlans.visibility = View.GONE
                    }

                    // Show features list and video always when we have plans.
                    ivDownloadsReviews?.visibility = View.VISIBLE
                    populateFeaturesList(activePlans[0])
                    videoSubscriptionExplainer.visibility = View.VISIBLE
                }
            } else {
                showErrorLoading(getString(R.string.no_active_subscription_plans))
            }
        } else {
            showErrorLoading(getString(R.string.no_subscription_plans))
        }
    }
    
    private fun updateTrialHeadline(plan: com.dailybook.keep.model.subscription.SubscriptionPlan) {
        binding?.apply {
            if (isTrialExpired) {
                // Trial expired: show "Special Offer for you 🎁"
                llTrialHeadline.visibility = View.VISIBLE
                tvTrialDays.text = getString(R.string.special_offer_for_you)
                tvTrialDays.visibility = View.VISIBLE
                tvFreePill.visibility = View.GONE
                tvTrialLabel.visibility = View.GONE
                tvTrialRefundNote.visibility = View.GONE
            } else if (plan.hasTrial) {
                llTrialHeadline.visibility = View.VISIBLE
                tvTrialDays.text = if (plan.trialDays == 1) getString(R.string.trial_headline_day, 1) else getString(R.string.trial_headline_days, plan.trialDays)
                tvTrialDays.visibility = View.VISIBLE
                tvFreePill.visibility = View.VISIBLE
                tvTrialLabel.visibility = View.VISIBLE
                tvTrialRefundNote.visibility = View.VISIBLE
            } else {
                // No free trial: hide the trial headline entirely
                llTrialHeadline.visibility = View.GONE
                tvTrialRefundNote.visibility = View.GONE
            }
        }
    }

    private fun updatePriceDisplay(plan: com.dailybook.keep.model.subscription.SubscriptionPlan) {
        binding?.apply {
            // Always show the payable price (discounted if available) and strike-through original price when discounted.
            val payablePrice = if (plan.hasDiscount) plan.discountedPrice else plan.price
            tvSelectedPrice.text = "₹$payablePrice"
            
            // Show original price with strikethrough only when there is a real discount.
            if (plan.hasDiscount) {
                tvOriginalPrice.visibility = android.view.View.VISIBLE
                tvOriginalPrice.text = "₹${plan.price}"
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvOriginalPrice.visibility = android.view.View.GONE
            }

            // Show interval text like "/month unless cancelled" under the price.
            val intervalLabel = formatIntervalForTrial(plan.interval)
            val perIntervalText = when (intervalLabel) {
                "month" -> getString(R.string.price_per_month_unless_cancelled)
                "year" -> getString(R.string.price_per_year_unless_cancelled)
                "quarter" -> getString(R.string.price_per_quarter_unless_cancelled)
                else -> getString(R.string.price_per_interval_unless_cancelled, intervalLabel)
            }
            tvPerInterval.visibility = android.view.View.VISIBLE
            tvPerInterval.text = perIntervalText
            
            // Button: "START FREE" for trial, "Start pro" for non-trial
            btnStartTrial.text = if (plan.hasTrial) getString(R.string.start_free) else getString(R.string.start_pro)
        }
    }

    private fun bindPlanItem(
        itemBinding: ItemSubscriptionPlanBinding,
        plan: com.dailybook.keep.model.subscription.SubscriptionPlan,
        isSelected: Boolean
    ) {
        itemBinding.apply {
            val finalPrice = if (plan.hasDiscount) plan.discountedPrice else plan.price

            tvPlanName.text = plan.interval.replaceFirstChar { it.uppercase() }

            if (plan.hasDiscount) {
                tvDiscountBadge.visibility = View.VISIBLE
                tvDiscountBadge.text = "${plan.discountPercent}% OFF"
            } else {
                tvDiscountBadge.visibility = View.GONE
            }

            val weeksInPeriod = when (plan.interval.lowercase()) {
                "yearly", "year" -> 52
                "monthly", "month" -> 4
                "quarterly", "quarter" -> 13
                "weekly", "week" -> 1
                else -> 4
            }
            val weeklyPrice = finalPrice / weeksInPeriod

            val intervalText = when (plan.interval.lowercase()) {
                "yearly", "year" -> "yearly"
                "monthly", "month" -> "monthly"
                "quarterly", "quarter" -> "quarterly"
                "weekly", "week" -> "weekly"
                else -> plan.interval.lowercase()
            }
            tvPlanBreakdown.text =
                "₹$weeklyPrice/weekly, billed $intervalText at ₹$finalPrice"

            updatePlanSelectionVisuals(this, isSelected)
        }
    }

    private fun updatePlanSelectionVisuals(
        itemBinding: ItemSubscriptionPlanBinding,
        isSelected: Boolean
    ) {
        itemBinding.ivSelectionCheck.visibility =
            if (isSelected) View.VISIBLE else View.GONE
        itemBinding.clPlanRoot.setBackgroundResource(
            if (isSelected) R.drawable.plan_selected_border else R.drawable.plan_unselected_border
        )
    }

    private fun getStaticReviews(): List<PaywallReviewItem> = listOf(
        PaywallReviewItem("Sai Rawal", "Very nice app, great for attendance marking. Easy to use and accurate.", avatarResId = R.drawable.paywall_user1),
        PaywallReviewItem("Sasc interior Contractor", "Best app for labor management. Saves a lot of time.", avatarResId = R.drawable.paywall_user2),
        PaywallReviewItem("Ankit", "Simple and effective. Attendance and payments in one place.", avatarResId = R.drawable.paywall_user3),
        PaywallReviewItem("SUNDER SINGH", "वेरी नाइस अप बहुत मस्त है हाजिरी चढ़ाने का", avatarResId = R.drawable.paywall_user4),
        PaywallReviewItem("Jagdish Dehury", "Happy with the app. Good for tracking staff and wages.", avatarResId = R.drawable.paywall_user5)
    )
    
    /**
     * Start auto-scrolling reviews carousel in a loop.
     * Smoothly scrolls through reviews every 3 seconds, then loops back to the start.
     * Includes multiple safety checks to prevent crashes.
     * Feature can be toggled via Remote Config: "paywall_reviews_auto_scroll_enabled"
     */
    private fun startReviewsAutoScroll() {
        // Check Remote Config flag - feature can be disabled remotely if issues occur
        val isAutoScrollEnabled = try {
            remoteConfig.getBoolean("paywall_reviews_auto_scroll_enabled")
        } catch (e: Exception) {
            Timber.e(e, "Error reading remote config for auto-scroll, defaulting to true")
            true // Default to enabled if config read fails
        }
        
        if (!isAutoScrollEnabled) {
            Timber.d("Reviews auto-scroll disabled via Remote Config")
            return
        }
        
        // Cancel any existing auto-scroll job
        reviewsAutoScrollJob?.cancel()
        
        reviewsAutoScrollJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Safety check: ensure fragment is added and view is available
                if (!isAdded || binding == null) return@launch
                
                val recyclerView = binding?.rvReviews ?: return@launch
                val itemCount = getStaticReviews().size
                
                // Safety check: ensure adapter is set and has items
                if (recyclerView.adapter == null || itemCount <= 0) return@launch
                
                var currentPosition = 0
                
                // Wait a bit before starting auto-scroll
                delay(2000)
                
                while (isAdded && binding != null) {
                    try {
                        // Safety checks before each scroll
                        val currentBinding = binding ?: break
                        val currentRecyclerView = currentBinding.rvReviews
                        
                        // Check if RecyclerView is still attached and visible
                        if (!currentRecyclerView.isAttachedToWindow) break
                        if (currentRecyclerView.adapter == null) break
                        if (currentRecyclerView.adapter?.itemCount ?: 0 <= 0) break
                        
                        // Calculate next position safely
                        val safeItemCount = currentRecyclerView.adapter?.itemCount ?: itemCount
                        if (safeItemCount <= 0) break
                        
                        currentPosition = (currentPosition + 1) % safeItemCount
                        
                        // Smooth scroll to next position
                        currentRecyclerView.smoothScrollToPosition(currentPosition)
                        
                        // Wait before scrolling to next item (3 seconds per review)
                        delay(3000)
                        
                        // When we reach the end, add a brief pause before looping
                        if (currentPosition == 0) {
                            delay(500)
                        }
                    } catch (e: Exception) {
                        // Log error but continue (might be transient)
                        Timber.e(e, "Error during review auto-scroll")
                        // Small delay before retrying
                        delay(1000)
                    }
                }
            } catch (e: Exception) {
                // Catch any outer exceptions
                Timber.e(e, "Fatal error in review auto-scroll, stopping")
            }
        }
    }

    /** Button text: "START FREE" for trial, "Try now" for non-trial. */
    private fun getStartTrialButtonText(plan: com.dailybook.keep.model.subscription.SubscriptionPlan?): CharSequence {
        if (plan == null) return getString(R.string.start)
        return if (plan.hasTrial) getString(R.string.start_free) else getString(R.string.try_now)
    }

    /**
     * Returns a display label for the plan's billing interval (e.g. month, week, year, quarter)
     * for use in trial text like "3 days free trial then ₹99/month".
     */
    private fun formatIntervalForTrial(interval: String): String {
        val normalized = interval.trim().lowercase()
        return when {
            normalized in listOf("month", "monthly") -> "month"
            normalized in listOf("year", "yearly") -> "year"
            normalized in listOf("week", "weekly") -> "week"
            normalized in listOf("quarter", "quarterly") -> "quarter"
            normalized.isNotEmpty() -> normalized
            else -> "month"
        }
    }
    
    /**
     * Formats feature text for two-line display: one word stays on one line;
     * two or more words: split after first space (first word line 1, rest line 2). Never crashes.
     */
    private fun formatFeatureTwoLines(text: CharSequence?): String {
        val s = text?.toString()?.trim() ?: return ""
        if (s.isEmpty()) return ""
        val firstSpace = s.indexOf(' ')
        return if (firstSpace < 0) s else s.replaceFirst(" ", "\n")
    }

    /** Offer timer banner controlled via Firebase Remote Config + first app-open timestamp fallback. */
    private suspend fun setupOfferTimerFromRemoteConfig() {
        val binding = binding ?: return
        val isEnabled = try {
            remoteConfig.getBoolean("paywall_offer_timer_enabled")
        } catch (e: Exception) {
            Timber.e(e, "Error reading paywall_offer_timer_enabled from Remote Config, defaulting to true")
            true
        }

        if (!isEnabled) {
            binding.llOfferTimer.visibility = View.GONE
            stopOfferCountdown()
            return
        }

        // Absolute end timestamp in epoch millis (UTC) from Remote Config.
        val endEpochMs = try {
            remoteConfig.getLong("paywall_offer_end_epoch_ms")
        } catch (e: Exception) {
            Timber.e(e, "Error reading paywall_offer_end_epoch_ms from Remote Config")
            0L
        }

        // If end timestamp not yet configured, fallback to local offer end timestamp from DataStore.
        val effectiveEndEpochMs = if (endEpochMs > 0L) {
            endEpochMs
        } else {
            dataStoreManager.read(DataStoreManager.PREMIUM_OFFER_LOCAL_END_EPOCH_MS, 0L).first()
        }

        val remainingMs: Long = effectiveEndEpochMs - System.currentTimeMillis()

        if (remainingMs <= 0L) {
            binding.llOfferTimer.visibility = View.GONE
            stopOfferCountdown()
            return
        }

        // Convert to an absolute end time on the monotonic clock so we keep correct remaining time across pause/resume.
        offerTimerEndAtElapsedMs = SystemClock.elapsedRealtime() + remainingMs
        binding.llOfferTimer.visibility = View.VISIBLE
        maybeStartOfferCountdown()
    }

    private fun maybeStartOfferCountdown() {
        val binding = binding ?: return
        val endAt = offerTimerEndAtElapsedMs ?: return
        if (binding.llOfferTimer.visibility != View.VISIBLE) return

        // Cancel any previous runnable
        stopOfferCountdown()

        offerTimerRunnable = object : Runnable {
            override fun run() {
                val currentBinding = binding ?: return
                val remainingMs = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                val remainingSeconds = remainingMs / 1000L

                if (remainingSeconds <= 0L) {
                    currentBinding.llOfferTimer.visibility = View.GONE
                    stopOfferCountdown()
                    return
                }

                currentBinding.tvOfferTimer.text = formatOfferDuration(remainingSeconds)
                currentBinding.llOfferTimer.postDelayed(this, 1000L)
            }
        }

        // Run immediately so the UI updates right away
        offerTimerRunnable?.run()
    }

    private fun stopOfferCountdown() {
        val binding = binding ?: return
        offerTimerRunnable?.let { binding.llOfferTimer.removeCallbacks(it) }
        offerTimerRunnable = null
    }

    /**
     * Formats remaining time in a human-friendly way:
     * - >= 1 day:    "2d 12h"
     * - >= 1 hour:   "2h 4m"
     * - >= 1 minute: "2m 20s"
     * - <  1 minute: "20s"
     */
    private fun formatOfferDuration(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0L)
        val days = safeSeconds / (24 * 60 * 60)
        val hours = (safeSeconds % (24 * 60 * 60)) / (60 * 60)
        val minutes = (safeSeconds % (60 * 60)) / 60
        val seconds = safeSeconds % 60

        return when {
            days > 0L -> if (hours > 0L) String.format("%dd %dh", days, hours) else String.format("%dd", days)
            hours > 0L -> if (minutes > 0L) String.format("%dh %dm", hours, minutes) else String.format("%dh", hours)
            minutes > 0L -> if (seconds > 0L) String.format("%dm %ds", minutes, seconds) else String.format("%dm", minutes)
            else -> String.format("%ds", seconds)
        }
    }

    /** Populates dynamic features list from plan metadata (multiple plans design). */
    private fun populateFeaturesList(plan: com.dailybook.keep.model.subscription.SubscriptionPlan) {
        binding?.llFeatures?.apply {
            removeAllViews()
            val features = plan.metaData.features
            val iconResIds = listOf(
                R.drawable.ic_ads_free,
                R.drawable.ic_staff_lb,
                R.drawable.ic_support_lb
            )
            val maxItems = minOf(3, features.size, iconResIds.size)
            for (i in 0 until maxItems) {
                val itemView = layoutInflater.inflate(R.layout.item_paywall_feature, this, false)
                itemView.findViewById<android.widget.ImageView>(R.id.iv_feature_icon).setImageResource(iconResIds[i])
                itemView.findViewById<android.widget.TextView>(R.id.tv_feature_text).text = formatFeatureTwoLines(features.getOrNull(i))
                val params = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                addView(itemView, params)
            }
        }
    }
    
    /**
     * Get Razorpay-specific UPI app code
     */
    private fun getUpiAppCode(packageName: String): String {
        return when (packageName) {
            "com.phonepe.app" -> "phonepe"
            "com.google.android.apps.nbu.paisa.user" -> "gpay"
            "com.google.android.apps.navi.market.activity" -> "gpay"
            "com.google.android.apps.walletnfcrel" -> "gpay"
            "net.one97.paytm" -> "paytm"
            "in.org.npci.upiapp" -> "bhim"
            "in.amazon.mShop.android.shopping" -> "amazonpay"
            "com.freecharge.android" -> "freecharge"
            "com.mobikwik_new" -> "mobikwik"
            "com.myairtelapp" -> "airtel"
            else -> "phonepe" // Default fallback for other UPI apps
        }
    }
    
    /**
     * Razorpay Custom SDK 1.4: Fetch enabled payment methods for the API key.
     * For subscriptions you can pass subscription_id in options to get amount etc.; we use UPI only.
     */
    private fun fetchPaymentMethods() {
        // RAZORPAY DISABLED: Original code commented out
        // razorpayCustom?.getPaymentMethods(object : PaymentMethodsCallback {
        //     override fun onPaymentMethodsReceived(result: String?) {
        //         if (result != null) Timber.d("Payment methods: $result")
        //     }
        //     override fun onError(error: String?) {
        //         Timber.e("getPaymentMethods error: $error")
        //     }
        // })
    }

    /**
     * Load UPI apps for Autopay Intent. Per Razorpay doc use Razorpay.getAppsWhichSupportUpi(this) or
     * Razorpay.getAppsWhichSupportAutopayIntent(this) { appList -> ... }; current Custom UI SDK (3.9.22)
     * does not expose these, so we use [UpiAppDetector] and parseRazorpayUpiAppList() when SDK adds the API.
     */
    private fun loadUpiAppsViaRazorpay() {
        loadUpiAppsFallback()
    }

    private fun loadUpiAppsFallback() {
        val list = UpiAppDetector.getAllUpiAppsWithInstalledState(requireContext())
        installedUpiApps = list
        val firstInstalled = list.firstOrNull { it.isInstalled }
        if (firstInstalled != null) {
            selectedUpiApp = firstInstalled
            binding?.tvSelectedUpi?.text = firstInstalled.displayName
        } else {
            selectedUpiApp = null
            binding?.tvSelectedUpi?.text = getString(R.string.select_upi_app)
        }
    }

    /**
     * Parse Razorpay getAppsWhichSupportAutopayIntent result into [InstalledUpiApp] list.
     * Use when switching to Razorpay.getAppsWhichSupportAutopayIntent(activity) { appList -> ... }.
     */
    private fun parseRazorpayUpiAppList(appList: Any?): List<InstalledUpiApp> {
        if (appList == null) return emptyList()
        val list = mutableListOf<InstalledUpiApp>()
        val pm = requireContext().packageManager
        try {
            when (appList) {
                is JSONArray -> {
                    for (i in 0 until appList.length()) {
                        val obj = appList.optJSONObject(i) ?: continue
                        val packageName = (obj.optString("package", "").ifBlank { obj.optString("package_name", "") }).takeIf { it.isNotBlank() } ?: continue
                        val displayName = obj.optString("name", "").ifBlank { obj.optString("display_name", "") }
                            .ifBlank { obj.optString("label", "") }.ifBlank { packageName }
                        try {
                            val icon = pm.getApplicationIcon(packageName)
                            val appLabel = try { pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString() } catch (_: Exception) { displayName }
                            list.add(InstalledUpiApp(packageName = packageName, displayName = displayName, appLabel = appLabel, icon = icon, isInstalled = true))
                        } catch (_: Exception) { /* skip if app not installed or no icon */ }
                    }
                }
                else -> list.addAll(UpiAppDetector.getAllUpiAppsWithInstalledState(requireContext()))
            }
        } catch (e: Exception) {
            Timber.e(e, "parseRazorpayUpiAppList error")
            list.addAll(UpiAppDetector.getAllUpiAppsWithInstalledState(requireContext()))
        }
        return list
    }

    private fun showUpiSelectionBottomSheet() {
        recordClickEvent(ConstantEventNames.SELECT_UPI_APP)
        val bottomSheet = UpiSelectionBottomSheet.newInstance(selectedUpiApp?.packageName ?: "")
        bottomSheet.setInstalledUpiApps(installedUpiApps)
        bottomSheet.setOnUpiSelectedListener { upiApp ->
            selectedUpiApp = upiApp
            binding?.tvSelectedUpi?.text = upiApp.displayName
            FacebookPaymentEvents.logAddPaymentInfo(requireContext())
        }
        bottomSheet.show(parentFragmentManager, UpiSelectionBottomSheet.TAG)
    }

    private fun handleStartTrial() {
        lifecycleScope.launch(Dispatchers.IO) {
            analytics.logEvent(ConstantEventNames.START_TRIAL_CLICK, Analytics.CLICK, listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE))
        }

        // Pause subscription explainer video when user initiates subscription/payment flow
        binding?.videoSubscriptionExplainer?.apply {
            if (isPlaying) {
                pause()
            }
        }
        
        if (selectedPlan == null) {
            showError(getString(R.string.please_select_subscription_plan))
            return
        }
        if (selectedUpiApp == null || !selectedUpiApp!!.isInstalled) {
            showError(getString(R.string.no_upi_apps_found))
            showUpiSelectionBottomSheet()
            return
        }
        
        lifecycleScope.launch {
            val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
            val mobileNumber = dataStoreManager.read(DataStoreManager.MOBILE_NUMBER, "").first()
            val upiId = "$mobileNumber@ybl"
            
            recordClickEvent("create_subscription_initiated", hashMapOf(
                Pair("user_id", userId),
                Pair("plan_id", selectedPlan!!.id),
                Pair("pg_plan_id", selectedPlan!!.pgPlanId),
                Pair("upi_id", upiId)
            ))
            val plan = selectedPlan!!
            FacebookPaymentEvents.logInitiateCheckout(
                requireContext(),
                plan.id,
                if (plan.hasTrial) 0.0 else plan.discountedPrice.toDouble(),
                plan.currency,
                installSource
            )
            viewModel.createSubscription(userId, plan.id, upiId, plan.hasTrial)
        }
    }
    
    // RAZORPAY DISABLED: Payment initiation commented out
    private fun initiateRazorpayPayment(response: com.dailybook.keep.model.subscription.CreateSubscriptionResponse) {
        // currentSubscriptionId = response.subscriptionId
        // val upiApp = selectedUpiApp
        // if (upiApp == null) {
        //     showPaymentFailedBottomSheet(getString(R.string.please_select_upi_app))
        //     return
        // }
        // initiatePaymentWithSubscription(response, upiApp)
        Toast.makeText(requireContext(), "Payments are currently disabled", Toast.LENGTH_SHORT).show()
    }

    // RAZORPAY DISABLED: SDK submission commented out
    private fun initiatePaymentWithSubscription(
        response: com.dailybook.keep.model.subscription.CreateSubscriptionResponse,
        upiApp: InstalledUpiApp
    ) {
        // val razorpay = razorpayCustom
        // val webView = binding?.paymentWebview
        // if (razorpay == null || webView == null) {
        //     showPaymentFailedBottomSheet(getString(R.string.failed_to_initiate_payment, "Razorpay not initialized"))
        //     return
        // }
        // val plan = selectedPlan ?: run {
        //     showPaymentFailedBottomSheet(getString(R.string.please_select_subscription_plan))
        //     return
        // }
        // val amountPaise = if (plan.hasTrial) 500 else (plan.discountedPrice * 100)
        // if (amountPaise < 100) {
        //     showPaymentFailedBottomSheet(getString(R.string.failed_to_initiate_payment, "Invalid amount"))
        //     return
        // }
        // lifecycleScope.launch {
        //     val mobileNumber = dataStoreManager.read(DataStoreManager.MOBILE_NUMBER, "").first()
        //     val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
        //     val email = "${userId.take(8)}@dailybook.app"
        //     val upiAppPackageName = upiApp.packageName
        //     val preferredAppsOrder = JSONArray().apply {
        //         put("com.phonepe.app")
        //         put("com.google.android.apps.nbu.paisa.user")
        //         put("net.one97.paytm")
        //     }
        //     val otherAppsOrder = JSONArray().apply {
        //         put("in.org.npci.upiapp")
        //     }
        //     val subscriptionId = response.pgSubscriptionId.takeIf { it.isNotBlank() }
        //     if (subscriptionId.isNullOrBlank()) {
        //         showPaymentFailedBottomSheet(getString(R.string.failed_to_initiate_payment, "Subscription ID not available"))
        //         return@launch
        //     }
        //     val data = JSONObject().apply {
        //         put("amount", amountPaise)
        //         put("currency", plan.currency)
        //         put("contact", mobileNumber)
        //         put("email", email)
        //         put("description", getString(R.string.pro_subscription))
        //         put("method", "upi")
        //         put("_[flow]", "intent")
        //         put("upi_app_package_name", upiAppPackageName)
        //         put("recurring", 1)
        //         put("preferred_apps_order", preferredAppsOrder)
        //         put("other_apps_order", otherAppsOrder)
        //         put("subscription_id", subscriptionId)
        //     }
        //     razorpay.setWebView(webView)
        //     webView.webViewClient = createPaymentWebViewClient()
        //     webView.visibility = View.VISIBLE
        //     try {
        //         razorpay.submit(data, this@PremiumOfferDialogFragment)
        //     } catch (e: Exception) {
        //         webView.visibility = View.GONE
        //         hideVerificationScreen()
        //         showPaymentFailedBottomSheet(getString(R.string.failed_to_initiate_payment, e.message ?: "Unknown error"))
        //     }
        // }
        Toast.makeText(requireContext(), "Payments are currently disabled", Toast.LENGTH_SHORT).show()
    }

    /** Hides Razorpay Custom UI WebView after payment completes (success or error). */
    private fun hidePaymentWebView() {
        binding?.paymentWebview?.visibility = View.GONE
    }
    
    // RAZORPAY DISABLED: Was override from PaymentResultWithDataListener
    fun onPaymentSuccess(razorpayPaymentId: String, paymentData: Any?) {
        // hidePaymentWebView()
        // recordClickEvent("payment_success", hashMapOf(
        //     Pair("payment_id", razorpayPaymentId),
        //     Pair("subscription_id", currentSubscriptionId ?: "")
        // ))
        // selectedPlan?.let { plan ->
        //     val amount = if (plan.hasTrial) 0.0 else plan.discountedPrice.toDouble()
        //     FacebookPaymentEvents.logPurchase(requireContext(), amount, plan.currency, installSource)
        // }
        // showVerificationScreen(getString(R.string.verifying_payment))
        // val razorpaySubscriptionId = paymentData?.data?.optString("razorpay_subscription_id") ?: ""
        // val signature = paymentData?.data?.optString("razorpay_signature") ?: paymentData?.signature ?: ""
        // if (currentSubscriptionId != null && razorpaySubscriptionId.isNotEmpty()) {
        //     val verifyRequest = VerifySubscriptionRequest(
        //         razorpaySubscriptionId = razorpaySubscriptionId,
        //         razorpayPaymentId = razorpayPaymentId,
        //         razorpaySignature = signature
        //     )
        //     startVerificationPolling(currentSubscriptionId!!, verifyRequest)
        // } else {
        //     hideVerificationScreen()
        //     showPaymentFailedBottomSheet(getString(R.string.payment_failed_try_again))
        // }
    }

    // RAZORPAY DISABLED: Was override from PaymentResultWithDataListener
    fun onPaymentError(errorCode: Int, errorMessage: String, paymentData: Any?) {
        // hidePaymentWebView()
        // try {
        //     val isCancellation = errorMessage.contains("cancelled", ignoreCase = true) ||
        //                         errorMessage.contains("back pressed", ignoreCase = true) ||
        //                         errorCode == 0
        //     recordClickEvent(
        //         "payment_error",
        //         hashMapOf(
        //             Pair("error_code", errorCode),
        //             Pair("error_message", errorMessage.take(100)),
        //             Pair("is_cancellation", isCancellation),
        //             Pair("subscription_id", currentSubscriptionId ?: "")
        //         )
        //     )
        //     showVerificationScreen(getString(R.string.checking_payment_status))
        //     val razorpaySubscriptionId = paymentData?.data?.optString("razorpay_subscription_id") ?: ""
        //     val razorpayPaymentId = paymentData?.data?.optString("razorpay_payment_id") ?: ""
        //     val signature = paymentData?.data?.optString("razorpay_signature") ?: paymentData?.signature ?: ""
        //     if (currentSubscriptionId != null && razorpaySubscriptionId.isNotEmpty() && razorpayPaymentId.isNotEmpty()) {
        //         val verifyRequest = VerifySubscriptionRequest(
        //             razorpaySubscriptionId = razorpaySubscriptionId,
        //             razorpayPaymentId = razorpayPaymentId,
        //             razorpaySignature = signature
        //         )
        //         startVerificationPolling(currentSubscriptionId!!, verifyRequest)
        //     } else if (currentSubscriptionId != null) {
        //         startStatusPolling(currentSubscriptionId!!)
        //     } else {
        //         hideVerificationScreen()
        //         showPaymentFailedBottomSheet(getString(R.string.payment_failed_try_again))
        //     }
        // } catch (e: Exception) {
        //     e.printStackTrace()
        //     try {
        //         hideVerificationScreen()
        //         showPaymentFailedBottomSheet(getString(R.string.payment_failed_try_again))
        //     } catch (e2: Exception) { e2.printStackTrace() }
        // }
    }
    
    /**
     * Show payment verification overlay
     */
    private fun showVerificationScreen(message: String) {
        binding?.apply {
            clVerificationOverlay.visibility = android.view.View.VISIBLE
            tvVerificationMessage.text = message
            
            // Disable interactions with main content
            scrollContent.isEnabled = false
            clBottomSection.isEnabled = false
            
            // Hide close button during verification
            ivClose.visibility = android.view.View.GONE
        }
        
        // Prevent dialog dismissal during verification
        isCancelable = false
    }
    
    /**
     * Hide payment verification overlay
     */
    private fun hideVerificationScreen() {
        binding?.apply {
            clVerificationOverlay.visibility = android.view.View.GONE
            
            // Re-enable interactions
            scrollContent.isEnabled = true
            clBottomSection.isEnabled = true
            
            // Show close button again
            ivClose.visibility = android.view.View.VISIBLE
        }
        
        // Allow dialog dismissal again
        isCancelable = true
        
        isVerificationInProgress = false
        verificationAttempts = 0
    }
    
    /**
     * Start polling to verify payment with backend
     */
    private fun startVerificationPolling(
        subscriptionId: String,
        verifyRequest: VerifySubscriptionRequest
    ) {
        if (isVerificationInProgress) return
        
        isVerificationInProgress = true
        verificationAttempts = 0
        
        lifecycleScope.launch {
            while (verificationAttempts < maxVerificationAttempts && isVerificationInProgress) {
                verificationAttempts++
                
                // Call verify API
                viewModel.verifySubscription(subscriptionId, verifyRequest)
                
                // Wait for response (observeViewModel will handle it)
                delay(3000) // Poll every 3 seconds
            }
            
            // Max attempts reached with verify API
            // Fallback to status polling as verification might have signature issues
            if (isVerificationInProgress) {
                // Reset attempts and switch to status polling
                verificationAttempts = 0
                binding?.tvVerificationMessage?.text = getString(R.string.checking_subscription_status)
                
                // Continue with status polling for 3 more attempts
                startStatusPolling(subscriptionId)
            }
        }
    }
    
    /**
     * Start polling to check subscription status (when payment data is missing)
     */
    private fun startStatusPolling(subscriptionId: String) {
        if (isVerificationInProgress) return
        
        isVerificationInProgress = true
        verificationAttempts = 0
        
        lifecycleScope.launch {
            val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
            
            while (verificationAttempts < maxVerificationAttempts && isVerificationInProgress) {
                verificationAttempts++
                
                // Check subscription status from backend
                viewModel.checkUserSubscriptionStatus(userId)
                
                // Wait for response
                delay(3000) // Poll every 3 seconds
            }
            
            // Max attempts reached
            if (isVerificationInProgress) {
                hideVerificationScreen()
                showPaymentFailedBottomSheet(getString(R.string.payment_failed_try_again))
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding?.apply {
            if (show) {
                // Show progress bar overlay
                clLoadingOverlay.visibility = android.view.View.VISIBLE
                pbLoading.visibility = android.view.View.VISIBLE
                
                // Hide headline and price while loading (plans list will be rebuilt on success)
                llTrialHeadline.visibility = android.view.View.GONE
                llPriceDisplay.visibility = android.view.View.GONE
                llPlans.visibility = android.view.View.GONE
                
                // Disable button and show processing state
                btnStartTrial.isEnabled = false
                btnStartTrial.text = getString(R.string.processing)
            } else {
                // Hide progress bar overlay
                clLoadingOverlay.visibility = android.view.View.GONE
                pbLoading.visibility = android.view.View.GONE
                
                // Show headline and price (rvSubscriptionPlans visibility set in handlePlansLoaded)
                llTrialHeadline.visibility = android.view.View.VISIBLE
                llPriceDisplay.visibility = android.view.View.VISIBLE
                
                // Enable button and restore text based on selected plan
                btnStartTrial.isEnabled = true
                btnStartTrial.text = getStartTrialButtonText(selectedPlan)
            }
        }
    }
    
    private fun showSpinnerLoading(show: Boolean) {
        binding?.apply {
            if (show) {
                // Show spinner in button only (for subscription creation)
                pbButtonLoading.visibility = android.view.View.VISIBLE
                
                // Disable button and show processing state
                btnStartTrial.isEnabled = false
                btnStartTrial.text = getString(R.string.processing)
            } else {
                // Hide button spinner
                pbButtonLoading.visibility = android.view.View.GONE
                
                // Enable button and restore text based on selected plan
                btnStartTrial.isEnabled = true
                btnStartTrial.text = getStartTrialButtonText(selectedPlan)
            }
        }
    }
    
    private fun showErrorLoading(message: String) {
        binding?.apply {
            // Show loading overlay with spinner (for unavailable data/errors)
            clLoadingOverlay.visibility = android.view.View.VISIBLE
            pbLoading.visibility = android.view.View.VISIBLE
            
            // Disable button
            btnStartTrial.isEnabled = false
            btnStartTrial.text = getString(R.string.processing)
            
            // Disable scrolling
            scrollContent.isEnabled = false
        }
        
        // Show error message and hide spinner after showing error
        showError(message)
        
        // Hide spinner after error is shown
        binding?.apply {
            lifecycleScope.launch {
                delay(1500) // Show spinner briefly while error is displayed
                clLoadingOverlay.visibility = android.view.View.GONE
                pbLoading.visibility = android.view.View.GONE
                
                // Enable button
                btnStartTrial.isEnabled = true
                btnStartTrial.text = getStartTrialButtonText(selectedPlan)
                
                // Enable scrolling
                scrollContent.isEnabled = true
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Show payment failed bottom sheet (design language) and keep user on subscription page.
     */
    private fun showPaymentFailedBottomSheet(message: String) {
        Timber.e("showPaymentFailedBottomSheet: message=$message")
        restoreSubscriptionPageUI()
        val bottomSheet = PaymentFailedBottomSheet.newInstance(message)
        bottomSheet.show(parentFragmentManager, PaymentFailedBottomSheet.TAG)
    }
    
    /**
     * Restore subscription page UI after payment failure: hide overlays, re-enable button and scroll.
     */
    private fun restoreSubscriptionPageUI() {
        paymentFlowInProgress = false
        hideVerificationScreen()
        binding?.apply {
            clLoadingOverlay.visibility = android.view.View.GONE
            pbLoading.visibility = android.view.View.GONE
            pbButtonLoading.visibility = android.view.View.GONE
            scrollContent.isEnabled = true
            btnStartTrial.isEnabled = true
            btnStartTrial.text = getStartTrialButtonText(selectedPlan)
        }
    }
    
    private fun showSuccessDialog() {
        Timber.d("showSuccessDialog: dismissing premium offer and showing success dialog")
        paymentFlowInProgress = false
        selectedPlan?.let { plan ->
            if (plan.hasTrial) {
                FacebookPaymentEvents.logStartTrial(requireContext(), 0.0, plan.currency, plan.id, installSource)
            } else {
                FacebookPaymentEvents.logSubscribe(requireContext(), plan.discountedPrice.toDouble(), plan.currency, plan.id, installSource)
            }
        }
        // Dismiss premium offer dialog first
        dismiss()
        
        // Show success dialog with API refresh
        val successDialog = SubscriptionSuccessDialogFragment.newInstance()
        successDialog.show(parentFragmentManager, SubscriptionSuccessDialogFragment.TAG)
    }
    
    // Analytics helper methods
    private fun triggerImpressionEvent(eventName: String, hashMap: HashMap<String, Any>? = null) {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    analytics.logEvent(
                        eventName,
                        Analytics.IMPRESSION,
                        listOf(AnalyticsPlatforms.FIREBASE),
                        hashMap
                    )
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
    }

    private fun recordClickEvent(eventName: String, hashMap: HashMap<String, Any>? = null) {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    analytics.logEvent(
                        eventName,
                        Analytics.CLICK,
                        listOf(AnalyticsPlatforms.FIREBASE),
                        hashMap
                    )
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
    }

    /**
     * Forward activity result to Razorpay for UPI Intent flow. Doc: override onActivityResult in activity
     * and pass to SDK: if (razorpay != null) razorpay.onActivityResult(requestCode, resultCode, data).
     */
    fun onActivityResultForRazorpay(requestCode: Int, resultCode: Int, data: Intent?) {
        // RAZORPAY DISABLED: SDK activity result forwarding commented out
        // razorpayCustom?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop subscription explainer video
        binding?.videoSubscriptionExplainer?.stopPlayback()
        // Stop offer timer runnable
        stopOfferCountdown()
        offerTimerEndAtElapsedMs = null
        // Stop auto-scroll for reviews
        reviewsAutoScrollJob?.cancel()
        reviewsAutoScrollJob = null
        // Stop any ongoing verification polling and release Razorpay Custom UI
        isVerificationInProgress = false
        razorpayCustom = null
        _binding = null
    }

    companion object {
        const val TAG = "PremiumOfferDialogFragment"
        private const val ARG_IS_TRIAL_EXPIRED = "is_trial_expired"

        @JvmStatic
        fun newInstance(isTrialExpired: Boolean = false) = PremiumOfferDialogFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_TRIAL_EXPIRED, isTrialExpired)
            }
        }
    }
}
