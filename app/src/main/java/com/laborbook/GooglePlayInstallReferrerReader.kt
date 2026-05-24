package com.laborbook

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.laborbook.base.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Reads install source from Google Play Install Referrer API.
 * Parses utm_source from the referrer URL to determine if the install
 * came from a Meta ad (facebook/instagram) or another source.
 * See: https://developer.android.com/google/play/installreferrer
 */
object GooglePlayInstallReferrerReader {

    data class Result(val source: String, val referrerUrl: String?)

    suspend fun getInstallReferrer(context: Context): Result {
        return suspendCancellableCoroutine { continuation ->
            val client = InstallReferrerClient.newBuilder(context).build()

            continuation.invokeOnCancellation {
                try { client.endConnection() } catch (_: Exception) {}
            }

            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    try {
                        when (responseCode) {
                            InstallReferrerClient.InstallReferrerResponse.OK -> {
                                val referrerUrl = client.installReferrer.installReferrer
                                client.endConnection()
                                val source = parseSource(referrerUrl)
                                Logger.d("GooglePlayInstallReferrerReader: referrer=$referrerUrl -> $source")
                                if (continuation.isActive) continuation.resume(Result(source, referrerUrl))
                            }
                            else -> {
                                client.endConnection()
                                Logger.d("GooglePlayInstallReferrerReader: response=$responseCode -> organic")
                                if (continuation.isActive) continuation.resume(Result("organic", null))
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e("GooglePlayInstallReferrerReader: error reading referrer: ${e.message}")
                        if (continuation.isActive) continuation.resume(Result("organic", null))
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Logger.d("GooglePlayInstallReferrerReader: service disconnected -> organic")
                    if (continuation.isActive) continuation.resume(Result("organic", null))
                }
            })
        }
    }

    /**
     * Parses utm_source from a URL-encoded referrer string.
     * Meta ads set utm_source to "facebook" or "instagram".
     * Returns "meta_ads", a raw utm_source value, or "organic".
     */
    private fun parseSource(referrerUrl: String?): String {
        if (referrerUrl.isNullOrBlank()) return "organic"
        val params = referrerUrl.split("&").associate { param ->
            val (key, value) = param.split("=", limit = 2).let {
                it.getOrElse(0) { "" }.trim().lowercase() to
                it.getOrElse(1) { "" }.trim().lowercase()
            }
            key to value
        }
        val utmSource = params["utm_source"] ?: return "organic"
        return when {
            utmSource.contains("facebook") ||
            utmSource.contains("instagram") ||
            utmSource.contains("meta") -> "meta_ads"
            utmSource.isNotEmpty() -> utmSource
            else -> "organic"
        }
    }
}
