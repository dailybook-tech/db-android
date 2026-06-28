package co.dailybook.base.ads

/**
 * Interface for providing custom ad configuration
 */
interface CustomAdProvider {
    fun getCustomAdData(): CustomAdData
}

/**
 * Manager class to handle custom ad configuration
 * Uses a provider pattern to get data from the app module
 */
class CustomAdManager(private val provider: CustomAdProvider? = null) {
    
    /**
     * Get custom ad data synchronously (for immediate use)
     */
    fun getCustomAdDataSync(): CustomAdData {
        return provider?.getCustomAdData() ?: CustomAdData(
            isEnabled = false,
            imageUrl = "",
            redirectUrl = "",
            title = ""
        )
    }
}
