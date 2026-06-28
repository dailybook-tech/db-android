package co.dailybook.keep.screen.addstaff.adapter

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
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.base.datastore.shouldShowGoogleAds
import co.dailybook.base.setRandomLightCircleBackground
import co.dailybook.keep.databinding.ItemContactListBinding
import co.dailybook.keep.databinding.ItemNativeAdBinding
import co.dailybook.keep.model.Staff
import co.dailybook.keep.screen.addstaff.model.ContactItem
import co.dailybook.keep.screen.calendar.utils.ObserverUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ContactItemAdapter(
    private val adUnitId: String? = null
) : ListAdapter<ContactListItem, RecyclerView.ViewHolder>(ItemDiffCallback()), KoinComponent {
    private val observerUtil: ObserverUtil by inject()
    private val dataStoreManager: DataStoreManager by inject()
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var originalList: List<ContactItem> = listOf()
    private val nativeAds: MutableMap<String, NativeAd> = mutableMapOf() // Store by adKey instead of position
    private val adPositions: MutableSet<String> = mutableSetOf() // Track ad loading attempts by key
    private val adRetryCount: MutableMap<String, Int> = mutableMapOf() // Track retry attempts for failed ads
    private val adViewStartTime: MutableMap<String, Long> = mutableMapOf() // Track when ad became visible
    private val adLastVisibleTime: MutableMap<String, Long> = mutableMapOf() // Track last time ad was visible
    private val mainHandler = Handler(Looper.getMainLooper())
    private var shouldRefreshAds = false // Flag to force refresh on next update
    
    companion object {
        private const val VIEW_TYPE_CONTACT = 0
        private const val VIEW_TYPE_AD = 1
        private const val MAX_RETRY_ATTEMPTS = 3 // Maximum retry attempts for failed ads
        private const val AD_VIEW_TIME_REFRESH_MS = 60000L // Refresh ad after 60 seconds of visibility
        private const val AD_OFF_SCREEN_REFRESH_MS = 30000L // Refresh if ad was off-screen for 30+ seconds
    }

    class ContactViewHolder(
        private val binding: ItemContactListBinding,
        private val observerUtil: ObserverUtil
    ) : RecyclerView.ViewHolder(binding.root), KoinComponent {

        fun bind(contactItem: ContactItem) {
            binding.tvInitial.setRandomLightCircleBackground()
            binding.tvName.text = contactItem.name
            binding.tvNumber.text = contactItem.mobileNumber
            binding.tvInitial.text = contactItem.name.first().toString()

            binding.itemRoot.setOnClickListener {
                observerUtil.onStaffUserAddedListener?.invoke(Staff(contactItem.name, contactItem.mobileNumber))
            }
        }
    }

    class AdViewHolder(
        private val binding: ItemNativeAdBinding,
        private val adUnitId: String?,
        private val adapter: ContactItemAdapter
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
            is ContactListItem.ContactItemView -> VIEW_TYPE_CONTACT
            is ContactListItem.AdItem -> VIEW_TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CONTACT -> {
                val binding = ItemContactListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ContactViewHolder(binding, observerUtil)
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
            is ContactListItem.ContactItemView -> {
                (holder as ContactViewHolder).bind(item.contactItem)
                
                // Find the original index of this contact in the original list
                val originalIndex = originalList.indexOf(item.contactItem)
                if (originalIndex != -1 && (originalIndex + 1) % 3 == 0) {
                    // Ad should appear after this contact item
                    val adKey = "${originalIndex}_${item.contactItem.id}" // Unique key for this ad position
                    
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
                    
                    if (shouldLoad && adUnitId != null) {
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
            is ContactListItem.AdItem -> {
                // Find which contact this ad belongs to based on position
                val contactIndex = findContactIndexBeforeAd(position)
                if (contactIndex != -1) {
                    val adKey = "${contactIndex}_${originalList[contactIndex].id}"
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
        if (adUnitId == null) return
        
        val adKey = "${originalIndex}_${originalList[originalIndex].id}"
        val targetContact = originalList[originalIndex]
        
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
                        insertAdIntoList(originalIndex, targetContact, nativeAd)
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
    
    private fun insertAdIntoList(originalIndex: Int, targetContact: ContactItem, nativeAd: NativeAd) {
        // Double-check the ad is still stored
        val adKey = "${originalIndex}_${targetContact.id}"
        if (nativeAds[adKey] != nativeAd) {
            nativeAds[adKey] = nativeAd
        }
        
        val currentList = currentList.toMutableList()
        val contactPosition = currentList.indexOfFirst { 
            it is ContactListItem.ContactItemView && it.contactItem == targetContact 
        }
        
        if (contactPosition != -1) {
            val adPosition = contactPosition + 1
            // Only insert if there's no ad already at this position
            if (adPosition <= currentList.size) {
                val existingItem = currentList.getOrNull(adPosition)
                if (existingItem !is ContactListItem.AdItem) {
                    currentList.add(adPosition, ContactListItem.AdItem(adPosition))
                    submitList(currentList)
                }
            }
        }
    }
    
    private fun findContactIndexBeforeAd(adPosition: Int): Int {
        // Find the contact item before this ad position
        val currentList = currentList
        if (adPosition > 0 && adPosition <= currentList.size) {
            for (i in adPosition - 1 downTo 0) {
                val item = currentList.getOrNull(i)
                if (item is ContactListItem.ContactItemView) {
                    return originalList.indexOf(item.contactItem)
                }
            }
        }
        return -1
    }

    fun submitOriginalList(list: List<ContactItem>, forceRefreshAds: Boolean = false) {
        val previousList = originalList
        originalList = list
        shouldRefreshAds = forceRefreshAds
        
        val items = mutableListOf<ContactListItem>()
        
        // If forcing refresh, clear all ads
        if (forceRefreshAds) {
            nativeAds.values.forEach { it.destroy() }
            nativeAds.clear()
            adPositions.clear()
            adRetryCount.clear()
            adViewStartTime.clear()
            adLastVisibleTime.clear()
        }
        
        // Build the list with contact items and preserve existing ads
        list.forEachIndexed { index, contactItem ->
            items.add(ContactListItem.ContactItemView(contactItem))
            
            // Check if an ad should appear after this contact (every 3rd)
            if ((index + 1) % 3 == 0 && index < list.size - 1) {
                val adKey = "${index}_${contactItem.id}"
                
                // Check if we have a loaded ad for this contact
                val existingAd = nativeAds[adKey]
                if (existingAd != null) {
                    // Preserve the ad - insert AdItem and store the ad
                    val adPosition = items.size
                    items.add(ContactListItem.AdItem(adPosition))
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
        
        // Find the contact index from the ad key
        val parts = adKey.split("_")
        if (parts.size == 2) {
            val contactIndex = parts[0].toIntOrNull()
            if (contactIndex != null && contactIndex < originalList.size) {
                // Trigger reload by finding the position in current list
                val currentList = currentList.toMutableList()
                val contactPosition = currentList.indexOfFirst {
                    it is ContactListItem.ContactItemView && originalList.indexOf(it.contactItem) == contactIndex
                }
                if (contactPosition != -1) {
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
        val filteredContactList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter { it.name.contains(query, true) || it.mobileNumber.contains(query) }
        }
        
        val items = mutableListOf<ContactListItem>()
        
        // Build filtered list with contact items and preserve existing ads
        filteredContactList.forEachIndexed { filteredIndex, contactItem ->
            items.add(ContactListItem.ContactItemView(contactItem))
            
            // Find original index of this contact
            val originalIndex = originalList.indexOf(contactItem)
            if (originalIndex != -1 && (originalIndex + 1) % 3 == 0 && originalIndex < originalList.size - 1) {
                val adKey = "${originalIndex}_${contactItem.id}"
                val existingAd = nativeAds[adKey]
                if (existingAd != null) {
                    // Preserve the ad in filtered list
                    val adPosition = items.size
                    items.add(ContactListItem.AdItem(adPosition))
                }
            }
        }
        
        submitList(items)
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
        val itemsWithoutAds = currentList.filter { it !is ContactListItem.AdItem }
        submitList(itemsWithoutAds)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ContactListItem>() {
        override fun areItemsTheSame(oldItem: ContactListItem, newItem: ContactListItem): Boolean {
            return when {
                oldItem is ContactListItem.ContactItemView && newItem is ContactListItem.ContactItemView -> {
                    oldItem.contactItem.mobileNumber == newItem.contactItem.mobileNumber
                }
                oldItem is ContactListItem.AdItem && newItem is ContactListItem.AdItem -> {
                    // Ads are considered the same if they have the same position
                    oldItem.adPosition == newItem.adPosition
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ContactListItem, newItem: ContactListItem): Boolean {
            return oldItem == newItem
        }
    }
}
