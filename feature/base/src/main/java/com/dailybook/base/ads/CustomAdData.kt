package com.dailybook.base.ads

/**
 * Data class to hold custom ad configuration from Firebase Remote Config
 */
data class CustomAdData(
    val isEnabled: Boolean = false,
    val imageUrl: String = "",
    val redirectUrl: String = "",
    val title: String = ""
) {
    /**
     * Check if the custom ad data is valid for display
     */
    fun isValid(): Boolean {
        return isEnabled && imageUrl.isNotEmpty() && redirectUrl.isNotEmpty()
    }
}
