package com.dailybook.base.ads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.dailybook.base.analytics.Analytics

/**
 * Custom ad view that displays an image and handles click events
 */
class CustomAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private var customAdData: CustomAdData? = null
    private var analytics: Analytics? = null

    init {
        setOnClickListener {
            handleAdClick()
        }
    }

    /**
     * Set the analytics instance for tracking events
     */
    fun setAnalytics(analytics: Analytics) {
        this.analytics = analytics
    }

    /**
     * Load and display the custom ad
     */
    fun loadAd(customAdData: CustomAdData) {
        this.customAdData = customAdData
        
        if (customAdData.isValid()) {
            // Load image using Glide
            Glide.with(context)
                .load(customAdData.imageUrl)
                .apply(
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                )
                .into(this)
            
            // Show the view
            visibility = VISIBLE
        } else {
            // Hide the view if ad data is invalid
            visibility = GONE
        }
    }

    /**
     * Hide the custom ad view
     */
    fun hide() {
        visibility = GONE
    }

    /**
     * Show the custom ad view
     */
    fun show() {
        visibility = VISIBLE
    }

    /**
     * Handle ad click event
     */
    private fun handleAdClick() {
        customAdData?.let { adData ->
            if (adData.isValid()) {
                try {
                    // Open the redirect URL
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adData.redirectUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle error if URL cannot be opened
                }
            }
        }
    }
}
