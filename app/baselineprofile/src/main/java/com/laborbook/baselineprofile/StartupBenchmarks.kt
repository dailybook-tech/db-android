package com.laborbook.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.UiDevice
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val fallbackPackageName = "com.laborbook"
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    companion object {
        private const val PHONE_NUMBER = "9090909090"
        private const val OTP = "4242"
        private const val TIMEOUT = 10000L
    }

    //
    // Login Flow
    //

    @Test
    fun testA_LoginFlow_NoProfile() {
        benchmarkFlow(
            compilationMode = CompilationMode.None(),
            isLoginRequired = true,
            clearAppDataBeforeStart = true
        )
    }

    @Test
    fun testB_LoginFlow_WithBaselineProfile() {
        benchmarkFlow(
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            isLoginRequired = true,
            clearAppDataBeforeStart = true
        )
    }

    //
    // Post Login Flow
    //

    @Test
    fun testC_PostLoginFlow_NoProfile() {
        benchmarkFlow(
            compilationMode = CompilationMode.None(),
            isLoginRequired = false,
            clearAppDataBeforeStart = false
        )
    }

    @Test
    fun testD_PostLoginFlow_WithBaselineProfile() {
        benchmarkFlow(
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            isLoginRequired = false,
            clearAppDataBeforeStart = false
        )
    }

    //
    // Common benchmark logic
    //

    private fun benchmarkFlow(compilationMode: CompilationMode, isLoginRequired: Boolean, clearAppDataBeforeStart: Boolean) {
        val packageName = InstrumentationRegistry.getArguments().getString("targetAppId") ?: fallbackPackageName

        rule.measureRepeated(
            packageName = packageName,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = 1,
            setupBlock = {
                device.grantNotificationPermission(packageName)

                if (clearAppDataBeforeStart) {
                    device.clearAppData(packageName)
                }

                pressHome()
            },
            measureBlock = {
                startActivityAndWait()

                if (isLoginRequired) {
                    device.findObjectOrNull(By.res(packageName, "et_number"))?.text = PHONE_NUMBER
                    device.findObjectOrNull(By.res(packageName, "otp_view"))?.text = OTP
                    device.findObjectOrNull(By.res(packageName, "btnEnglish"))?.click()
                }

                device.wait(Until.hasObject(By.text("ADD LABOR")), TIMEOUT)
                device.waitForIdle()
            }
        )
    }

    //
    // Utilities
    //

    private fun UiDevice.findObjectOrNull(selector: BySelector) =
        wait(Until.findObject(selector), 3000)

    private fun UiDevice.grantNotificationPermission(packageName: String) {
        executeShellCommand("pm grant $packageName android.permission.POST_NOTIFICATIONS")
    }

    private fun UiDevice.clearAppData(packageName: String) {
        executeShellCommand("pm clear $packageName")
    }
}