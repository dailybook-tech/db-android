package com.laborbook.keep.screen.home.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.laborbook.base.BaseConstants
import com.laborbook.base.BaseFragment
import com.laborbook.base.analytics.ConstantEventAttributes
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.base.analytics.ConstantEventSources
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.base.languagemanager.LanguageBottomSheetFragment
import com.laborbook.base.navigator.FragmentNavigator
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.laborbook.keep.databinding.FragmentSettingsBinding
import com.laborbook.keep.screen.premium.PremiumOfferDialogFragment
import com.laborbook.keep.screen.premium.PremiumOfferManager
import com.laborbook.keep.screen.profile.fragment.UpdateNameBottomsheetFragment
import com.laborbook.keep.utils.SubscriptionsFeatureFlag
import com.laborbook.keep.screen.profile.uistate.UserUiState
import com.laborbook.keep.screen.profile.viewmodel.UserProfileViewModel
import com.laborbook.keep.utils.CoachMarkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    override val screenName: String
        get() = ConstantEventNames.SETTINGS
    private val viewModel: UserProfileViewModel by sharedViewModel()
    private val coachMarkManager: CoachMarkManager by inject()
    private val premiumOfferManager: PremiumOfferManager by inject()
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentSettingsBinding? {
        return FragmentSettingsBinding.inflate(inflater,container,false)
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
        appUpdateManager = AppUpdateManagerFactory.create(requireContext())
        checkForUpdate()
        setupViews()
        viewModelObserver()
        registerOnClickListeners()
        setupDebugOptions()
        observeProStatusChanges()
        lifecycleScope.launch {
            viewModel.getUser(dataStoreManager.read(DataStoreManager.USER_ID,"").first())
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh Pro badge visibility when fragment resumes (e.g., after dismissing success dialog)
        updateProBadgeVisibility()
    }
    
    /**
     * Update Pro badge visibility based on current subscription status
     */
    private fun updateProBadgeVisibility() {
        lifecycleScope.launch {
            val isPro = dataStoreManager.read(DataStoreManager.PRO_STATUS, false).first()
            val subscriptionsEnabled = SubscriptionsFeatureFlag.isSubscriptionsEnabled(Firebase.remoteConfig)
            Timber.d("SettingsFragment: updateProBadgeVisibility isPro=$isPro subscriptionsEnabled=$subscriptionsEnabled")
            // Update UI based on Pro status if needed
            // For now, just log it - the click listener will check latest status
        }
    }

    private fun setupViews() {
        lifecycleScope.launch {
            binding?.apply {
                tvVersion.text = "V".plus(BaseConstants.APP_VERSION)
                tvName.text = dataStoreManager.read(DataStoreManager.USER_NAME, "").first()
                tvMobileNumber.text = dataStoreManager.read(DataStoreManager.MOBILE_NUMBER, "").first()
                val isAppLockEnabled = dataStoreManager.read(DataStoreManager.APP_LOCK_ENABLED, false).first()
                switchAppLock.isChecked = isAppLockEnabled
                
                // Hide Laborbook Pro card if subscriptions feature is disabled via Remote Config
                val remoteConfig = Firebase.remoteConfig
                if (!SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
                    clLaborbookPro.hide()
                } else {
                    clLaborbookPro.show()
                }
            }
        }
    }
    
    /**
     * Observe Pro status changes to update UI when subscription status changes
     */
    private fun observeProStatusChanges() {
        dataStoreManager.read(DataStoreManager.PRO_STATUS, false)
            .onEach { isPro ->
                Timber.d("SettingsFragment: PRO_STATUS changed to $isPro")
                // Pro status changed - update UI if needed
                // The click listener will check the latest status, so no UI update needed here
                // But we can add a visual indicator if needed in the future
            }
            .launchIn(lifecycleScope)
    }

    private fun registerOnClickListeners() {
        binding?.apply {
            clProfileName.setOnClickListener {
                fragmentNavigator.start(UpdateNameBottomsheetFragment.newInstance())
                recordClickEvent(ConstantEventNames.VIEW_PROFILE_NAME)
            }
        }

        binding?.clTerms?.setOnClickListener {
            val termsAndConditionsUrl = "https://laborbook.app/terms-of-service"
            openUrlInWebView(requireContext(), termsAndConditionsUrl, getString(com.laborbook.keep.R.string.terms_conditions))
            recordClickEvent(ConstantEventNames.VIEW_TERMS_AND_CONDITIONS)
        }

        binding?.clPrivacyPolicy?.setOnClickListener {
            val privacyPolicyUrl = "https://laborbook.app/privacy-policy"
            openUrlInWebView(requireContext(), privacyPolicyUrl, getString(com.laborbook.keep.R.string.privacy_policy))
            recordClickEvent(ConstantEventNames.VIEW_PRIVACY_POLICY)
        }

        binding?.clPricing?.setOnClickListener {
            val pricingUrl = "https://laborbook.app/pricing"
            openUrlInWebView(requireContext(), pricingUrl, null)
            recordClickEvent(ConstantEventNames.VIEW_PRICING)
        }

        binding?.clInviteFriends?.setOnClickListener {
            ReferFriendBottomSheetFragment.newInstance()
                .show(parentFragmentManager, ReferFriendBottomSheetFragment.TAG)
            recordClickEvent(ConstantEventNames.REFER_A_FRIEND, hashMapOf(Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.SETTINGS)))
        }

        binding?.ivToolbarLanguage?.setOnClickListener {
            fragmentNavigator.start(LanguageBottomSheetFragment.newInstance())
            recordClickEvent(ConstantEventNames.VIEW_LANGUAGES)
        }

        binding?.clRatings?.setOnClickListener {
            openAppReviewPage(requireContext())
            recordClickEvent(ConstantEventNames.OPEN_RATINGS)
        }

        binding?.clAppUpdate?.setOnClickListener {
            openAppUpdatePage(requireContext())
            recordClickEvent(ConstantEventNames.OPEN_APP_UPDATE)
        }

        binding?.btnAppUpdate?.setOnClickListener {
            openAppUpdatePage(requireContext())
            recordClickEvent(ConstantEventNames.OPEN_APP_UPDATE)
        }

        binding?.clRequestFeature?.setOnClickListener {
            openRequestFeatureBottomSheet()
            recordClickEvent(ConstantEventNames.OPEN_REQUEST_FEATURE)
        }

        binding?.switchAppLock?.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                dataStoreManager.write(DataStoreManager.APP_LOCK_ENABLED, isChecked)
                recordClickEvent(ConstantEventNames.VIEW_PROFILE_NAME, hashMapOf(Pair("app_lock_enabled", isChecked)))
            }
        }

        binding?.clAppBackup?.setOnClickListener {
            handleAppBackup()
            recordClickEvent(ConstantEventNames.VIEW_PROFILE_NAME, hashMapOf(Pair("action", "app_backup")))
        }

        binding?.clManageTeams?.setOnClickListener {
            fragmentNavigator.start(com.laborbook.keep.screen.teams.TeamListFragment.newInstance())
        }

        binding?.clLogout?.setOnClickListener {
            fragmentNavigator.start(LogoutBottomSheetFragment.newInstance())
            recordClickEvent(ConstantEventNames.VIEW_PROFILE_NAME, hashMapOf(Pair("action", "logout")))
        }

        binding?.clLaborbookPro?.setOnClickListener {
            lifecycleScope.launch {
                // Check if subscriptions feature is enabled via Remote Config
                val remoteConfig = Firebase.remoteConfig
                if (!SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
                    // Feature is disabled - don't show anything
                    return@launch
                }
                
                // Check if user is PRO or FREE
                val isPro = premiumOfferManager.isPremiumUser()
                
                if (isPro) {
                    // User is PRO - Open Pro Settings
                    fragmentNavigator.start(com.laborbook.keep.screen.premium.PremiumSettingsFragment.newInstance())
                    recordClickEvent(ConstantEventNames.VIEW_LABORBOOK_PRO, hashMapOf(Pair("user_type", "pro")))
                } else {
                    // User is FREE - Show Pro Offer Dialog
                    val dialog = PremiumOfferDialogFragment.newInstance()
                    dialog.show(parentFragmentManager, PremiumOfferDialogFragment.TAG)
                    recordClickEvent(ConstantEventNames.VIEW_LABORBOOK_PRO, hashMapOf(Pair("user_type", "free")))
                }
            }
        }
        
        // Debug options (only in debug builds)
        if (com.laborbook.base.BaseConstants.DEBUG) {
            binding?.clResetCoachMark?.setOnClickListener {
                lifecycleScope.launch {
                    coachMarkManager.resetCoachMark(requireContext())
                    Toast.makeText(requireContext(), "Coach mark reset successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupDebugOptions() {
        // Show debug options only in debug builds
        if (com.laborbook.base.BaseConstants.DEBUG) {
            binding?.tvDebug?.visibility = View.VISIBLE
            binding?.clResetCoachMark?.visibility = View.VISIBLE
        }
    }

    private fun viewModelObserver() {
        viewModel.uiState().observe(viewLifecycleOwner){
            when(it){
                is UserUiState.Loading -> {}
                is UserUiState.RefreshUserNameSuccess -> {
                    lifecycleScope.launch {
                        binding?.tvName?.text = dataStoreManager.read(DataStoreManager.USER_NAME, "").first()
                    }
                }
                is UserUiState.GetUserNameSucess -> {
                    lifecycleScope.launch {
                        binding?.tvName?.text = it.name
                        dataStoreManager.write(DataStoreManager.USER_NAME, it.name)
                    }
                }
                is UserUiState.Error -> {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    // Method to open the app update page in the Play Store
    private fun openAppUpdatePage(context: Context) {
        val appPackageName = "com.laborbook" // Replace with your app package name
        try {
            // Open Play Store app if installed
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (e: ActivityNotFoundException) {
            // Open Play Store in the browser if Play Store app is not available
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }

    // Method to open the app's review page in the Play Store
    private fun openAppReviewPage(context: Context) {
        val appPackageName = "com.laborbook" // Replace with your app package name
        try {
            // Open Play Store app if installed
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName&reviewId=0")))
        } catch (e: ActivityNotFoundException) {
            // Open Play Store in the browser if Play Store app is not available
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName&reviewId=0")))
        }
    }

    private fun openRequestFeatureBottomSheet() {
        fragmentNavigator.start(RequestFeatureBottomSheetFragment.newInstance())
    }

    private fun checkForUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                binding?.clAppUpdate?.show()
                triggerSystemEvent(ConstantEventNames.SHOW_UPDATE_BUTTON)
            } else {
                binding?.clAppUpdate?.hide()
            }
        }
    }

    private fun handleAppBackup() {
        fragmentNavigator.start(AppBackupBottomSheetFragment.newInstance())
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}