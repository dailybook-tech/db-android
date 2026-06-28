package co.dailybook.keep.screen.premium

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri

object UpiAppDetector {

    /**
     * All UPI autopay–supported apps: display name to list of package names (multiple for variants, e.g. Google Pay).
     * Order defines list order in the picker.
     */
    private val ALL_UPI_APPS_ORDERED = listOf(
        "PhonePe" to listOf("com.phonepe.app"),
        "Google Pay" to listOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.google.android.apps.navi.market.activity",
            "com.google.android.apps.walletnfcrel"
        ),
        "Paytm" to listOf("net.one97.paytm"),
        "BHIM UPI" to listOf("in.org.npci.upiapp"),
        "Amazon Pay" to listOf("in.amazon.mShop.android.shopping"),
        "Freecharge" to listOf("com.freecharge.android"),
        "MobiKwik" to listOf("com.mobikwik_new"),
        "Airtel Payments Bank" to listOf("com.myairtelapp")
    )

    /**
     * Returns all UPI apps with installed state. Installed apps are enabled; others are shown but disabled.
     * Use this for the picker so users see all options and can install a missing app.
     *
     * Installed state is determined by: (1) app resolves the UPI pay intent, or (2) package is
     * present on the device. We use 0 (not MATCH_DEFAULT_ONLY) so apps that handle UPI but don't
     * declare DEFAULT category are still shown as installed; we also fallback to package presence
     * for OEMs that don't return the intent in queryIntentActivities.
     */
    fun getAllUpiAppsWithInstalledState(context: Context): List<InstalledUpiApp> {
        val pm = context.packageManager
        val upiIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("upi://pay?pa=test@paytm&pn=Test")
        }
        // Use 0 instead of MATCH_DEFAULT_ONLY so all handlers are included; some UPI apps
        // don't declare DEFAULT category and would otherwise show as not selectable.
        @Suppress("DEPRECATION")
        val resolvingFromIntent = pm.queryIntentActivities(upiIntent, 0)
            .map { it.activityInfo.packageName }
            .toSet()

        return ALL_UPI_APPS_ORDERED.mapIndexed { orderIndex, (displayName, packages) ->
            var installedPackage = packages.firstOrNull { it in resolvingFromIntent }
            if (installedPackage == null) {
                // Fallback: app may be installed but not returned by queryIntentActivities
                // (e.g. OEM quirks, or intent not declared with DEFAULT category).
                installedPackage = packages.firstOrNull { isPackageInstalled(pm, it) }
            }
            val isInstalled = installedPackage != null
            val packageName = installedPackage ?: packages.first()
            val icon: Drawable? = if (isInstalled) {
                try {
                    pm.getApplicationIcon(packageName)
                } catch (e: Exception) {
                    null
                }
            } else null
            val appLabel = if (isInstalled) {
                try {
                    pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
                } catch (e: Exception) {
                    displayName
                }
            } else displayName

            InstalledUpiApp(
                packageName = packageName,
                displayName = displayName,
                appLabel = appLabel,
                icon = icon,
                isInstalled = isInstalled
            )
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns only installed UPI apps (for flows that must have an installed app).
     */
    fun getInstalledUpiApps(context: Context): List<InstalledUpiApp> {
        return getAllUpiAppsWithInstalledState(context).filter { it.isInstalled }
    }
    
    /**
     * Get UPI ID suffix for app
     * Note: These are the actual banking partner handles used by these apps
     */
    fun getUpiSuffix(packageName: String): String {
        return when {
            packageName == "com.phonepe.app" -> "ybl" // Yes Bank Limited (PhonePe's banking partner)
            packageName.contains("google") -> "okhdfcbank" // Any Google Pay variant (also uses okaxis, okicici)
            packageName == "net.one97.paytm" -> "paytm" // Paytm Payments Bank
            packageName == "in.org.npci.upiapp" -> "upi" // BHIM UPI generic
            packageName.contains("amazon") -> "apl" // Amazon Pay
            packageName.contains("freecharge") -> "freecharge" // Freecharge
            packageName.contains("mobikwik") -> "mobikwik" // MobiKwik
            packageName.contains("airtel") -> "airtel" // Airtel Payments Bank
            else -> "ybl" // Default fallback (most common)
        }
    }
}

data class InstalledUpiApp(
    val packageName: String,
    val displayName: String,
    val appLabel: String,
    val icon: Drawable?,
    val isInstalled: Boolean = true
)
