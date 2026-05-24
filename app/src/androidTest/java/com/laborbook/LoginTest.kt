package com.laborbook

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for Login functionality
 * Test credentials: Phone: 9090909090, OTP: 4242
 */
@RunWith(AndroidJUnit4::class)
class LoginTest {

    private lateinit var device: UiDevice
    private val testPhoneNumber = "9090909090"
    private val testOtp = "4242"
    private val timeout = 10000L

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        launchApp()
    }

    @Test
    fun testSuccessfulLogin() {
        // Wait for login screen to load
        device.wait(Until.findObject(By.text("Enter Mobile Number")), timeout)
        
        // Enter phone number
        val phoneInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_number"))
        assertTrue("Phone input field not found", phoneInput.exists())
        phoneInput.setText(testPhoneNumber)
        
        // Click get OTP button
        val getOtpButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_login"))
        assertTrue("Get OTP button not found", getOtpButton.exists())
        getOtpButton.click()
        
        // Wait for OTP screen
        device.wait(Until.findObject(By.text("Verify OTP")), timeout)
        
        // Enter OTP
        val otpView = device.findObject(UiSelector().resourceId("com.laborbook:id/otp_view"))
        assertTrue("OTP view not found", otpView.exists())
        otpView.setText(testOtp)
        
        // Click verify button
        val verifyButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_verify_otp"))
        assertTrue("Verify button not found", verifyButton.exists())
        verifyButton.click()
        
        // Wait for successful login and home screen
        device.wait(Until.findObject(By.text("Home")), timeout)
        
        // Verify we're on the home screen
        val homeText = device.findObject(UiSelector().text("Home"))
        assertTrue("Login failed - not on home screen", homeText.exists())
    }

    @Test
    fun testInvalidPhoneNumber() {
        // Wait for login screen to load
        device.wait(Until.findObject(By.text("Enter Mobile Number")), timeout)
        
        // Enter invalid phone number
        val phoneInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_number"))
        phoneInput.setText("123")
        
        // Click get OTP button
        val getOtpButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_login"))
        getOtpButton.click()
        
        // Should show error message
        val errorMessage = device.findObject(UiSelector().text("Please enter a valid phone number"))
        assertTrue("Error message not shown for invalid phone", errorMessage.exists())
    }

    @Test
    fun testInvalidOtp() {
        // Wait for login screen to load
        device.wait(Until.findObject(By.text("Enter Mobile Number")), timeout)
        
        // Enter valid phone number
        val phoneInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_number"))
        phoneInput.setText(testPhoneNumber)
        
        // Click get OTP button
        val getOtpButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_login"))
        getOtpButton.click()
        
        // Wait for OTP screen
        device.wait(Until.findObject(By.text("Verify OTP")), timeout)
        
        // Enter invalid OTP
        val otpView = device.findObject(UiSelector().resourceId("com.laborbook:id/otp_view"))
        otpView.setText("0000")
        
        // Click verify button
        val verifyButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_verify_otp"))
        verifyButton.click()
        
        // Should show error message
        val errorMessage = device.findObject(UiSelector().text("Invalid OTP"))
        assertTrue("Error message not shown for invalid OTP", errorMessage.exists())
    }

    @Test
    fun testResendOtp() {
        // Wait for login screen to load
        device.wait(Until.findObject(By.text("Enter Mobile Number")), timeout)
        
        // Enter phone number
        val phoneInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_number"))
        phoneInput.setText(testPhoneNumber)
        
        // Click get OTP button
        val getOtpButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_login"))
        getOtpButton.click()
        
        // Wait for OTP screen
        device.wait(Until.findObject(By.text("Verify OTP")), timeout)
        
        // Click resend OTP
        val resendOtp = device.findObject(UiSelector().resourceId("com.laborbook:id/iv_resend_otp"))
        resendOtp.click()
        
        // Should show success message
        val successMessage = device.findObject(UiSelector().text("OTP sent successfully"))
        assertTrue("Resend OTP failed", successMessage.exists())
    }

    private fun launchApp() {
        // Start the app
        device.pressHome()
        val launcherPackage = device.launcherPackageName
        assertNotNull("Launcher package not found", launcherPackage)
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), timeout)
        
        val context = InstrumentationRegistry.getInstrumentation().context
        val packageName = context.packageName
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), timeout)
        
        // Launch the app
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        
        // Wait for app to load
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), timeout)
    }
} 