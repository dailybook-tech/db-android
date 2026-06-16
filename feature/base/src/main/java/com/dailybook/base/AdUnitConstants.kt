package com.dailybook.base

/**
 * Centralized AdMob Ad Unit IDs
 *
 * Currently using Google-provided test ad unit IDs.
 * Replace with production IDs when new AdMob account is set up.
 * See: https://developers.google.com/admob/android/test-ads#sample_ad_units
 */
object AdUnitConstants {

    object NativeAds {
        const val EXPENSE_LIST = "ca-app-pub-3940256099942544/2247696110"
        const val INCOME_LIST = "ca-app-pub-3940256099942544/2247696110"
        const val STAFF_LIST = "ca-app-pub-3940256099942544/2247696110"
        const val CONTACTS_LIST = "ca-app-pub-3940256099942544/2247696110"
    }

    object BannerAds {
        const val HOME_PAGE = "ca-app-pub-3940256099942544/9214589741"
        const val TRANSACTION_STATUS_KEEP = "ca-app-pub-3940256099942544/9214589741"
        const val TRANSACTION_STATUS_EXPENSE = "ca-app-pub-3940256099942544/9214589741"
        const val TRANSACTION_STATUS_INCOME = "ca-app-pub-3940256099942544/9214589741"
    }

    object InterstitialAds {
        const val APP_OPEN = "ca-app-pub-3940256099942544/1033173712"
    }
}
