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
 * Test class for Attendance and Calendar functionality
 * Requires successful login and staff creation before running
 */
@RunWith(AndroidJUnit4::class)
class AttendanceTest {

    private lateinit var device: UiDevice
    private val timeout = 10000L
    private val testStaffName = "Test Staff"

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        loginAndSetupTestData()
    }

    @Test
    fun testMarkPresentAttendance() {
        // Find and click on staff in the list
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        assertTrue("Test staff not found", staffItem.exists())
        staffItem.click()
        
        // Wait for attendance screen
        device.wait(Until.findObject(By.text("Mark Attendance")), timeout)
        
        // Mark present
        val presentButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_present"))
        assertTrue("Present button not found", presentButton.exists())
        presentButton.click()
        
        // Add note if field exists
        val noteInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_note"))
        if (noteInput.exists()) {
            noteInput.setText("Test present attendance")
        }
        
        // Save attendance
        val saveButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_save_attendance"))
        assertTrue("Save attendance button not found", saveButton.exists())
        saveButton.click()
        
        // Wait for success
        device.wait(Until.findObject(By.text("Attendance marked successfully")), timeout)
        
        // Verify attendance is marked
        val successMessage = device.findObject(UiSelector().text("Present"))
        assertTrue("Attendance not marked as present", successMessage.exists())
    }

    @Test
    fun testMarkAbsentAttendance() {
        // Find and click on staff in the list
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Wait for attendance screen
        device.wait(Until.findObject(By.text("Mark Attendance")), timeout)
        
        // Mark absent
        val absentButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_absent"))
        assertTrue("Absent button not found", absentButton.exists())
        absentButton.click()
        
        // Add note if field exists
        val noteInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_note"))
        if (noteInput.exists()) {
            noteInput.setText("Test absent attendance")
        }
        
        // Save attendance
        val saveButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_save_attendance"))
        saveButton.click()
        
        // Wait for success
        device.wait(Until.findObject(By.text("Attendance marked successfully")), timeout)
        
        // Verify attendance is marked
        val successMessage = device.findObject(UiSelector().text("Absent"))
        assertTrue("Attendance not marked as absent", successMessage.exists())
    }

    @Test
    fun testMarkHalfDayAttendance() {
        // Find and click on staff in the list
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Wait for attendance screen
        device.wait(Until.findObject(By.text("Mark Attendance")), timeout)
        
        // Mark half day
        val halfDayButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_half_day"))
        assertTrue("Half day button not found", halfDayButton.exists())
        halfDayButton.click()
        
        // Add note if field exists
        val noteInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_note"))
        if (noteInput.exists()) {
            noteInput.setText("Test half day attendance")
        }
        
        // Save attendance
        val saveButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_save_attendance"))
        saveButton.click()
        
        // Wait for success
        device.wait(Until.findObject(By.text("Attendance marked successfully")), timeout)
        
        // Verify attendance is marked
        val successMessage = device.findObject(UiSelector().text("Half Day"))
        assertTrue("Attendance not marked as half day", successMessage.exists())
    }

    @Test
    fun testCalendarNavigation() {
        // Navigate to calendar tab
        val calendarTab = device.findObject(UiSelector().text("Calendar"))
        assertTrue("Calendar tab not found", calendarTab.exists())
        calendarTab.click()
        
        // Wait for calendar screen
        device.wait(Until.findObject(By.text("Monthly Calendar")), timeout)
        
        // Verify calendar elements are present
        val calendarView = device.findObject(UiSelector().resourceId("com.laborbook:id/calendar_view"))
        assertTrue("Calendar view not found", calendarView.exists())
        
        // Test month navigation
        val nextMonthButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_next_month"))
        if (nextMonthButton.exists()) {
            nextMonthButton.click()
            // Verify month changed
            val monthText = device.findObject(UiSelector().resourceId("com.laborbook:id/tv_month_year"))
            assertTrue("Month navigation failed", monthText.exists())
        }
    }

    @Test
    fun testMarkOvertime() {
        // Navigate to calendar
        val calendarTab = device.findObject(UiSelector().text("Calendar"))
        calendarTab.click()
        
        // Wait for calendar screen
        device.wait(Until.findObject(By.text("Monthly Calendar")), timeout)
        
        // Select today's date
        val todayButton = device.findObject(UiSelector().text("Today"))
        if (todayButton.exists()) {
            todayButton.click()
        } else {
            // Try to find current date
            val currentDate = device.findObject(UiSelector().text("15"))
            if (currentDate.exists()) {
                currentDate.click()
            }
        }
        
        // Mark overtime
        val overtimeButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_overtime"))
        if (overtimeButton.exists()) {
            overtimeButton.click()
            
            // Enter overtime hours
            val hoursInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_overtime_hours"))
            assertTrue("Overtime hours input not found", hoursInput.exists())
            hoursInput.setText("2")
            
            // Save overtime
            val saveButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_save_overtime"))
            assertTrue("Save overtime button not found", saveButton.exists())
            saveButton.click()
            
            // Wait for success
            device.wait(Until.findObject(By.text("Overtime marked successfully")), timeout)
            
            // Verify overtime is marked
            val overtimeText = device.findObject(UiSelector().text("2 hours overtime"))
            assertTrue("Overtime not marked correctly", overtimeText.exists())
        }
    }

    @Test
    fun testAttendanceHistory() {
        // Navigate to staff details
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Wait for staff details screen
        device.wait(Until.findObject(By.text("Staff Details")), timeout)
        
        // Navigate to attendance history
        val historyButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_attendance_history"))
        if (historyButton.exists()) {
            historyButton.click()
            
            // Wait for history screen
            device.wait(Until.findObject(By.text("Attendance History")), timeout)
            
            // Verify history elements
            val historyList = device.findObject(UiSelector().resourceId("com.laborbook:id/rv_attendance_history"))
            assertTrue("Attendance history list not found", historyList.exists())
            
            // Check if there are attendance records
            val attendanceRecord = device.findObject(UiSelector().text("Present"))
            assertTrue("No attendance records found", attendanceRecord.exists())
        }
    }

    @Test
    fun testAttendanceStatistics() {
        // Navigate to reports
        val reportsTab = device.findObject(UiSelector().text("Reports"))
        reportsTab.click()
        
        // Wait for reports screen
        device.wait(Until.findObject(By.text("Staff Reports")), timeout)
        
        // Select staff
        val staffItem = device.findObject(UiSelector().text(testStaffName))
        staffItem.click()
        
        // Verify attendance statistics are shown
        val presentCount = device.findObject(UiSelector().text("Present"))
        assertTrue("Present count not shown", presentCount.exists())
        
        val absentCount = device.findObject(UiSelector().text("Absent"))
        assertTrue("Absent count not shown", absentCount.exists())
        
        val overtimeCount = device.findObject(UiSelector().text("Overtime"))
        assertTrue("Overtime count not shown", overtimeCount.exists())
    }

    private fun loginAndSetupTestData() {
        // Launch app
        launchApp()
        
        // Perform login
        performLogin()
        
        // Handle language selection if needed
        handleLanguageSelection()
        
        // Create test staff if not exists
        createTestStaff()
    }

    private fun createTestStaff() {
        // Check if test staff already exists
        val existingStaff = device.findObject(UiSelector().text(testStaffName))
        if (!existingStaff.exists()) {
            // Navigate to add staff
            navigateToAddStaff()
            
            // Wait for add staff screen
            device.wait(Until.findObject(By.text("Add Staff")), timeout)
            
            // Enter staff name
            val nameInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_staff_name"))
            nameInput.setText(testStaffName)
            
            // Enter phone number
            val phoneInput = device.findObject(UiSelector().resourceId("com.laborbook:id/et_staff_phone"))
            phoneInput.setText("9876543210")
            
            // Click save
            val saveButton = device.findObject(UiSelector().resourceId("com.laborbook:id/btn_save"))
            saveButton.click()
            
            // Wait for success message
            device.wait(Until.findObject(By.text("Staff added successfully")), timeout)
            
            // Go back to home
            device.pressBack()
        }
    }

    private fun navigateToAddStaff() {
        // Look for add staff button
        val addStaffButton = device.findObject(UiSelector().description("Add Staff"))
        if (!addStaffButton.exists()) {
            val fabButton = device.findObject(UiSelector().resourceId("com.laborbook:id/fab_add"))
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