package com.laborbook.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    private val fallbackPackageName = "com.laborbook"

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    companion object {
        private const val PHONE_NUMBER = "9090909090"
        private const val OTP = "4242"
        private const val TIMEOUT = 10000L
    }

    /**
     * A - Full login flow → phone → otp → language → home → add labor
     */
    @Test
    fun testA_LoginAndHomeFlow() {
        val packageName = InstrumentationRegistry.getArguments().getString("targetAppId") ?: fallbackPackageName

        rule.collect(
            packageName = packageName,
            includeInStartupProfile = true,
            maxIterations = 1
        ) {
            // Grant notification permission
            grantNotificationPermission(packageName)

            // Start fresh
            pressHome()
            startActivityAndWait()

            // Login flow
            loginAndWaitForHome(packageName)

            // Done
            device.waitForIdle()
        }
    }

    /**
     * B - Post login flow → open app → home → add labor
     */
    @Test
    fun testB_PostLoginHomeFlow() {
        val packageName = InstrumentationRegistry.getArguments().getString("targetAppId") ?: fallbackPackageName

        rule.collect(
            packageName = packageName,
            includeInStartupProfile = true,
            maxIterations = 1
        ) {
            // Grant notification permission
            grantNotificationPermission(packageName)

            // Start fresh
            pressHome()
            startActivityAndWait()

            // Post login → wait till ADD LABOR
            device.wait(Until.hasObject(By.text("ADD LABOR")), TIMEOUT)

            // Done
            device.waitForIdle()
        }
    }

    /**
     * Enter phone number → otp → language (optional) → home (add labor)
     */
    private fun loginAndWaitForHome(packageName: String) {
        // Enter phone number
        device.wait(Until.hasObject(By.res(packageName, "et_number")), TIMEOUT)
        device.findObject(By.res(packageName, "et_number")).text = PHONE_NUMBER

        // OTP screen
        device.wait(Until.hasObject(By.res(packageName, "otp_view")), TIMEOUT)
        device.findObject(By.res(packageName, "otp_view")).text = OTP

        // Language selection (optional)
        device.findObjectOrNull(By.res(packageName, "btnEnglish"))?.click()

        // Wait for Home → ADD LABOR
        device.wait(Until.hasObject(By.text("ADD LABOR")), TIMEOUT)

        device.waitForIdle()
    }

    /**
     * Grant notification permission
     */
    private fun grantNotificationPermission(packageName: String) {
        device.executeShellCommand("pm grant $packageName android.permission.POST_NOTIFICATIONS")
    }

    /**
     * Safe find object or return null
     */
    private fun UiDevice.findObjectOrNull(selector: BySelector): androidx.test.uiautomator.UiObject2? {
        return wait(Until.findObject(selector), 3000)
    }
}