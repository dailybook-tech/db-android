package com.laborbook.base

/**
 * Centralized AdMob Ad Unit IDs
 * 
 * Using separate ad units per page allows for:
 * - Better analytics and performance tracking per page
 * - Independent optimization and A/B testing
 * - Easier troubleshooting and revenue optimization
 */
object AdUnitConstants {
    
    // Base AdMob Publisher ID
    private const val PUBLISHER_ID = "ca-app-pub-4991346658410627"
    
    /**
     * Native Ad Units - For list views (ads after every 3rd item)
     */
    object NativeAds {
        // Expense list native ads
        const val EXPENSE_LIST = "$PUBLISHER_ID/8368376961"
        
        // Income list native ads
        const val INCOME_LIST = "$PUBLISHER_ID/7055295298"
        
        // Staff list native ads
        const val STAFF_LIST = "$PUBLISHER_ID/9633233465"
        
        // Contacts list native ads
        const val CONTACTS_LIST = "$PUBLISHER_ID/2868638273"
    }
    
    /**
     * Banner Ad Units - For fixed banner placements
     */
    object BannerAds {
        // Home page banner
        const val HOME_PAGE = "$PUBLISHER_ID/7843571317"
        
        // Transaction status pages
        const val TRANSACTION_STATUS_KEEP = "$PUBLISHER_ID/4760367156"
        const val TRANSACTION_STATUS_EXPENSE = "$PUBLISHER_ID/1765482230"
        const val TRANSACTION_STATUS_INCOME = "$PUBLISHER_ID/2079007696"
    }

    /**
     * Interstitial Ad Units
     */
    object InterstitialAds {
        // App open interstitial
        const val APP_OPEN = "$PUBLISHER_ID/1647300108"
    }
}

