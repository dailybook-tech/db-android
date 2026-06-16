# DailyBook Android

Android app for tracking daily activities, attendance, income, and expenses.

## Features

- **Auth** — OTP login, Truecaller one-tap, biometric unlock
- **Attendance** — Mark and track daily attendance
- **Income** — Manage income records
- **Expense** — Track expenses
- **Keep** — Core bookkeeping module
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
  └── keep/           # Core bookkeeping
```

Shared libraries are included locally under `boilerplate/`.

## Setup

1. Clone the repo:
   ```bash
   git clone git@github.com:DailyBook/dailybook-android.git
   ```

2. Add `google-services.json` to the `app/` directory.

3. Open in Android Studio and sync Gradle.

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
