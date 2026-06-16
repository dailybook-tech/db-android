package com.dailybook.income.screen.home.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.base.datastore.shouldShowGoogleAds
import com.dailybook.income.R
import com.dailybook.income.databinding.ItemIncomeBinding
import com.dailybook.income.databinding.ItemNativeAdBinding
import com.dailybook.income.model.Transaction
import com.dailybook.income.model.TransactionItem
import com.dailybook.income.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TransactionListAdapter(
    private var isPrivacyModeEnabled: Boolean,
    private val onExpenseClick: (Transaction, Int) -> Unit, // Lambda for handling item clicks
    private val onLoadMore: () -> Unit, // Lambda for handling load more pagination
    private val adUnitId: String? = null // Optional ad unit ID
) : ListAdapter<TransactionItem, RecyclerView.ViewHolder>(ExpenseDiffCallback()), KoinComponent {

    private val dataStoreManager: DataStoreManager by inject()
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var originalTransactions: List<Transaction> = listOf()
    private val nativeAds: MutableMap<String, NativeAd> = mutableMapOf() // Store by adKey
    private val adPositions: MutableSet<String> = mutableSetOf() // Track ad loading attempts by key
    private val adRetryCount: MutableMap<String, Int> = mutableMapOf() // Track retry attempts for failed ads
    private val adViewStartTime: MutableMap<String, Long> = mutableMapOf() // Track when ad became visible
    private val adLastVisibleTime: MutableMap<String, Long> = mutableMapOf() // Track last time ad was visible
    private val mainHandler = Handler(Looper.getMainLooper())
    private var shouldRefreshAds = false // Flag to force refresh on next update

    companion object {
        const val VIEW_TYPE_EXPENSE = 0
        const val VIEW_TYPE_AD = 1
        private const val MAX_RETRY_ATTEMPTS = 3 // Maximum retry attempts for failed ads
        private const val AD_VIEW_TIME_REFRESH_MS = 60000L // Refresh ad after 60 seconds of visibility
        private const val AD_OFF_SCREEN_REFRESH_MS = 30000L // Refresh if ad was off-screen for 30+ seconds
    }

    // Function to toggle privacy mode and update all items
    fun togglePrivacyMode(newValue: Boolean) {
        isPrivacyModeEnabled = newValue
        notifyDataSetChanged()
    }

    fun getExpenseItemAt(position: Int): TransactionItem? {
        return if (position in 0 until itemCount) getItem(position) else null
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionItem.TransactionItemView -> VIEW_TYPE_EXPENSE
            is TransactionItem.AdItem -> VIEW_TYPE_AD
            else -> throw IllegalArgumentException("Unsupported item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_EXPENSE -> ExpenseViewHolder(
                ItemIncomeBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onExpenseClick
            )

            VIEW_TYPE_AD -> {
                val binding = ItemNativeAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding, this)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionItem.TransactionItemView -> {
                (holder as ExpenseViewHolder).bind(
                    item.transaction,
                    position,
                    isPrivacyModeEnabled
                )
                
                // Check if we should load an ad after this transaction
                if (adUnitId != null) {
                    val transactionIndex = originalTransactions.indexOf(item.transaction)
                    if (transactionIndex != -1 && (transactionIndex + 1) % 3 == 0) {
                        val adKey = "${transactionIndex}_${item.transaction.id}"
                        
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
                                        loadNativeAd(position + 1, transactionIndex, holder.itemView.context)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is TransactionItem.AdItem -> {
                val transactionIndex = findTransactionIndexBeforeAd(position)
                if (transactionIndex != -1) {
                    val adKey = "${transactionIndex}_${originalTransactions[transactionIndex].id}"
                    val nativeAd = nativeAds[adKey]
                    (holder as AdViewHolder).bind(nativeAd, adKey)
                } else {
                    (holder as AdViewHolder).bind(null, null)
                }
            }
            else -> {
                // DateHeader and other unsupported types are ignored
            }
        }

        // Trigger onLoadMore lambda when reaching the last item
        if (position == itemCount - 1) {
            onLoadMore()
        }
    }
    
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is AdViewHolder) {
            holder.unregisterAd()
        }
    }

    // ViewHolder for Expense Items
    class ExpenseViewHolder(
        private val binding: ItemIncomeBinding,
        private val onExpenseClick: (Transaction, Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction, position: Int, isPrivacyModeEnabled: Boolean) {
            // Extract day number from date string (e.g., "3 Wed" -> "3" or "01" -> "01")
            val dayNumber = extractDayNumber(transaction.dateStr)
            binding.tvIncomeDate.text = dayNumber
            
            // Extract and display day of week (first 3 characters)
            val dayOfWeek = extractDayOfWeek(transaction.date)
            binding.tvIncomeDay.text = dayOfWeek
            
            binding.tvIncomeReason.text = transaction.reason

            if (transaction.type == Constants.CREDIT && transaction.amount > 0) {
                // Toggle visibility and content based on privacy mode
                if (isPrivacyModeEnabled) {
                    // Hide amount text but keep arrow visible
                    binding.tvCashInAmount.visibility = View.GONE
                    binding.ivSecretAmount.visibility = View.VISIBLE
                    // Keep the arrow visible - it's inside llAmountWithArrow
                    binding.llAmountWithArrow.visibility = View.VISIBLE
                } else {
                    binding.tvCashInAmount.text = "₹${transaction.amount}"
                    binding.tvCashInAmount.visibility = View.VISIBLE
                    binding.llAmountWithArrow.visibility = View.VISIBLE
                    binding.ivSecretAmount.visibility = View.GONE
                }
            } else {
                binding.llAmountWithArrow.visibility = View.GONE
                binding.ivSecretAmount.visibility = View.GONE
            }

            // Set click listener for the entire expense item
            binding.container.setOnClickListener {
                onExpenseClick(transaction, position) // Trigger the lambda function with the expense object
            }
        }
        
        private fun extractDayNumber(dateStr: String): String {
            // Extract day number from date string
            // Examples: "3 Wed" -> "3", "01 Mon" -> "01", "15 Tue" -> "15"
            return dateStr.split(" ").firstOrNull() ?: dateStr
        }
        
        private fun extractDayOfWeek(dateString: String): String {
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                val date = sdf.parse(dateString)
                val calendar = java.util.Calendar.getInstance()
                date?.let { calendar.time = it }
                val dayName = calendar.getDisplayName(
                    java.util.Calendar.DAY_OF_WEEK,
                    java.util.Calendar.SHORT,
                    java.util.Locale.getDefault()
                )
                dayName?.take(3) ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }

    
    class AdViewHolder(
        private val binding: ItemNativeAdBinding,
        private val adapter: TransactionListAdapter
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var currentNativeAd: NativeAd? = null
        private var adKey: String? = null

        fun bind(nativeAd: NativeAd?, key: String?) {
            val adView = binding.root as NativeAdView
            val currentTime = System.currentTimeMillis()
            
            if (nativeAd == null) {
                binding.root.visibility = ViewGroup.GONE
                key?.let { adapter.recordAdHidden(it, currentTime) }
                return
            }

            binding.root.visibility = ViewGroup.VISIBLE
            
            // Check if we should refresh based on view time
            key?.let { k ->
                val shouldRefresh = adapter.shouldRefreshAdByViewTime(k, currentTime)
                if (shouldRefresh && currentNativeAd == nativeAd) {
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
            adKey?.let { adapter.recordAdHidden(it, System.currentTimeMillis()) }
            currentNativeAd = null
            adKey = null
        }
    }
    
    private fun loadNativeAd(listPosition: Int, transactionIndex: Int, context: Context) {
        if (adUnitId == null) return
        
        val adKey = "${transactionIndex}_${originalTransactions[transactionIndex].id}"
        val targetTransaction = originalTransactions[transactionIndex]
        
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
                    
                    nativeAds[adKey] = nativeAd
                    // Clear retry count on successful load
                    adRetryCount.remove(adKey)
                    
                    mainHandler.post {
                        insertAdIntoList(transactionIndex, targetTransaction, nativeAd)
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
    
    private fun insertAdIntoList(transactionIndex: Int, targetTransaction: Transaction, nativeAd: NativeAd) {
        val currentList = currentList.toMutableList()
        val transactionPosition = currentList.indexOfFirst { 
            it is TransactionItem.TransactionItemView && it.transaction == targetTransaction 
        }
        
        if (transactionPosition != -1) {
            val adPosition = transactionPosition + 1
            if (adPosition <= currentList.size) {
                val existingItem = currentList.getOrNull(adPosition)
                if (existingItem !is TransactionItem.AdItem) {
                    currentList.add(adPosition, TransactionItem.AdItem(adPosition))
                    submitList(currentList)
                }
            }
        }
    }
    
    private fun findTransactionIndexBeforeAd(adPosition: Int): Int {
        val currentList = currentList
        if (adPosition > 0 && adPosition <= currentList.size) {
            for (i in adPosition - 1 downTo 0) {
                val item = currentList.getOrNull(i)
                if (item is TransactionItem.TransactionItemView) {
                    return originalTransactions.indexOf(item.transaction)
                }
            }
        }
        return -1
    }
    
    fun submitOriginalList(transactions: List<Transaction>, forceRefreshAds: Boolean = false) {
        originalTransactions = transactions
        shouldRefreshAds = forceRefreshAds
        
        // If forcing refresh, clear all ads
        if (forceRefreshAds) {
            nativeAds.values.forEach { it.destroy() }
            nativeAds.clear()
            adPositions.clear()
            adRetryCount.clear()
            adViewStartTime.clear()
            adLastVisibleTime.clear()
        }
        // Build list with transactions, date headers, and existing ads
        // This will be handled by the fragment's existing logic
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
        
        // Find the transaction index from the ad key
        val parts = adKey.split("_")
        if (parts.size == 2) {
            val transactionIndex = parts[0].toIntOrNull()
            if (transactionIndex != null && transactionIndex < originalTransactions.size) {
                // Trigger reload by finding the position in current list
                val currentList = currentList.toMutableList()
                val transactionPosition = currentList.indexOfFirst {
                    it is TransactionItem.TransactionItemView && 
                    originalTransactions.indexOf(it.transaction) == transactionIndex
                }
                if (transactionPosition != -1) {
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
        val itemsWithoutAds = currentList.filter { it !is TransactionItem.AdItem }
        submitList(itemsWithoutAds)
    }
}

// DiffUtil Callback for efficient list updates
class ExpenseDiffCallback : DiffUtil.ItemCallback<TransactionItem>() {
    override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
        return when {
            oldItem is TransactionItem.TransactionItemView && newItem is TransactionItem.TransactionItemView ->
                oldItem.transaction.id == newItem.transaction.id

            oldItem is TransactionItem.AdItem && newItem is TransactionItem.AdItem ->
                oldItem.adPosition == newItem.adPosition

            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
        return oldItem == newItem
    }
}