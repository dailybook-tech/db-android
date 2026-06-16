package com.dailybook.keep.screen.home.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.base.datastore.shouldShowGoogleAds
import com.dailybook.base.navigator.FragmentNavigator
import com.dailybook.base.setRandomLightCircleBackground
import com.dailybook.keep.databinding.ItemContactListBinding
import com.dailybook.keep.databinding.ItemNativeAdBinding
import com.dailybook.keep.model.StaffUser
import com.dailybook.keep.screen.calendar.fragment.LaborMonthlyCalendarFragment
import com.dailybook.keep.utils.SubscriptionsFeatureFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StaffUserAdapter(
    val onNavigate: () -> Unit,
    private val adUnitId: String,
    private val onLockedStaffClick: () -> Unit
) : ListAdapter<StaffListItem, RecyclerView.ViewHolder>(ItemDiffCallback()), KoinComponent {

    private var originalList: List<StaffUser> = listOf()
    private val nativeAds: MutableMap<String, NativeAd> = mutableMapOf() // Store by adKey instead of position
    private val adPositions: MutableSet<String> = mutableSetOf() // Track ad loading attempts by key
    private val adRetryCount: MutableMap<String, Int> = mutableMapOf() // Track retry attempts for failed ads
    private val adViewStartTime: MutableMap<String, Long> = mutableMapOf() // Track when ad became visible
    private val adLastVisibleTime: MutableMap<String, Long> = mutableMapOf() // Track last time ad was visible
    private val mainHandler = Handler(Looper.getMainLooper())
    private var shouldRefreshAds = false // Flag to force refresh on next update
    
    // Inject DataStoreManager to check Pro status
    private val dataStoreManager: DataStoreManager by inject()
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val VIEW_TYPE_STAFF = 0
        private const val VIEW_TYPE_AD = 1
        private const val MAX_RETRY_ATTEMPTS = 3 // Maximum retry attempts for failed ads
        private const val AD_VIEW_TIME_REFRESH_MS = 60000L // Refresh ad after 60 seconds of visibility
        private const val AD_OFF_SCREEN_REFRESH_MS = 30000L // Refresh if ad was off-screen for 30+ seconds
    }

    class StaffViewHolder(private val binding: ItemContactListBinding) : RecyclerView.ViewHolder(binding.root), KoinComponent {

        private val fragmentNavigator: FragmentNavigator by inject()

        fun bind(staffUser: StaffUser, isLocked: Boolean, onNavigate: () -> Unit, onLockedClick: () -> Unit) {
            binding.tvInitial.setRandomLightCircleBackground()
            binding.tvName.text = staffUser.name
            binding.tvNumber.text = staffUser.mobileNumber
            binding.tvInitial.text = staffUser.name.first().toString().uppercase()

            if (isLocked) {
                // Show lock icon for locked staff
                binding.ivLock.visibility = ViewGroup.VISIBLE
                
                // Apply opacity to indicate locked state
                binding.itemRoot.alpha = 0.5f
                
                // Enable clicks for locked staff to show premium offer
                binding.itemRoot.isClickable = true
                binding.itemRoot.setOnClickListener {
                    onLockedClick()
                }
            } else {
                // Hide lock icon for unlocked staff
                binding.ivLock.visibility = ViewGroup.GONE
                
                // Normal opacity
                binding.itemRoot.alpha = 1.0f
                
                // Enable clicks for unlocked staff
                binding.itemRoot.isClickable = true
                binding.itemRoot.setOnClickListener {
                    onNavigate()
                    fragmentNavigator.start(LaborMonthlyCalendarFragment.newInstance(staffUser.id, staffUser.mobileNumber))
                }
            }
        }
    }

    class AdViewHolder(
        private val binding: ItemNativeAdBinding,
        private val adUnitId: String,
        private val adapter: StaffUserAdapter
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var currentNativeAd: NativeAd? = null
        private var adKey: String? = null
        private var viewStartTime: Long = 0

        fun bind(nativeAd: NativeAd?, key: String?) {
            val adView = binding.root as NativeAdView
            val currentTime = System.currentTimeMillis()
            
            if (nativeAd == null) {
                // Hide the view but keep the ad reference (it's cached in adapter)
                binding.root.visibility = ViewGroup.GONE
                // Record that ad is no longer visible
                key?.let { adapter.recordAdHidden(it, currentTime) }
                return
            }

            binding.root.visibility = ViewGroup.VISIBLE
            
            // Check if we should refresh based on view time
            key?.let { k ->
                val shouldRefresh = adapter.shouldRefreshAdByViewTime(k, currentTime)
                if (shouldRefresh && currentNativeAd == nativeAd) {
                    // Ad has been visible too long, trigger refresh
                    adapter.refreshAdByKey(k)
                    return
                }
            }

            // If it's the same ad, just update visibility time
            if (currentNativeAd == nativeAd && adKey == key) {
                key?.let { adapter.recordAdVisible(it, currentTime) }
                return
            }
            
            // Record new ad binding
            adKey = key
            currentNativeAd = nativeAd
            viewStartTime = currentTime
            key?.let { adapter.recordAdVisible(it, currentTime) }

            // Bind all native ad views
            adView.mediaView = binding.adMedia
            adView.headlineView = binding.adHeadline
            adView.bodyView = binding.adBody
            adView.callToActionView = binding.adCallToAction
            adView.iconView = binding.adIcon

            // First, set button text and make it visible
            if (!nativeAd.callToAction.isNullOrEmpty()) {
                binding.adCallToAction.text = nativeAd.callToAction
                binding.adCallToAction.visibility = ViewGroup.VISIBLE
            } else {
                binding.adCallToAction.visibility = ViewGroup.GONE
            }

            // Set headline text and make visible if available
            if (!nativeAd.headline.isNullOrEmpty()) {
                binding.adHeadline.text = nativeAd.headline
                binding.adHeadline.visibility = ViewGroup.VISIBLE
            } else {
                binding.adHeadline.visibility = ViewGroup.GONE
            }

            // Set body text and make visible if available
            if (!nativeAd.body.isNullOrEmpty()) {
                binding.adBody.text = nativeAd.body
                binding.adBody.visibility = ViewGroup.VISIBLE
            } else {
                binding.adBody.visibility = ViewGroup.GONE
            }

            // Show icon if available, otherwise show media view
            if (nativeAd.icon != null) {
                binding.adIcon.setImageDrawable(nativeAd.icon?.drawable)
                binding.adIcon.visibility = ViewGroup.VISIBLE
                binding.adMedia.visibility = ViewGroup.GONE
            } else if (nativeAd.mediaContent != null) {
                binding.adIcon.visibility = ViewGroup.GONE
                binding.adMedia.visibility = ViewGroup.VISIBLE
            } else {
                binding.adIcon.visibility = ViewGroup.GONE
                binding.adMedia.visibility = ViewGroup.GONE
            }

            // Set the native ad - this will handle click tracking and media content
            adView.setNativeAd(nativeAd)
        }
        
        fun unregisterAd() {
            // Record that ad is no longer visible
            adKey?.let { adapter.recordAdHidden(it, System.currentTimeMillis()) }
            // Don't destroy the NativeAd - it's cached in the adapter and will be reused
            // Just clear the reference so we know this view holder doesn't have an ad bound
            // The NativeAdView will keep the ad registered until a new one is set
            currentNativeAd = null
            adKey = null
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is StaffListItem.StaffItem -> VIEW_TYPE_STAFF
            is StaffListItem.AdItem -> VIEW_TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_STAFF -> {
        val binding = ItemContactListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                StaffViewHolder(binding)
            }
            VIEW_TYPE_AD -> {
                val binding = ItemNativeAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding, adUnitId, this)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is StaffListItem.StaffItem -> {
                (holder as StaffViewHolder).bind(item.staffUser, item.isLocked, onNavigate, onLockedStaffClick)
                
                // Find the original index of this staff in the original list
                val originalIndex = originalList.indexOf(item.staffUser)
                if (originalIndex != -1 && (originalIndex + 1) % 3 == 0) {
                    // Ad should appear after this staff item
                    val adKey = "${originalIndex}_${item.staffUser.id}" // Unique key for this ad position
                    
                    // Load ad if:
                    // 1. We haven't attempted to load it yet, OR
                    // 2. We should refresh ads (list was refreshed), OR
                    // 3. Ad failed but hasn't exceeded max retries
                    val shouldLoad = when {
                        !adPositions.contains(adKey) && !nativeAds.containsKey(adKey) -> true
                        shouldRefreshAds && nativeAds.containsKey(adKey) -> {
                            // Force refresh: destroy old ad and reload
                            nativeAds[adKey]?.destroy()
                            nativeAds.remove(adKey)
                            adPositions.remove(adKey)
                            adRetryCount.remove(adKey)
                            true
                        }
                        !nativeAds.containsKey(adKey) && (adRetryCount[adKey] ?: 0) < MAX_RETRY_ATTEMPTS -> {
                            // Retry failed ad
                            true
                        }
                        else -> false
                    }
                    
                    if (shouldLoad) {
                        // Check if ads should be shown (not Pro and ads enabled) before loading
                        adapterScope.launch {
                            val shouldShowAds = dataStoreManager.shouldShowGoogleAds()
                            if (shouldShowAds) {
                                mainHandler.post {
                                    if (!adPositions.contains(adKey)) {
                                        adPositions.add(adKey)
                                    }
                                    // Reset refresh flag after first use
                                    if (shouldRefreshAds) {
                                        shouldRefreshAds = false
                                    }
                                    val adPosition = position + 1
                                    loadNativeAd(adPosition, originalIndex, holder.itemView.context)
                                }
                            }
                        }
                    }
                }
            }
            is StaffListItem.AdItem -> {
                // Find which staff this ad belongs to based on position
                val staffIndex = findStaffIndexBeforeAd(position)
                if (staffIndex != -1) {
                    val adKey = "${staffIndex}_${originalList[staffIndex].id}"
                    val nativeAd = nativeAds[adKey]
                    (holder as AdViewHolder).bind(nativeAd, adKey)
                } else {
                    (holder as AdViewHolder).bind(null, null)
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is AdViewHolder) {
            // Only unregister the ad from the view, don't destroy it
            // The NativeAd is cached in nativeAds map and will be reused
            holder.unregisterAd()
        }
    }

    private fun loadNativeAd(listPosition: Int, originalIndex: Int, context: Context) {
        val adKey = "${originalIndex}_${originalList[originalIndex].id}"
        val targetStaff = originalList[originalIndex]
        
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                // Double-check Pro status before inserting ad (in case status changed after ad was requested)
                adapterScope.launch {
                    val shouldShowAds = dataStoreManager.shouldShowGoogleAds()
                    if (!shouldShowAds) {
                        // User is Pro - destroy the ad and don't show it
                        nativeAd.destroy()
                        return@launch
                    }
                    
                    // Store ad by key
                    nativeAds[adKey] = nativeAd
                    // Clear retry count on successful load
                    adRetryCount.remove(adKey)
                    
                    // Post to main thread to ensure UI updates happen on main thread
                    mainHandler.post {
                        insertAdIntoList(originalIndex, targetStaff, nativeAd)
                    }
                }
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Increment retry count
                    val retryCount = (adRetryCount[adKey] ?: 0) + 1
                    adRetryCount[adKey] = retryCount
                    
                    // Remove from tracking if max retries exceeded
                    if (retryCount >= MAX_RETRY_ATTEMPTS) {
                        adPositions.remove(adKey)
                        nativeAds.remove(adKey)
                        adRetryCount.remove(adKey)
                    } else {
                        // Remove from adPositions so we can retry, but keep retry count
                        adPositions.remove(adKey)
                    }
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
    
    private fun insertAdIntoList(originalIndex: Int, targetStaff: StaffUser, nativeAd: NativeAd) {
        // Double-check the ad is still stored
        val adKey = "${originalIndex}_${targetStaff.id}"
        if (nativeAds[adKey] != nativeAd) {
            nativeAds[adKey] = nativeAd
        }
        
        val currentList = currentList.toMutableList()
        val staffPosition = currentList.indexOfFirst { 
            it is StaffListItem.StaffItem && it.staffUser == targetStaff 
        }
        
        if (staffPosition != -1) {
            val adPosition = staffPosition + 1
            // Only insert if there's no ad already at this position
            if (adPosition <= currentList.size) {
                val existingItem = currentList.getOrNull(adPosition)
                if (existingItem !is StaffListItem.AdItem) {
                    currentList.add(adPosition, StaffListItem.AdItem(adPosition))
                    submitList(currentList)
                }
            }
        }
    }
    
    private fun findStaffIndexBeforeAd(adPosition: Int): Int {
        // Find the staff item before this ad position
        val currentList = currentList
        if (adPosition > 0 && adPosition <= currentList.size) {
            for (i in adPosition - 1 downTo 0) {
                val item = currentList.getOrNull(i)
                if (item is StaffListItem.StaffItem) {
                    return originalList.indexOf(item.staffUser)
                }
            }
        }
        return -1
    }

    fun submitOriginalList(list: List<StaffUser>, forceRefreshAds: Boolean = false) {
        val previousList = originalList
        originalList = list
        shouldRefreshAds = forceRefreshAds
        
        val items = mutableListOf<StaffListItem>()
        
        // If forcing refresh, clear all ads
        if (forceRefreshAds) {
            nativeAds.values.forEach { it.destroy() }
            nativeAds.clear()
            adPositions.clear()
            adRetryCount.clear()
            adViewStartTime.clear()
            adLastVisibleTime.clear()
        }
        
        // Check if user is Pro to determine if staff should be locked
        adapterScope.launch {
            val isPro = dataStoreManager.read(DataStoreManager.PRO_STATUS, false).first()
            val remoteConfig = Firebase.remoteConfig
            val subscriptionsEnabled = SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)
            val maxStaffCount = SubscriptionsFeatureFlag.getFreeUserMaxStaffCount(remoteConfig)
            
            mainHandler.post {
                // Build the list with staff items and preserve existing ads
                list.forEachIndexed { index, staffUser ->
                    // All existing staff are always accessible; paywall only blocks adding new staff
                    items.add(StaffListItem.StaffItem(staffUser, isLocked = false))
                    
                    // Check if an ad should appear after this staff (every 3rd)
                    if ((index + 1) % 3 == 0 && index < list.size - 1) {
                        val adKey = "${index}_${staffUser.id}"
                        
                        // Check if we have a loaded ad for this staff
                        val existingAd = nativeAds[adKey]
                        if (existingAd != null) {
                            // Preserve the ad - insert AdItem and store the ad
                            val adPosition = items.size
                            items.add(StaffListItem.AdItem(adPosition))
                            // Keep the adKey in adPositions so we don't reload
                            if (!adPositions.contains(adKey)) {
                                adPositions.add(adKey)
                            }
                        } else if (!adPositions.contains(adKey)) {
                            // No ad loaded yet, but we should try to load one
                            // Don't add placeholder, ad will be inserted when it loads
                        }
                    }
                }
                
                submitList(items)
                
                // Reset refresh flag after processing
                shouldRefreshAds = false
                
                // If list changed significantly, clear invalid ad attempts
                if (previousList.size != list.size) {
                    // Remove ad keys that are no longer valid
                    val validKeys = list.indices.filter { (it + 1) % 3 == 0 && it < list.size - 1 }
                        .map { "${it}_${list[it].id}" }
                        .toSet()
                    adPositions.retainAll(validKeys)
                    nativeAds.keys.retainAll(validKeys)
                    adRetryCount.keys.retainAll(validKeys)
                    adViewStartTime.keys.retainAll(validKeys)
                    adLastVisibleTime.keys.retainAll(validKeys)
                }
            }
        }
    }
    
    fun refreshAds() {
        // Force refresh all ads on next update
        shouldRefreshAds = true
        // Trigger reload by submitting current list
        val currentList = currentList.toMutableList()
        submitList(currentList)
    }
    
    fun refreshAdByKey(adKey: String) {
        // Refresh a specific ad by destroying and reloading
        nativeAds[adKey]?.destroy()
        nativeAds.remove(adKey)
        adPositions.remove(adKey)
        adRetryCount.remove(adKey)
        adViewStartTime.remove(adKey)
        adLastVisibleTime.remove(adKey)
        
        // Find the staff index from the ad key
        val parts = adKey.split("_")
        if (parts.size == 2) {
            val staffIndex = parts[0].toIntOrNull()
            if (staffIndex != null && staffIndex < originalList.size) {
                // Trigger reload by finding the position in current list
                val currentList = currentList.toMutableList()
                val staffPosition = currentList.indexOfFirst {
                    it is StaffListItem.StaffItem && originalList.indexOf(it.staffUser) == staffIndex
                }
                if (staffPosition != -1) {
                    // Reload will happen on next bind
                    submitList(currentList)
                }
            }
        }
    }
    
    fun recordAdVisible(adKey: String, currentTime: Long) {
        if (!adViewStartTime.containsKey(adKey)) {
            adViewStartTime[adKey] = currentTime
        }
        adLastVisibleTime[adKey] = currentTime
    }
    
    fun recordAdHidden(adKey: String, currentTime: Long) {
        adLastVisibleTime[adKey] = currentTime
    }
    
    fun shouldRefreshAdByViewTime(adKey: String, currentTime: Long): Boolean {
        val startTime = adViewStartTime[adKey] ?: return false
        val lastVisibleTime = adLastVisibleTime[adKey] ?: startTime
        
        // Refresh if ad has been visible for more than AD_VIEW_TIME_REFRESH_MS
        if (currentTime - startTime >= AD_VIEW_TIME_REFRESH_MS) {
            return true
        }
        
        // Refresh if ad was off-screen for more than AD_OFF_SCREEN_REFRESH_MS and now visible again
        if (currentTime - lastVisibleTime >= AD_OFF_SCREEN_REFRESH_MS && 
            currentTime - lastVisibleTime < AD_VIEW_TIME_REFRESH_MS) {
            return true
        }
        
        return false
    }

    fun filter(query: String) {
        val filteredStaffList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter { it.name.contains(query, true) || it.mobileNumber.contains(query) }
        }
        
        // Check if user is Pro to determine if staff should be locked
        adapterScope.launch {
            val isPro = dataStoreManager.read(DataStoreManager.PRO_STATUS, false).first()
            val remoteConfig = Firebase.remoteConfig
            val subscriptionsEnabled = SubscriptionsFeatureFlag.isSubscriptionsEnabled(remoteConfig)
            val maxStaffCount = SubscriptionsFeatureFlag.getFreeUserMaxStaffCount(remoteConfig)
            
            mainHandler.post {
                val items = mutableListOf<StaffListItem>()
                
                // Build filtered list with staff items and preserve existing ads
                filteredStaffList.forEachIndexed { filteredIndex, staffUser ->
                    // Find original index of this staff
                    val originalIndex = originalList.indexOf(staffUser)
                    
                    // All existing staff are always accessible; paywall only blocks adding new staff
                    items.add(StaffListItem.StaffItem(staffUser, isLocked = false))
                    
                    if (originalIndex != -1 && (originalIndex + 1) % 3 == 0 && originalIndex < originalList.size - 1) {
                        val adKey = "${originalIndex}_${staffUser.id}"
                        val existingAd = nativeAds[adKey]
                        if (existingAd != null) {
                            // Preserve the ad in filtered list
                            val adPosition = items.size
                            items.add(StaffListItem.AdItem(adPosition))
                        }
                    }
                }
                
                submitList(items)
            }
        }
    }

    fun releaseAds() {
        nativeAds.values.forEach { it.destroy() }
        nativeAds.clear()
        adPositions.clear()
        adRetryCount.clear()
        adViewStartTime.clear()
        adLastVisibleTime.clear()
    }
    
    /**
     * Remove all ads from the list (called when user becomes Pro)
     */
    fun removeAllAds() {
        // Destroy all native ads
        nativeAds.values.forEach { it.destroy() }
        nativeAds.clear()
        adPositions.clear()
        adRetryCount.clear()
        adViewStartTime.clear()
        adLastVisibleTime.clear()
        
        // Remove all AdItem entries from the current list
        val itemsWithoutAds = currentList.filter { it !is StaffListItem.AdItem }
        submitList(itemsWithoutAds)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<StaffListItem>() {
        override fun areItemsTheSame(oldItem: StaffListItem, newItem: StaffListItem): Boolean {
            return when {
                oldItem is StaffListItem.StaffItem && newItem is StaffListItem.StaffItem -> {
                    oldItem.staffUser.mobileNumber == newItem.staffUser.mobileNumber
        }
                oldItem is StaffListItem.AdItem && newItem is StaffListItem.AdItem -> {
                    // Ads are considered the same if they have the same position
                    oldItem.adPosition == newItem.adPosition
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: StaffListItem, newItem: StaffListItem): Boolean {
            return oldItem == newItem
        }
    }
}
