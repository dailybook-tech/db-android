package com.dailybook.keep.screen.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.dailybook.base.BaseFragment
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.base.analytics.FacebookPaymentEvents
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.keep.R
import com.dailybook.keep.databinding.FragmentPremiumSettingsBinding
import com.dailybook.keep.model.subscription.SubscriptionDetails
import com.dailybook.keep.screen.premium.viewmodel.SubscriptionViewModel
import com.dailybook.keep.utils.SubscriptionsFeatureFlag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*

class PremiumSettingsFragment : BaseFragment<FragmentPremiumSettingsBinding>() {

    override val screenName: String
        get() = ConstantEventNames.PREMIUM_SETTINGS

    private val subscriptionViewModel: SubscriptionViewModel by viewModel()
    
    private var isPremiumDetailsExpanded = true
    private var isFaq1Expanded = false
    private var isFaq2Expanded = false
    private var isFaq3Expanded = false
    private var isFaq4Expanded = false
    
    private var currentSubscription: SubscriptionDetails? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentPremiumSettingsBinding? {
        return FragmentPremiumSettingsBinding.inflate(inflater, container, false)
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
        
        // Check if subscriptions feature is enabled via Remote Config
        lifecycleScope.launch {
            val remoteConfig = Firebase.remoteConfig
            if (!SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)) {
                // Feature is disabled - go back
                fragmentNavigator.goBack()
                return@launch
            }
        }
        
