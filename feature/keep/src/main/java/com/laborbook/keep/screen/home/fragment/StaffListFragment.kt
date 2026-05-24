package com.laborbook.keep.screen.home.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isNotEmpty
import androidx.lifecycle.lifecycleScope
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.laborbook.base.AdUnitConstants
import com.laborbook.base.BaseFragment
import com.laborbook.base.analytics.ConstantEventAttributes
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.base.analytics.ConstantEventSources
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.base.datastore.shouldShowGoogleAds
import com.laborbook.base.hideKeyboard
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.laborbook.base.languagemanager.LanguageBottomSheetFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.laborbook.keep.databinding.FragmentStaffListBinding
import com.laborbook.keep.screen.addstaff.fragment.AddStaffContactsFragment
import com.laborbook.keep.screen.calendar.utils.ObserverUtil
import com.laborbook.keep.screen.home.adapter.StaffUserAdapter
import com.laborbook.keep.utils.SubscriptionsFeatureFlag
import com.laborbook.keep.screen.home.uistate.StaffsUiState
import com.laborbook.keep.screen.home.viewmodel.StaffsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class StaffListFragment : BaseFragment<FragmentStaffListBinding>() {

    override val screenName: String
        get() = ConstantEventNames.LABORS
    private var shouldScrollToTop: Boolean = false
    private lateinit var adapter: StaffUserAdapter
    private val viewModel : StaffsViewModel by viewModel()
    private val observerUtil: ObserverUtil by inject()
    private var wasProPreviously: Boolean = false
    
    private val adUnitId = AdUnitConstants.NativeAds.STAFF_LIST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentStaffListBinding? {
        return FragmentStaffListBinding.inflate(inflater,container,false)
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
        lifecycleScope.launch {
            // Only initialize ads if user is not Pro and ads are enabled
            if (dataStoreManager.shouldShowGoogleAds()) {
                MobileAds.initialize(requireContext())
            }
        }
        viewModelObserver()
        setupView()
        registerOnClickListeners()
        callGetStaffsAPI()
        registerObservers()
        showLanguageBottomSheet()
        triggerInAppReview()
        
        // Observe Pro status changes to remove ads immediately when user upgrades
        observeProStatusChanges()
    }

    /**
     * Observe Pro status changes and reload staff list when user becomes Pro
     */
    private fun observeProStatusChanges() {
        dataStoreManager.read(DataStoreManager.PRO_STATUS, false)
            .onEach { isPro ->
                if (isPro && ::adapter.isInitialized) {
                    // User just became Pro (status changed from false to true)
                    if (!wasProPreviously) {
                        // Remove all ads from adapter immediately
                        adapter.removeAllAds()
                        
                        // Reload staff list to unlock all staff items
                        isRefresh = true
                        callGetStaffsAPI()
                    }
                }
                wasProPreviously = isPro
            }
            .launchIn(lifecycleScope)
    }

    private fun showLanguageBottomSheet() {
        lifecycleScope.launch {
            delay(1000)
            if(!dataStoreManager.read(DataStoreManager.FIRST_TIME_APP_OPEN, false).first()){
                try {
                    fragmentNavigator.start(LanguageBottomSheetFragment.newInstance())
                    dataStoreManager.write(DataStoreManager.FIRST_TIME_APP_OPEN, true)
                } catch (e: Exception){

                }
            }
        }
    }

    private fun triggerInAppReview() {
        try {
            lifecycleScope.launch {
                try {
                    val appOpenCount =
                        dataStoreManager.read(DataStoreManager.APP_OPEN_COUNT, 0).first()
                    if (appOpenCount != 0 && appOpenCount % 3 == 0) {
                        val reviewManager: ReviewManager =
                            ReviewManagerFactory.create(requireContext())
                        val request: Task<ReviewInfo> = reviewManager.requestReviewFlow()
                        request.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                try {
                                    // We got the ReviewInfo object
                                    val reviewInfo = task.result
                                    val flow: Task<Void> =
                                        reviewManager.launchReviewFlow(
                                            requireActivity(),
                                            reviewInfo
                                        )
                                    triggerImpressionEvent(ConstantEventNames.IN_APP_REVIEW)
                                    flow.addOnCompleteListener { _ ->
                                        recordClickEvent(ConstantEventNames.IN_APP_REVIEW_DONE)
                                    }
                                }catch (e: Exception){}
                            } else {
                            }
                        }
                    }
                }catch (e: Exception){}
            }
        }catch (e: Exception){}
    }

    private fun registerObservers() {
        observerUtil.refreshStaffs = {
            if(it){
                isRefresh = true
                callGetStaffsAPI()
                if(shouldScrollToTop){
                    shouldScrollToTop = false
                    lifecycleScope.launch {
                        try {
                            delay(500)
                            if(binding?.rvStaffs?.isNotEmpty() == true) {
                                binding?.rvStaffs?.smoothScrollToPosition(0)
                            }
                        }catch (e: Exception){}
                    }
                }
            }
        }

        observerUtil.addedStaff = {
            shouldScrollToTop = it
        }

        observerUtil.clearSearchText = {
            if(it && binding?.etSearchStaffs?.text?.isNotEmpty() == true) {
                binding?.etSearchStaffs?.setText("")
            }
        }

        observerUtil.showReviewBottomSheet = {
            if(it) {
                lifecycleScope.launch {
                    delay(1000)
                    triggerInAppReview()
                }
            }
        }
    }

    private fun callGetStaffsAPI() = lifecycleScope.launch(Dispatchers.IO) {
        viewModel.getUsers(dataStoreManager.read(DataStoreManager.USER_ID, "").first())
    }

    private fun setupView() {
        adapter = StaffUserAdapter(
            onNavigate = {
            recordClickEvent(ConstantEventNames.VIEW_LABOR_CALENDAR)
            binding?.etSearchStaffs?.setText("")
            activity?.let { binding?.etSearchStaffs?.hideKeyboard(it) }
            },
            adUnitId = adUnitId,
            onLockedStaffClick = {
                // Check if subscriptions feature is enabled via Remote Config
                val remoteConfig = Firebase.remoteConfig
                if (SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
                    // Show premium offer dialog when locked staff is clicked
                    val premiumDialog = com.laborbook.keep.screen.premium.PremiumOfferDialogFragment.newInstance()
                    premiumDialog.show(parentFragmentManager, com.laborbook.keep.screen.premium.PremiumOfferDialogFragment.TAG)
                    recordClickEvent(ConstantEventNames.VIEW_LABOR_CALENDAR, hashMapOf(Pair("blocked_by_subscription", true)))
                }
            }
        )

        binding?.apply {
            rvStaffs.adapter = adapter

            etSearchStaffs.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let {
                        adapter.filter(it.toString())
                    }
                }

                override fun afterTextChanged(s: Editable?) {

                }
            })
        }
    }

    private fun registerOnClickListeners() {
        binding?.apply {
            btnAddStaff.setOnClickListener {
                // Check if user is Pro and staff count before navigating
                lifecycleScope.launch {
                    // Check if subscriptions feature is enabled via Remote Config
                    val remoteConfig = Firebase.remoteConfig
                    val subscriptionsEnabled = SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)
                    val maxStaffCount = SubscriptionsFeatureFlag.getFreeUserMaxStaffCount(remoteConfig)
                    
                    val isPro = dataStoreManager.read(DataStoreManager.PRO_STATUS, false).first()
                    val staffCount = viewModel.getStaffCount()
                    
                    if (subscriptionsEnabled && !isPro && staffCount >= maxStaffCount) {
                        // Free user at or above max staff limit - show premium offer
                        val premiumDialog = com.laborbook.keep.screen.premium.PremiumOfferDialogFragment.newInstance()
                        premiumDialog.show(parentFragmentManager, com.laborbook.keep.screen.premium.PremiumOfferDialogFragment.TAG)
                        recordClickEvent(ConstantEventNames.ADD_LABOR, hashMapOf(Pair("blocked_by_subscription", true)))
                    } else {
                        // Pro user or free user under limit or feature disabled - allow adding
                        fragmentNavigator.start(AddStaffContactsFragment.newInstance())
                        recordClickEvent(ConstantEventNames.ADD_LABOR)
                    }
                }
            }

            tvShare.setOnClickListener {
                ReferFriendBottomSheetFragment.newInstance()
                    .show(parentFragmentManager, ReferFriendBottomSheetFragment.TAG)
                recordClickEvent(ConstantEventNames.REFER_A_FRIEND, hashMapOf(Pair(ConstantEventAttributes.SOURCE, ConstantEventSources.LABOR_LIST)))
            }

            ivProTag.setOnClickListener {
                lifecycleScope.launch {
                    val isPro = dataStoreManager.read(DataStoreManager.PRO_STATUS, false).first()
                    if (isPro) {
                        fragmentNavigator.start(com.laborbook.keep.screen.premium.PremiumSettingsFragment.newInstance())
                    } else {
                        val premiumDialog = com.laborbook.keep.screen.premium.PremiumOfferDialogFragment.newInstance()
                        premiumDialog.show(parentFragmentManager, com.laborbook.keep.screen.premium.PremiumOfferDialogFragment.TAG)
                    }
                    recordClickEvent(ConstantEventNames.VIEW_LABORBOOK_PRO, hashMapOf(
                        Pair("source", "staff_list_pro_tag"),
                        Pair("is_pro_user", isPro)
                    ))
                }
            }

            etSearchStaffs.setOnClickListener {
                recordClickEvent(ConstantEventNames.SEARCH_LABOR)
            }
        }
    }

    private fun viewModelObserver() {
        viewModel.uiState().observe(viewLifecycleOwner){
            when(it){
                is StaffsUiState.Loading -> {
                    if(!isRefresh) {
                        binding?.pb?.show()
                    }
                }
                is StaffsUiState.Success -> {
                    binding?.pb?.hide()
                    if(it.staffs.isNotEmpty()) {
                        isRefresh = false
                        binding?.llAddStaffToolTip?.hide()
                        binding?.ivDownChevron?.hide()
                        binding?.etSearchStaffs?.show()
                        binding?.tvStaffs?.show()
                        binding?.rvStaffs?.show()
                        adapter.submitOriginalList(it.staffs, forceRefreshAds = isRefresh)
                        updateTeamFilterChips()
                    } else {
                        isRefresh = false
                        binding?.llAddStaffToolTip?.show()
                        lifecycleScope.launch {
                            binding?.ivDownChevron?.show()
                            delay(5000)
                            binding?.ivDownChevron?.hide()
                        }
                    }
                }
                is StaffsUiState.Error -> {
                    isRefresh = false
                    binding?.pb?.hide()
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private var selectedTeamId: String? = null // null = All

    private fun updateTeamFilterChips() {
        val teamIds = viewModel.getDistinctTeamIds()
        // Only show the filter row when workers belong to more than one team group
        if (teamIds.size <= 1 && teamIds.firstOrNull() == null) {
            binding?.hsvTeamFilter?.visibility = View.GONE
            return
        }
        binding?.hsvTeamFilter?.visibility = View.VISIBLE
        val container = binding?.llTeamChips ?: return
        container.removeAllViews()

        // "All" chip
        addTeamChip(container, null, getString(com.laborbook.keep.R.string.all_workers), selectedTeamId == null)

        teamIds.filterNotNull().forEach { teamId ->
            addTeamChip(container, teamId, teamId.take(8), selectedTeamId == teamId)
        }
    }

    private fun addTeamChip(container: android.widget.LinearLayout, teamId: String?, label: String, isSelected: Boolean) {
        val chip = TextView(requireContext()).apply {
            text = label
            textSize = 11f
            setPadding(
                resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_10),
                resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_4),
                resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_10),
                resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_4),
            )
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_6) }
            layoutParams = lp
            background = androidx.core.content.ContextCompat.getDrawable(
                requireContext(),
                if (isSelected) com.laborbook.keep.R.drawable.custom_radio_button_bg_selected
                else com.laborbook.keep.R.drawable.custom_radio_button_bg
            )
            setTextColor(androidx.core.content.ContextCompat.getColor(
                requireContext(),
                if (isSelected) android.R.color.white
                else com.laborbook.keep.R.color.custom_radio_text_color
            ))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                selectedTeamId = teamId
                viewModel.filterByTeam(teamId)
                updateTeamFilterChips()
            }
        }
        container.addView(chip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::adapter.isInitialized) {
            adapter.releaseAds()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = StaffListFragment()
    }
}