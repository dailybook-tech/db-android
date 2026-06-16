package com.dailybook

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for Reports and PDF Generation functionality
 * Requires successful login and staff with attendance data before running
 */
@RunWith(AndroidJUnit4::class)
class ReportsTest {

    private lateinit var device: UiDevice
    private val timeout = 15000L
    private val testStaffName = "Test Staff"

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        loginAndSetupTestData()
    }

    @Test
    fun testGenerateStaffReport() {
        // Navigate to reports tab
        val reportsTab = device.findObject(UiSelector().text("Reports"))
        assertTrue("Reports tab not found", reportsTab.exists())
        reportsTab.click()
        
        // Wait for reports screen
        device.wait(Until.findObject(By.text("Staff Reports")), timeout)
        
        // Select staff
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        assertTrue("Test staff not found in reports", staffItem.exists())
        staffItem.click()
        
        // Wait for staff report screen
        device.wait(Until.findObject(By.text("Staff Report")), timeout)
        
        // Verify report elements are present
        val attendanceSummary = device.findObject(UiSelector().text("Attendance Summary"))
        assertTrue("Attendance summary not found", attendanceSummary.exists())
        
        val paymentSummary = device.findObject(UiSelector().text("Payment Summary"))
        assertTrue("Payment summary not found", paymentSummary.exists())
        
        // Check if attendance data is displayed
        val presentCount = device.findObject(UiSelector().text("Present"))
        assertTrue("Present count not shown", presentCount.exists())
        
        val absentCount = device.findObject(UiSelector().text("Absent"))
        assertTrue("Absent count not shown", absentCount.exists())
    }

    @Test
    fun testGeneratePDFReport() {
        // Navigate to reports
        val reportsTab = device.findObject(UiSelector().text("Reports"))
        reportsTab.click()
        
        // Wait for reports screen
        device.wait(Until.findObject(By.text("Staff Reports")), timeout)
        
        // Select staff
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Wait for staff report screen
        device.wait(Until.findObject(By.text("Staff Report")), timeout)
        
        // Generate PDF
        val generateButton = device.findObject(UiSelector().resourceId("com.dailybook:id/btn_share_pdf"))
        assertTrue("Share PDF button not found", generateButton.exists())
        generateButton.click()
        
        // Wait for PDF generation (button text should change to "Generating PDF")
        device.wait(Until.findObject(By.text("Generating PDF")), timeout)
        
        // Wait for PDF generation to complete
        device.wait(Until.findObject(By.text("Share PDF")), timeout)
        
        // Verify PDF was generated successfully
        val successMessage = device.findObject(UiSelector().text("PDF generated successfully"))
        assertTrue("PDF generation failed", successMessage.exists())
    }

    @Test
    fun testSharePDFReport() {
        // Generate PDF first
        testGeneratePDFReport()
        
        // Click share button
        val shareButton = device.findObject(UiSelector().text("Share"))
        assertTrue("Share button not found", shareButton.exists())
        shareButton.click()
        
        // Wait for share dialog
        device.wait(Until.findObject(By.text("Share via")), timeout)
        
        // Verify share options are available
        val shareOptions = device.findObject(UiSelector().text("WhatsApp"))
        assertTrue("Share options not available", shareOptions.exists())
        
        // Cancel share operation
        device.pressBack()
        
        // Verify we're back to the report screen
        device.wait(Until.findObject(By.text("Staff Report")), timeout)
    }

    @Test
    fun testMonthSelectionInReports() {
        // Navigate to reports
        val reportsTab = device.findObject(UiSelector().text("Reports"))
        reportsTab.click()
        
        // Wait for reports screen
        device.wait(Until.findObject(By.text("Staff Reports")), timeout)
        
        // Select staff
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Wait for staff report screen
        device.wait(Until.findObject(By.text("Staff Report")), timeout)
        
        // Select month if selector exists
        val monthSelector = device.findObject(UiSelector().resourceId("com.dailybook:id/btn_month_selector"))
        if (monthSelector.exists()) {
            monthSelector.click()
            
            // Wait for month picker
            device.wait(Until.findObject(By.text("Select Month")), timeout)
            
            // Select a different month
            val previousMonth = device.findObject(UiSelector().text("November"))
            if (previousMonth.exists()) {
                previousMonth.click()
                
                // Verify month changed
                val monthText = device.findObject(UiSelector().resourceId("com.dailybook:id/tv_month_year"))
                assertTrue("Month selection failed", monthText.exists())
            }
        }
    }

    @Test
    fun testReportDataAccuracy() {
        // Navigate to reports
        val reportsTab = device.findObject(UiSelector().text("Reports"))
        reportsTab.click()
        
        // Wait for reports screen
        device.wait(Until.findObject(By.text("Staff Reports")), timeout)
        
        // Select staff
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Wait for staff report screen
        device.wait(Until.findObject(By.text("Staff Report")), timeout)
        
        // Verify staff information is correct
        val staffName = device.findObject(UiSelector().text(testStaffName))
        assertTrue("Staff name not displayed correctly", staffName.exists())
        
        val staffPhone = device.findObject(UiSelector().text("9876543210"))
        assertTrue("Staff phone not displayed correctly", staffPhone.exists())
        
        // Verify attendance data is present
        val attendanceData = device.findObject(UiSelector().text("Present"))
        assertTrue("Attendance data not displayed", attendanceData.exists())
        
        // Verify payment data is present
        val paymentData = device.findObject(UiSelector().text("Total Earnings"))
        assertTrue("Payment data not displayed", paymentData.exists())
    }

    @Test
    fun testReportNavigation() {
        // Navigate to reports
        val reportsTab = device.findObject(UiSelector().text("Reports"))
        reportsTab.click()
        
        // Wait for reports screen
        device.wait(Until.findObject(By.text("Staff Reports")), timeout)
        
        // Test back navigation
        device.pressBack()
        
        // Should be back to main screen
        val homeText = device.findObject(UiSelector().text("Home"))
        assertTrue("Back navigation failed", homeText.exists())
        
        // Navigate back to reports
        val reportsTabAgain = device.findObject(UiSelector().text("Reports"))
        reportsTabAgain.click()
        
        // Select staff and test back navigation from report
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Wait for staff report screen
        device.wait(Until.findObject(By.text("Staff Report")), timeout)
        
        // Go back
        device.pressBack()
        
        // Should be back to staff list
        device.wait(Until.findObject(By.text("Staff Reports")), timeout)
    }

    @Test
    fun testEmptyReportHandling() {
        // Create a new staff member without attendance
        val newStaffName = "Empty Report Staff"
        createStaffWithoutAttendance(newStaffName)
        
        // Navigate to reports
        val reportsTab = device.findObject(UiSelector().text("Reports"))
        reportsTab.click()
        
        // Wait for reports screen
        device.wait(Until.findObject(By.text("Staff Reports")), timeout)
        
        // Select the new staff
        val staffItem = device.findObject(UiSelector().text(newStaffName))
        staffItem.click()
        
        // Wait for staff report screen
        device.wait(Until.findObject(By.text("Staff Report")), timeout)
        
        // Verify empty state is handled properly
        val noDataMessage = device.findObject(UiSelector().text("No attendance data"))
        if (noDataMessage.exists()) {
            assertTrue("Empty state not handled properly", true)
        } else {
            // If no empty state message, verify basic structure is still present
            val attendanceSummary = device.findObject(UiSelector().text("Attendance Summary"))
            assertTrue("Report structure not maintained for empty data", attendanceSummary.exists())
        }
    }

    private fun createStaffWithoutAttendance(staffName: String) {
        // Navigate to add staff
        navigateToAddStaff()
        
        // Wait for add staff screen
        device.wait(Until.findObject(By.text("Add Staff")), timeout)
        
        // Enter staff name
        val nameInput = device.findObject(UiSelector().resourceId("com.dailybook:id/et_staff_name"))
        nameInput.setText(staffName)
        
        // Enter phone number
        val phoneInput = device.findObject(UiSelector().resourceId("com.dailybook:id/et_staff_phone"))
        phoneInput.setText("8765432109")
        
        // Click save
        val saveButton = device.findObject(UiSelector().resourceId("com.dailybook:id/btn_save"))
        saveButton.click()
        
        // Wait for success message
        device.wait(Until.findObject(By.text("Staff added successfully")), timeout)
        
        // Go back to home
        device.pressBack()
    }

    private fun navigateToAddStaff() {
        // Look for add staff button
        val addStaffButton = device.findObject(UiSelector().description("Add Staff"))
        if (!addStaffButton.exists()) {
            val fabButton = device.findObject(UiSelector().resourceId("com.dailybook:id/fab_add"))
            if (fabButton.exists()) {
                fabButton.click()
            } else {
                val addButton = device.findObject(UiSelector().text("Add Staff"))
                addButton.click()
            }
        } else {
            addStaffButton.click()
        }
    }

    private fun loginAndSetupTestData() {
        // Launch app
        launchApp()
        
        // Perform login
        performLogin()
        
        // Handle language selection if needed
        handleLanguageSelection()
        
        // Create test staff and mark attendance
        createTestStaffWithAttendance()
    }

    private fun createTestStaffWithAttendance() {
        // Check if test staff already exists
        val existingStaff = device.findObject(UiSelector().text(testStaffName))
        if (!existingStaff.exists()) {
            // Create staff
            navigateToAddStaff()
            
            // Wait for add staff screen
            device.wait(Until.findObject(By.text("Add Staff")), timeout)
            
            // Enter staff name
            val nameInput = device.findObject(UiSelector().resourceId("com.dailybook:id/et_staff_name"))
            nameInput.setText(testStaffName)
            
            // Enter phone number
            val phoneInput = device.findObject(UiSelector().resourceId("com.dailybook:id/et_staff_phone"))
            phoneInput.setText("9876543210")
            
            // Click save
            val saveButton = device.findObject(UiSelector().resourceId("com.dailybook:id/btn_save"))
            saveButton.click()
            
            // Wait for success message
            device.wait(Until.findObject(By.text("Staff added successfully")), timeout)
            
            // Go back to home
            device.pressBack()
            
            // Mark attendance for the staff
            markAttendanceForStaff()
        }
    }

    private fun markAttendanceForStaff() {
        // Find and click on staff in the list
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Wait for attendance screen
        device.wait(Until.findObject(By.text("Mark Attendance")), timeout)
        
        // Mark present
        val presentButton = device.findObject(UiSelector().resourceId("com.dailybook:id/btn_present"))
        presentButton.click()
        
        // Add note if field exists
        val noteInput = device.findObject(UiSelector().resourceId("com.dailybook:id/et_note"))
        if (noteInput.exists()) {
            noteInput.setText("Test attendance for reports")
        }
        
        // Save attendance
        val saveButton = device.findObject(UiSelector().resourceId("com.dailybook:id/btn_save_attendance"))
        saveButton.click()
        
        // Wait for success
        device.wait(Until.findObject(By.text("Attendance marked successfully")), timeout)
        
        // Go back
        device.pressBack()
    }

    private fun performLogin() {
        // Wait for login screen to load
        device.wait(Until.findObject(By.text("Enter Mobile Number")), timeout)
        
        // Enter phone number
        val phoneInput = device.findObject(UiSelector().resourceId("com.dailybook:id/et_number"))
        phoneInput.setText("9090909090")
        
        // Click get OTP button
        val getOtpButton = device.findObject(UiSelector().resourceId("com.dailybook:id/btn_login"))
        getOtpButton.click()
        
        // Wait for OTP screen
        device.wait(Until.findObject(By.text("Verify OTP")), timeout)
        
        // Enter OTP
        val otpView = device.findObject(UiSelector().resourceId("com.dailybook:id/otp_view"))
        otpView.setText("4242")
        
        // Click verify button
        val verifyButton = device.findObject(UiSelector().resourceId("com.dailybook:id/btn_verify_otp"))
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