        setupViews()
        registerOnClickListeners()
        observeViewModel()
        loadSubscriptionData()
    }

    private fun setupViews() {
        lifecycleScope.launch {
            binding?.apply {
                // Set user information
                val userName = dataStoreManager.read(DataStoreManager.USER_NAME, "").first()
                tvUserName.text = userName
            }
        }
    }
    
    private fun loadSubscriptionData() {
        lifecycleScope.launch {
            val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
            if (userId.isNotEmpty()) {
                subscriptionViewModel.checkUserSubscriptionStatus(userId)
            }
        }
    }
    
    private fun observeViewModel() {
        subscriptionViewModel.subscriptionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SubscriptionViewModel.SubscriptionState.Loading -> {
                    showLoading(true)
                }
                is SubscriptionViewModel.SubscriptionState.UserSubscriptionLoaded -> {
                    showLoading(false)
                    currentSubscription = state.subscription.subscription
                    updateSubscriptionUI(state.subscription.subscription)
                }
                is SubscriptionViewModel.SubscriptionState.SubscriptionCancelled -> {
                    showLoading(false)
                    // Show success message
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.subscription_cancelled))
                        .setMessage(state.response.message)
                        .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                            dialog.dismiss()
                            // Refresh subscription status
                            lifecycleScope.launch {
                                val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
                                if (userId.isNotEmpty()) {
                                    subscriptionViewModel.checkUserSubscriptionStatus(userId)
                                }
                            }
                        }
                        .show()
                }
                is SubscriptionViewModel.SubscriptionState.Error -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    showLoading(false)
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding?.apply {
            if (show) {
                progressBar.show()
                scrollContent.alpha = 0.5f
            } else {
                progressBar.hide()
                scrollContent.alpha = 1f
            }
        }
    }
    
    private fun updateSubscriptionUI(subscription: SubscriptionDetails?) {
        binding?.apply {
            if (subscription != null) {
                // Format dates
                val startDate = formatDate(subscription.startAt)
                val endDate = formatDate(subscription.endAt)
                
                // Member since
                tvMemberSince.text = getString(R.string.member_since, startDate)
                
                // Purchase date
                tvPurchaseDate.text = startDate
                
                // Next bill date
                tvNextBill.text = endDate
                
                // Amount paid - Use API amount if available, otherwise fallback to plan name
                val amount = if (subscription.amount != null && subscription.amount > 0) {
                    "₹${subscription.amount}"
                } else {
                    // Fallback to hardcoded values based on plan name
                    when {
                        subscription.planName.contains("Monthly", ignoreCase = true) -> "₹79"
                        subscription.planName.contains("Yearly", ignoreCase = true) -> "₹699"
                        else -> "₹79"
                    }
                }
                tvAmountPaid.text = amount
            } else {
                // No active subscription
                tvMemberSince.text = getString(R.string.no_active_subscription)
                tvPurchaseDate.text = "-"
                tvNextBill.text = "-"
                tvAmountPaid.text = "-"
            }
        }
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            // Parse ISO 8601 format: "2025-03-25T00:00:00Z"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)
            
            // Format to: "25 March, 2025"
            val outputFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            // Fallback: try simpler format
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                val outputFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e2: Exception) {
                dateString
            }
        }
    }

    private fun registerOnClickListeners() {
        binding?.apply {
            // Back button
            ivBack.setOnClickListener {
                fragmentNavigator.goBack()
            }

            // Toggle Pro Details
            clPremiumUserInfo.setOnClickListener {
                togglePremiumDetails()
            }

            // FAQ 1 Toggle
            clFaq1.setOnClickListener {
                toggleFaq1()
            }

            // FAQ 2 Toggle
            clFaq2.setOnClickListener {
                toggleFaq2()
            }

            // FAQ 3 Toggle
            clFaq3.setOnClickListener {
                toggleFaq3()
            }

            // FAQ 4 Toggle
            clFaq4.setOnClickListener {
                toggleFaq4()
            }

            // Cancel subscription
            tvCancel.setOnClickListener {
                recordClickEvent("premium_cancel_subscription")
                showCancelConfirmationDialog()
            }
        }
    }

    private fun togglePremiumDetails() {
        binding?.apply {
            isPremiumDetailsExpanded = !isPremiumDetailsExpanded
            if (isPremiumDetailsExpanded) {
                clPremiumDetails.show()
                ivExpand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_up)
                )
            } else {
                clPremiumDetails.hide()
                ivExpand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_down)
                )
            }
        }
    }

    private fun toggleFaq1() {
        binding?.apply {
            isFaq1Expanded = !isFaq1Expanded
            if (isFaq1Expanded) {
                tvFaq1Answer.show()
                ivFaq1Expand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_up)
                )
            } else {
                tvFaq1Answer.hide()
                ivFaq1Expand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_down)
                )
            }
        }
    }

    private fun toggleFaq2() {
        binding?.apply {
            isFaq2Expanded = !isFaq2Expanded
            if (isFaq2Expanded) {
                tvFaq2Answer.show()
                ivFaq2Expand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_up)
                )
            } else {
                tvFaq2Answer.hide()
                ivFaq2Expand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_down)
                )
            }
        }
    }

    private fun toggleFaq3() {
        binding?.apply {
            isFaq3Expanded = !isFaq3Expanded
            if (isFaq3Expanded) {
                tvFaq3Answer.show()
                ivFaq3Expand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_up)
                )
            } else {
                tvFaq3Answer.hide()
                ivFaq3Expand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_down)
                )
            }
        }
    }

    private fun toggleFaq4() {
        binding?.apply {
            isFaq4Expanded = !isFaq4Expanded
            if (isFaq4Expanded) {
                tvFaq4Answer.show()
                ivFaq4Expand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_up)
                )
            } else {
                tvFaq4Answer.hide()
                ivFaq4Expand.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_chevron_down)
                )
            }
        }
    }

    private fun showCancelConfirmationDialog() {
        val endDate = currentSubscription?.endAt?.let { formatDate(it) } 
            ?: getString(R.string.end_of_billing_period)
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.cancel_subscription))
            .setMessage(getString(R.string.cancel_subscription_confirmation, endDate))
            .setPositiveButton(getString(R.string.yes_cancel)) { dialog, _ ->
                cancelSubscription()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.no_keep_subscription)) { dialog, _ ->
                recordClickEvent("premium_cancel_subscription_cancelled")
                dialog.dismiss()
            }
            .show()
    }
    
    private fun cancelSubscription() {
        val subscriptionId = currentSubscription?.id
        if (subscriptionId != null) {
            // Call ViewModel to cancel subscription
            subscriptionViewModel.cancelSubscription(subscriptionId)
            
            recordClickEvent("premium_subscription_cancelled", hashMapOf(
                Pair("subscription_id", subscriptionId),
                Pair("plan_name", currentSubscription?.planName ?: "")
            ))
            FacebookPaymentEvents.logSubscriptionCancelled(requireContext(), currentSubscription?.planName)
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.unable_to_cancel_subscription),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PremiumSettingsFragment()
    }
}
