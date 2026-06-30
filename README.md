# Dailybook Android

Android app for managing worker attendance, staff tracking, and payroll for businesses.

## Features

- **Auth** — OTP login, Truecaller one-tap, Google Sign-In, biometric unlock
- **Attendance** — Mark and track daily worker attendance
- **Income** — Manage worker wages and income records
- **Expense** — Track business expenses
- **Keep** — Notes and reminders
- **Subscriptions** — Razorpay-powered subscription plans (FREE / PRO tiers)
- **In-app Updates** — Google Play in-app update flow

## Tech Stack

- Kotlin 2.0, AGP 8.6.1
- Architecture: Multi-module (feature modules + shared boilerplate libraries)
- DI: Koin 3.0
- Networking: Retrofit 2.9 + OkHttp 4.12 (via `boilerplate:network`)
- Local DB: Room 2.6
- DataStore: Preferences DataStore
- Analytics: Mixpanel, Firebase Analytics
- Crash Reporting: Firebase Crashlytics
- Performance: Firebase Performance
- Push: Firebase Messaging
- Ads: InMobi, Google Mobile Ads
- Attribution: AppsFlyer, Meta Install Referrer, Google Install Referrer
- Image Loading: Glide 4.16
- UI: Material 1.12, SplashScreen, Biometric

## Module Structure

```
app/                  # Application module
feature/
  ├── auth/           # Login, OTP, Truecaller
  ├── base/           # Shared feature utilities
  ├── expense/        # Expense tracking
  ├── income/         # Income management
  └── keep/           # Notes
```

Shared libraries are consumed from [boilerplate-android](https://github.com/Labourbook/boilerplate-android) via GitHub Packages.

## Setup

1. Clone the repo:
   ```bash
   git clone git@github.com:Labourbook/dailybook-android.git
   ```

2. Add GitHub Packages credentials to `~/.gradle/gradle.properties`:
   ```
   gpr.user=YOUR_GITHUB_USERNAME
   gpr.key=YOUR_GITHUB_TOKEN
   ```

3. Add `google-services.json` to the `app/` directory.

4. Open in Android Studio and sync Gradle.

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Requirements

- Min SDK: 24
- Target SDK: 35
- Android Studio Ladybug or later
