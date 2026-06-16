package com.dailybook.base.analytics

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import java.math.BigDecimal
import java.util.Currency

/**
 * Logs Facebook (Meta) standard app events for the payment funnel to support
 * ad optimization and ROAS (Return on Ad Spend). All methods are no-op on exception.
 */
object FacebookPaymentEvents {

    private const val EVENT_VIEW_CONTENT = "ViewContent"
    /** Fired when user lands on home screen for the first time (post-login). */
    private const val EVENT_FIRST_TIME_HOME_SCREEN = "FirstTimeHomeScreen"
    private const val EVENT_INITIATE_CHECKOUT = "InitiateCheckout"
    private const val EVENT_ADD_PAYMENT_INFO = "AddPaymentInfo"
    private const val EVENT_START_TRIAL = "StartTrial"
    private const val EVENT_SUBSCRIBE = "Subscribe"
    private const val PARAM_CONTENT_TYPE = "content_type"
    private const val PARAM_CONTENT_NAME = "content_name"
    private const val PARAM_CONTENT_ID = "content_id"
    private const val PARAM_VALUE = "value"
    private const val PARAM_CURRENCY = "currency"
    private const val PARAM_INSTALL_SOURCE = "install_source"
    private const val CONTENT_TYPE_PRODUCT = "product"

    /** Default currency for Meta events when not provided (ISO 4217). */
    private const val DEFAULT_CURRENCY = "INR"

    /**
     * Call when user lands on the home screen for the first time (post-login).
     * Used for Meta attribution and onboarding funnel.
     */
    fun logFirstTimeHomeScreen(context: Context, installSource: String = "organic") {
        try {
            val params = Bundle().apply {
                putString(PARAM_CONTENT_TYPE, "screen")
                putString(PARAM_CONTENT_NAME, "Home")
                putString(PARAM_INSTALL_SOURCE, installSource)
            }
            AppEventsLogger.newLogger(context).logEvent(EVENT_FIRST_TIME_HOME_SCREEN, params)
        } catch (e: Exception) {
            // No-op; do not affect app flow
        }
    }

    /**
     * Call when the premium offer dialog is shown (impression).
     */
    fun logViewContent(context: Context, contentName: String = "DailyBook Pro") {
        try {
            val params = Bundle().apply {
                putString(PARAM_CONTENT_TYPE, CONTENT_TYPE_PRODUCT)
                putString(PARAM_CONTENT_NAME, contentName)
            }
            AppEventsLogger.newLogger(context).logEvent(EVENT_VIEW_CONTENT, params)
        } catch (e: Exception) {
            // No-op; do not affect app flow
        }
    }

    /**
     * Call when user taps "Start Trial" / "Start Pro" (create subscription initiated).
     */
    fun logInitiateCheckout(
        context: Context,
        planId: String?,
        value: Double,
        currency: String,
        installSource: String = "organic"
    ) {
        try {
            val currencyCode = normalizeCurrency(currency)
            val params = Bundle().apply {
                planId?.let { putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, it) }
                putDouble(PARAM_VALUE, value.coerceAtLeast(0.0))
                putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currencyCode)
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, CONTENT_TYPE_PRODUCT)
                putString(PARAM_INSTALL_SOURCE, installSource)
            }
            AppEventsLogger.newLogger(context).logEvent(EVENT_INITIATE_CHECKOUT, value.coerceAtLeast(0.0), params)
        } catch (e: Exception) {
            // No-op
        }
    }

    /**
     * Call when user selects a UPI app (payment method added).
     */
    fun logAddPaymentInfo(context: Context) {
        try {
            val params = Bundle().apply {
                putString(PARAM_CONTENT_TYPE, CONTENT_TYPE_PRODUCT)
            }
            AppEventsLogger.newLogger(context).logEvent(EVENT_ADD_PAYMENT_INFO, params)
        } catch (e: Exception) {
            // No-op
        }
    }

    /**
     * Call when subscription is activated and the plan had a trial.
     */
    fun logStartTrial(
        context: Context,
        value: Double = 0.0,
        currency: String,
        planId: String? = null,
        installSource: String = "organic"
    ) {
        try {
            val currencyCode = normalizeCurrency(currency)
            val params = Bundle().apply {
                putDouble(PARAM_VALUE, value.coerceAtLeast(0.0))
                putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currencyCode)
                planId?.let { putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, it) }
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, CONTENT_TYPE_PRODUCT)
                putString(PARAM_INSTALL_SOURCE, installSource)
            }
            AppEventsLogger.newLogger(context).logEvent(EVENT_START_TRIAL, value.coerceAtLeast(0.0), params)
        } catch (e: Exception) {
            // No-op
        }
    }

    /**
     * Call when subscription is activated and the plan had no trial (direct subscription).
     * Sends valid price and currency so Meta can use the event for ROAS (Diagnostics require this).
     */
    fun logSubscribe(
        context: Context,
        value: Double,
        currency: String,
        planId: String? = null,
        installSource: String = "organic"
    ) {
        try {
            val currencyCode = normalizeCurrency(currency)
            val params = Bundle().apply {
                putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currencyCode)
                putDouble(PARAM_VALUE, value.coerceAtLeast(0.0))
                planId?.let { putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, it) }
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, CONTENT_TYPE_PRODUCT)
                putString(PARAM_INSTALL_SOURCE, installSource)
            }
            AppEventsLogger.newLogger(context).logEvent(EVENT_SUBSCRIBE, value.coerceAtLeast(0.0), params)
        } catch (e: Exception) {
            // No-op
        }
    }

    private fun normalizeCurrency(currency: String?): String {
        val code = currency?.trim()?.uppercase()
        if (!code.isNullOrEmpty() && code.length == 3) return code
        return DEFAULT_CURRENCY
    }

    /**
     * Call on payment success / subscription verified. Primary event for ROAS.
     * Uses the recommended logPurchase API with actual amount charged.
     */
    fun logPurchase(context: Context, amount: Double, currencyCode: String, installSource: String = "organic") {
        try {
            val code = normalizeCurrency(currencyCode)
            val currency = try {
                Currency.getInstance(code)
            } catch (e: Exception) {
                Currency.getInstance(DEFAULT_CURRENCY)
            }
            val params = Bundle().apply {
                putString(PARAM_INSTALL_SOURCE, installSource)
            }
            AppEventsLogger.newLogger(context)
                .logPurchase(BigDecimal.valueOf(amount.coerceAtLeast(0.0)), currency, params)
        } catch (e: Exception) {
            // No-op
        }
    }

    /**
     * Optional: call when user cancels subscription (custom event for funnel analysis).
     */
    fun logSubscriptionCancelled(context: Context, planName: String? = null) {
        try {
            val params = Bundle().apply {
                planName?.let { putString(PARAM_CONTENT_NAME, it) }
            }
            AppEventsLogger.newLogger(context).logEvent("SubscriptionCancelled", params)
        } catch (e: Exception) {
            // No-op
        }
    }
}
