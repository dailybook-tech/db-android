# DailyBook UI Automation Tests

This directory contains comprehensive UI automation tests for the DailyBook Android application. The tests cover the complete app flow from login to using all features.

## Test Credentials

- **Phone Number**: `9090909090`
- **OTP**: `4242`

## Test Structure

### 1. LoginTest.kt
Tests the login functionality including:
- Successful login with valid credentials
- Invalid phone number validation
- Invalid OTP validation
- OTP resend functionality

### 2. StaffManagementTest.kt
Tests staff management features including:
- Adding new staff members
- Editing staff information
- Deleting staff members
- Staff search functionality
- Validation for invalid data

### 3. AttendanceTest.kt
Tests attendance and calendar functionality including:
- Marking present attendance
- Marking absent attendance
- Marking half-day attendance
- Calendar navigation
- Overtime marking
- Attendance history
- Attendance statistics

### 4. ReportsTest.kt
Tests reports and PDF generation including:
- Generating staff reports
- PDF generation and sharing
- Month selection in reports
- Report data accuracy
- Navigation between reports
- Empty report handling

### 5. DailyBookUITest.kt
Comprehensive end-to-end test covering all features in sequence:
- Complete login flow
- Language selection
- Staff management
- Attendance marking
- Calendar operations
- Advance payments
- Report generation
- Profile management
- Settings
- Staff deletion
- Logout

## Prerequisites

1. **Android Device/Emulator**: A physical device or emulator running Android API level 24 or higher
2. **ADB Access**: Ensure the device is connected and accessible via ADB
3. **App Installation**: The DailyBook app should be installed on the test device

## Running the Tests

### Option 1: Run All Tests
```bash
# From the project root directory
./gradlew connectedAndroidTest
```

### Option 2: Run Specific Test Class
```bash
# Run only login tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dailybook.LoginTest

# Run only staff management tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dailybook.StaffManagementTest

# Run only attendance tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dailybook.AttendanceTest

# Run only reports tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dailybook.ReportsTest

# Run complete end-to-end test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dailybook.DailyBookUITest
```

### Option 3: Run Specific Test Method
```bash
# Run a specific test method
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dailybook.LoginTest#testSuccessfulLogin
```

### Option 4: Run from Android Studio
1. Open the project in Android Studio
2. Navigate to the test files in `app/src/androidTest/java/com/dailybook/`
3. Right-click on any test class or method
4. Select "Run" or "Debug"

## Test Configuration

### Test Runner
The tests use a custom test runner (`DailyBookTestRunner.kt`) that:
- Initializes the app with proper configuration
- Sets up Koin dependency injection for testing
- Handles app launch and cleanup

### Timeouts
- Default timeout: 10-15 seconds for most operations
- PDF generation timeout: 15 seconds
- App launch timeout: 10 seconds

### Test Data
- Test staff name: "Test Staff"
- Test staff phone: "9876543210"
- Test attendance notes: Various descriptive notes
- Test overtime hours: 2 hours

## Test Dependencies

The tests use the following dependencies (already included in build.gradle):
- `androidx.test.uiautomator:uiautomator` - For UI automation
- `androidx.test.ext:junit` - For JUnit 4 support
- `androidx.test.espresso:espresso-core` - For additional testing utilities

## Troubleshooting

### Common Issues

1. **Element Not Found**
   - Ensure the app is in the correct state before the test
   - Check if the element ID or text has changed
   - Increase timeout values if needed

2. **Test Failures on Different Devices**
   - UI elements may have different IDs on different screen sizes
   - Use text-based selectors when possible
   - Test on multiple device configurations

3. **PDF Generation Failures**
   - Ensure the device has sufficient storage space
   - Check if the app has proper permissions
   - Verify the PDF generation service is working

4. **Login Failures**
   - Verify the test credentials are still valid
   - Check if the login flow has changed
   - Ensure the device has internet connectivity

### Debug Mode

To run tests in debug mode with more verbose output:
```bash
./gradlew connectedAndroidTest --debug
```

### Test Reports

Test results and reports are generated in:
- `app/build/reports/androidTests/connected/`
- `app/build/outputs/androidTest-results/connected/`

## Best Practices

1. **Test Independence**: Each test should be independent and not rely on other tests
2. **Cleanup**: Tests should clean up after themselves
3. **Retry Logic**: Implement retry logic for flaky operations
4. **Error Handling**: Proper error handling and meaningful error messages
5. **Documentation**: Keep test documentation up to date

## Continuous Integration

These tests can be integrated into CI/CD pipelines:
- GitHub Actions
- Jenkins
- GitLab CI
- Firebase Test Lab

Example GitHub Actions workflow:
```yaml
name: UI Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Run UI Tests
        run: ./gradlew connectedAndroidTest
```

## Support

For issues with the tests:
1. Check the troubleshooting section above
2. Review the test logs for specific error messages
3. Verify the app functionality manually
4. Update test selectors if the UI has changed 