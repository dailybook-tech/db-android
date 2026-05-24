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
 * Test class for Staff Management functionality
 * Requires successful login before running
 */
@RunWith(AndroidJUnit4::class)
class StaffManagementTest {

    private lateinit var device: UiDevice
    private val timeout = 10000L
    private val testStaffName = "Test Staff"
    private val testStaffPhone = "9876543210"

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        loginAndNavigateToHome()
    }

    @Test
    fun testAddNewStaff() {
        // Navigate to add staff
        navigateToAddStaff()
        
        // Wait for add staff screen
        device.wait(Until.findObject(By.text("Add Staff")), timeout)
        
        // Enter staff name
        val nameInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_staff_name"))
        assertTrue("Staff name input not found", nameInput.exists())
        nameInput.setText(testStaffName)
        
        // Enter phone number
        val phoneInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_staff_phone"))
        assertTrue("Staff phone input not found", phoneInput.exists())
        phoneInput.setText(testStaffPhone)
        
        // Click save
        val saveButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_save"))
        assertTrue("Save button not found", saveButton.exists())
        saveButton.click()
        
        // Wait for success message
        device.wait(Until.findObject(By.text("Staff added successfully")), timeout)
        
        // Verify staff appears in list
        val staffInList = device.findObject(UiSelector().text(testStaffName))
        assertTrue("Staff not found in list after adding", staffInList.exists())
    }

    @Test
    fun testAddStaffWithInvalidData() {
        // Navigate to add staff
        navigateToAddStaff()
        
        // Wait for add staff screen
        device.wait(Until.findObject(By.text("Add Staff")), timeout)
        
        // Try to save without entering data
        val saveButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_save"))
        saveButton.click()
        
        // Should show validation error
        val errorMessage = device.findObject(UiSelector().text("Please enter staff name"))
        assertTrue("Validation error not shown", errorMessage.exists())
    }

    @Test
    fun testEditStaff() {
        // First add a staff member
        testAddNewStaff()
        
        // Find and click on staff in the list
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Wait for staff details screen
        device.wait(Until.findObject(By.text("Staff Details")), timeout)
        
        // Click edit button
        val editButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_edit"))
        editButton.click()
        
        // Update staff name
        val nameInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_staff_name"))
        nameInput.setText("Updated Test Staff")
        
        // Save changes
        val saveButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_save"))
        saveButton.click()
        
        // Wait for success message
        device.wait(Until.findObject(By.text("Staff updated successfully")), timeout)
        
        // Verify updated name appears
        val updatedStaff = device.findObject(UiSelector().text("Updated Test Staff"))
        assertTrue("Updated staff name not found", updatedStaff.exists())
    }

    @Test
    fun testDeleteStaff() {
        // First add a staff member
        testAddNewStaff()
        
        // Navigate to staff list
        navigateToStaffList()
        
        // Wait for staff list
        device.wait(Until.findObject(By.text("Staff List")), timeout)
        
        // Long press on staff to show delete option
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.longClick()
        
        // Click delete
        val deleteButton = device.findObject(UiSelector().text("Delete"))
        deleteButton.click()
        
        // Confirm deletion
        val confirmButton = device.findObject(UiSelector().text("Confirm"))
        confirmButton.click()
        
        // Wait for success
        device.wait(Until.findObject(By.text("Staff deleted successfully")), timeout)
        
        // Verify staff is removed from list
        val staffInList = device.findObject(UiSelector().text(testStaffName))
        assertFalse("Staff still found in list after deletion", staffInList.exists())
    }

    @Test
    fun testStaffSearch() {
        // First add a staff member
        testAddNewStaff()
        
        // Navigate to staff list
        navigateToStaffList()
        
        // Find search field
        val searchField = device.findObject(UiSelector().resourceId("com.laborbook:id/et_search"))
        searchField.setText(testStaffName)
        
        // Verify search results
        val searchResult = device.findObject(UiSelector().text(testStaffName))
        assertTrue("Search result not found", searchResult.exists())
        
        // Search for non-existent staff
        searchField.setText("NonExistentStaff")
        
        // Verify no results
        val noResults = device.findObject(UiSelector().text("No staff found"))
        assertTrue("No results message not shown", noResults.exists())
    }

    private fun navigateToAddStaff() {
        // Look for add staff button (FAB or menu item)
        val addStaffButton = device.findObject(UiSelector().description("Add Staff"))
        if (!addStaffButton.exists()) {
            // Try alternative selectors
            val fabButton = device.findObject(UiSelector().resourceId("com.laborbook:id/fab_add"))
            if (fabButton.exists()) {
                fabButton.click()
            } else {
                // Look for text-based button
                val addButton = device.findObject(UiSelector().text("Add Staff"))
                addButton.click()
            }
        } else {
            addStaffButton.click()
        }
    }

    private fun navigateToStaffList() {
        // Look for staff list navigation
        val staffListButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_staff_list"))
        if (!staffListButton.exists()) {
            // Try to find staff list in navigation
            val staffTab = device.findObject(UiSelector().text("Staff"))
            staffTab.click()
        } else {
            staffListButton.click()
        }
    }

    private fun loginAndNavigateToHome() {
        // Launch app
        launchApp()
        
        // Perform login
        performLogin()
        
        // Handle language selection if needed
        handleLanguageSelection()
    }

    private fun performLogin() {
        // Wait for login screen to load
        device.wait(Until.findObject(By.text("Enter Mobile Number")), timeout)
        
        // Enter phone number
        val phoneInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_number"))
        phoneInput.setText("9090909090")
        
        // Click get OTP button
        val getOtpButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_login"))
        getOtpButton.click()
        
        // Wait for OTP screen
        device.wait(Until.findObject(By.text("Verify OTP")), timeout)
        
        // Enter OTP
        val otpView = device.findObject(UiSelector().resourceId("com.laborbook:id/otp_view"))
        otpView.setText("4242")
        
        // Click verify button
        val verifyButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_verify_otp"))
        verifyButton.click()
        
        // Wait for successful login and home screen
        device.wait(Until.findObject(By.text("Home")), timeout)
    }

    private fun handleLanguageSelection() {
        // Check if language selection dialog appears
        val languageDialog = device.findObject(UiSelector().text("Select Language"))
        if (languageDialog.exists()) {
            // Select English
            val englishOption = device.findObject(UiSelector().text("English"))
            englishOption.click()
            
            // Click continue
            val continueButton = device.findObject(UiSelector().text("Continue"))
            continueButton.click()
        }
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