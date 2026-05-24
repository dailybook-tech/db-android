# DailyBook Android — Project Documentation

---

## What Is DailyBook?

DailyBook is a mobile bookkeeping app for Indian users who need to track recurring daily activities.
It was originally built for **worker attendance and payroll** (marking present/absent, computing
salaries, paying advances), but its user base has organically expanded to track **any daily habit or
recurring expense** — milk deliveries, utility payments, subscriptions, and more.

**Core value proposition:** A simple, multilingual, offline-capable daily tracker with a built-in
cashbook (income + expenses), PDF reports, and WhatsApp-native sharing — all behind a freemium model
with a Pro subscription tier.

### Target Market

- Small business owners, contractors, and households in India
- Multilingual: English, Hindi, Tamil, Telugu, Kannada, Marathi, Bengali
- Monetized via Google AdMob + InMobi ads (free tier) and Razorpay UPI subscriptions (Pro tier)

---

## Architecture Overview

### Module Structure

```
dailybook-android/
├── app/                          # Application shell: launcher, FCM, alarms, routing, DI bootstrap
│   └── baselineprofile/          # Macro benchmark / baseline profile generation
├── feature/
│   ├── auth/                     # OTP login, Truecaller OAuth, SMS auto-read
│   ├── base/                     # Shared base classes, PDF, DataStore, ads, analytics, language
│   ├── keep/                     # Core module: staff, attendance, calendar, reports, subscriptions, teams
│   ├── expense/                  # Cashbook — expense (debit) transactions
│   └── income/                   # Cashbook — income (credit) transactions
├── boilerplate/
│   ├── network/                  # Retrofit/OkHttp client, token refresh, caching layer
│   ├── navigator/                # Custom multi-stack fragment navigation
│   ├── uikit/                    # Design system: themed buttons, text views, fonts
│   └── analytics/                # Multi-platform analytics manager
├── gradle/libs.versions.toml     # Centralized dependency version catalog
└── build.gradle / settings.gradle
```

### Module Dependency Graph

```
app
 ├── boilerplate: network, navigator, uikit, analytics
 ├── feature: auth, base, keep, expense, income
 └── app:baselineprofile

feature:auth   → base, network, uikit, navigator, analytics
feature:base   → navigator, analytics, uikit
feature:keep   → base, expense, income, network, uikit, navigator, analytics
feature:expense → base, network, uikit, navigator, analytics
feature:income  → base, network, uikit, navigator, analytics
```

### Architecture Patterns

| Layer | Pattern / Library |
|-------|-------------------|
| **Presentation** | MVVM — `BaseViewModel<UiState>` + `LiveData`, Fragments + ViewBinding |
| **Domain** | Use Cases (interface + implementation per feature) |
| **Data** | Repository pattern → NetworkModule → Retrofit API |
| **DI** | Koin 3.0 (one module per feature, bootstrapped in `MainApp`) |
| **Navigation** | Custom `MultipleStackNavigator` — one fragment back-stack per bottom tab |
| **Local storage** | Room (staff + attendance cache), Jetpack DataStore Preferences (session, settings) |
| **Networking** | Retrofit 2.9 + OkHttp 4.12, Flow call adapter, custom `NetworkHandler` caching |
| **UI framework** | Traditional Views — XML layouts + ViewBinding (no Jetpack Compose) |

---

## Tech Stack

| Category | Libraries |
|----------|-----------|
| Language | Kotlin 2.0, JVM target 1.8 |
| Build | AGP 8.6.1, Gradle 8.9, Version Catalog |
| DI | Koin 3.0.2 |
| Network | Retrofit 2.9, OkHttp 4.12, Gson 2.10.1 |
| Database | Room 2.6.1 |
| Preferences | Jetpack DataStore 1.1.1 |
| Async | Kotlin Coroutines 1.8.1 + Flow |
| UI | Material 1.12, SplashScreen, ViewPager2, SwipeRefreshLayout, Biometric, ConstraintLayout |
| Image loading | Glide 4.16 |
| Analytics | Mixpanel 7.3.2, Firebase Analytics |
| Crash reporting | Firebase Crashlytics |
| Performance | Firebase Performance, Baseline Profile |
| Push notifications | Firebase Cloud Messaging |
| Remote config | Firebase Remote Config |
| Ads | Google AdMob 23.4, InMobi 10.8.3 |
| Payments | Razorpay Custom UI 3.9.22 |
| Auth | Truecaller SDK 3.1.0, Google SMS Retriever |
| Attribution | Google Install Referrer, Facebook SDK 18.1.3 |
| PDF | iText7 7.2.5, Android PdfDocument |
| Testing | JUnit 4, Mockito, MockK, Espresso, UiAutomator |
| Logging | Timber 5.0.1 |

---

## App Flow & User Journeys

### Entry Point

```
RoutingActivity (LAUNCHER, splash screen)
  ├── Initialize: Firebase Remote Config, FCM token, attendance alarm, language
  ├── IS_LOGGED_IN == false → LoginActivity
  └── IS_LOGGED_IN == true  → BookKeepActivity
```

### Authentication Flow

1. **LoginFragment** — User enters phone number (country code `+91`)
   - Option A: **Truecaller one-tap** → `POST api/v1/login/truecaller` → session
   - Option B: **OTP** → `POST api/v1/create-otp` → navigate to OtpFragment
2. **OtpFragment** — 4-digit OTP entry with SMS Retriever auto-fill
   - `POST api/v1/verify-otp` → receives `AuthResponse` (token, user)
3. **Post-login** — Stores session in DataStore: `ACCESS_TOKEN`, `USER_ID`, `COMPANY_ID`, etc.
   - Sets network headers: `Authorization: Bearer …`, `x-db-companyID`, `x-db-userID`
   - Identifies user in Mixpanel
   - Navigates to `BookKeepActivity`, finishes auth stack

### Main App (BookKeepActivity)

Three-tab bottom navigation:

| Tab | Root Fragment | Purpose |
|-----|---------------|---------|
| **Staff** | `StaffListFragment` | List of workers/entries, search, team filter, add new |
| **Cashbook** | `CashbookFragment` | ViewPager2: Expense tab + Income tab, month picker, balance |
| **Settings** | `SettingsFragment` | Profile, app lock, teams, premium, language, logout |

### Key Workflows

#### Staff & Attendance
```
StaffListFragment
  → tap worker → LaborMonthlyCalendarFragment (monthly grid)
    → tap day → AttendanceMarkBottomsheetFragment (P/A/Half-day/OT)
    → salary setup → EditProfileBottomsheetFragment
    → pay advance → PayAdvanceBottomsheetFragment
    → share report → ReportFragment (PDF/CSV/WhatsApp)
  → add worker → AddStaffContactsFragment (contact import + manual)
```

#### Cashbook (Income/Expense)
```
CashbookFragment → ViewPager2
  ├── ExpenseFragment → FAB → CashInOutBottomSheetFragment (add debit)
  └── IncomeFragment  → FAB → CashInOutBottomSheetFragment (add credit)
  → tap entry → TransactionDetailsBottomSheetFragment (view/edit/delete)
  → reports → TransactionReportsFragment (PDF + WhatsApp share)
```

#### Premium / Subscriptions
```
Staff limit reached or locked feature
  → PremiumOfferDialogFragment (paywall)
    → select plan → Razorpay UPI payment (via intent or inline)
    → POST api/v1/subscriptions → POST .../verify
    → SubscriptionSuccessDialogFragment
    → PRO_STATUS saved to DataStore → ads hidden, staff unlocked
```

#### App Lock
```
BookKeepActivity.onResume → >30s since last auth? → AppLockActivity
  → BiometricPrompt (device credential fallback)
  → success → return to app
```

---

## API Layer

**Base URL:** `https://api.dailybook.app/` (defined in `BaseConstants.BASE_URL`)

All responses wrapped in `DataResponse<T>` (`{ data: T }`).

### Auth Endpoints (`AuthApi`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `api/v1/create-otp` | Request OTP |
| POST | `api/v1/resend-otp` | Resend OTP |
| POST | `api/v1/verify-otp` | Verify OTP, get token |
| POST | `api/v1/login/truecaller` | Truecaller OAuth login |

### Staff & Attendance Endpoints (`KeepApi`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `api/v1/users?manager_id=` | List staff |
| POST | `api/v1/users` | Bulk add staff |
| POST | `api/v1/user` | Add single staff |
| GET/PUT/DELETE | `api/v1/user/{id}` | Staff CRUD |
| GET | `api/v1/user/{id}/attendances` | Monthly attendance + calendar |
| POST | `api/v1/user/{id}/attendances` | Mark single attendance |
| PATCH | `api/v1/user/{id}/attendance` | Bulk mark attendance |
| PATCH | `api/v1/user/{id}/advance` | Add advance (legacy) |
| POST/GET | `api/v1/user/{id}/advances` | Advance entries CRUD |
| DELETE | `api/v1/advances/{id}` | Delete advance |
| PUT | `api/v1/user/{id}/ot` | Set overtime |
| POST/GET | `api/v1/users/{id}/salaries` | Salary CRUD |
| GET | `api/v1/users/{id}/salaries/current` | Current active salary |
| POST/GET | `api/v1/teams` | Team CRUD |
| PUT/DELETE | `api/v1/teams/{id}` | Team update/delete |
| POST | `api/v1/user/{id}/team` | Assign worker to team |

### Transaction Endpoints (Income & Expense — same API, different `type`)

| Method | Endpoint | Query |
|--------|----------|-------|
| GET | `api/v1/users/{id}/transactions` | `month`, `year`, `page_no`, `type=CREDIT\|DEBIT` |
| GET | `api/v1/users/{id}/transactions/summary` | Same filters |
| POST | `api/v1/users/{id}/transactions` | Create |
| PUT | `api/v1/users/{id}/transactions/{txn_id}` | Update |
| DELETE | `api/v1/users/{id}/transactions/{txn_id}` | Delete |

### Subscription Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `api/v1/subscription-plans` | Available plans |
| GET | `api/v1/users/{id}/subscription` | User's current subscription |
| POST | `api/v1/subscriptions` | Create subscription (Razorpay) |
| POST | `api/v1/subscriptions/verify` | Verify payment |
| POST | `api/v1/subscriptions/cancel` | Cancel subscription |

### Token Refresh (Boilerplate)

| Method | Endpoint | Note |
|--------|----------|------|
| POST | `/atom/external/v1/auth/refresh-token` | Uses separate `api.a4b.io` host; triggered on HTTP 401 |

---

## Data Layer

### Room Databases

#### AppDatabase (v7) — `feature/keep`
| Entity | Table | Purpose |
|--------|-------|---------|
| `StaffUser` | `staffs` | Cached staff list |
| `AttendanceUser` | `attendance_user` | Monthly attendance summary per worker |
| `CalendarItem` | `calendar_item` | Individual day attendance records (capped at 20,000 rows) |

**Caching strategy:** Local-first emit from Room → fetch from API → update Room if changed. Only staff list and attendance use this pattern; transactions are network-only.

#### ContactDatabase (v1) — `feature/keep`
| Entity | Table | Purpose |
|--------|-------|---------|
| `ContactItem` | `contacts` | Temporary cache of device contacts during staff creation |

### DataStore Preferences (`app_datastore`)

**Session:** `ACCESS_TOKEN`, `USER_ID`, `USER_NAME`, `USER_TYPE`, `MOBILE_NUMBER`, `COMPANY_ID`, `IS_LOGGED_IN`

**Settings:** `LANGUAGE_KEY`, `PRIVACY_MODE_ENABLED`, `APP_LOCK_ENABLED`, `FCM_TOKEN`

**Monetization:** `PRO_STATUS`, `PREMIUM_*` fields, `GOOGLE_ADS_ENABLED`, `HOME_PAGE_ADS_ENABLED`, `INTERSTITIAL_*` counters

**Onboarding:** `APP_OPEN_COUNT`, `FIRST_TIME_APP_OPEN`, `HAS_SEEN_HOME_SCREEN`, coach mark flags

**Attribution:** `INSTALL_SOURCE`, `INSTALL_REFERRER_RAW`

---

## Integrations

### Firebase
| Service | Usage |
|---------|-------|
| **Analytics** | Event logging (alongside Mixpanel) |
| **Crashlytics** | Crash reporting |
| **Performance** | Plugin-level performance traces |
| **Cloud Messaging** | Push notifications → `MyFirebaseMessagingService` |
| **Remote Config** | Feature flags: ads toggles, subscription enabled, staff limits, custom ads, interstitial frequency, paywall review carousel |

### Analytics
| Platform | Status |
|----------|--------|
| **Mixpanel** | Primary — initialized in `MainApp`, identified on login |
| **Firebase Analytics** | Secondary — events logged alongside Mixpanel |
| **CleverTap** | Supported in `AnalyticsManagerImpl` but **not initialized** |
| **AppsFlyer** | Supported in `AnalyticsManagerImpl` but **not initialized** |

### Ads & Monetization
| Provider | Ad Types |
|----------|----------|
| **Google AdMob** | Native (in staff/transaction lists), banner (home), interstitial (on calendar exit) |
| **InMobi** | Mediation adapter for AdMob |
| **Firebase Custom Ads** | Remote Config-driven banner with image URL + redirect (fallback/cross-promo) |

Free users see ads; Pro subscribers (`PRO_STATUS == true`) have all ads hidden.

### Payments
- **Razorpay Custom UI 3.9.22** — UPI subscription flow
- UPI app detection (PhonePe, GPay, Paytm, BHIM, Amazon Pay, etc.) via `UpiAppDetector`
- Plan selection → `POST api/v1/subscriptions` → Razorpay payment → `POST .../verify` → success

### Authentication
| Method | Library |
|--------|---------|
| Phone OTP | Custom backend + Google SMS Retriever for auto-read |
| Truecaller | Truecaller SDK 3.1.0 (OAuth + PKCE) |
| App lock | AndroidX Biometric (device credential fallback) |

### Attribution
| Source | Library |
|--------|---------|
| Google Play Install Referrer | `com.android.installreferrer:installreferrer:2.2` |
| Facebook/Meta | Facebook SDK 18.1.3 (app events, install attribution) |

### Export & Sharing
| Feature | Implementation |
|---------|---------------|
| Staff/payslip PDF | iText7 (`PdfGenerator.generateStaffReport()`, `generatePayslip()`) |
| Transaction report PDF | Android `PdfDocument` (screenshot-based) |
| CSV export | Manual CSV generation → `FileProvider` share |
| Screenshot share | Full-page scroll capture → PNG → share intent |
| WhatsApp share | Targeted intent (`com.whatsapp`) with text + image/PDF |

---

## Background Work

| Mechanism | What | When |
|-----------|------|------|
| **AlarmManager** | Daily attendance reminder notification (7:30 PM IST) | Set on app launch; rescheduled on `BOOT_COMPLETED` |
| **FCM Service** | Push notification display + token persistence | On incoming message / token refresh |
| **Coroutines** | Network calls, PDF generation, DataStore I/O | Throughout app lifecycle |

**No WorkManager, JobScheduler, or SyncAdapter** is used.

---

## Koin Dependency Injection

Bootstrapped in `MainApp.onCreate()`:

```kotlin
startKoin {
    androidContext(this@MainApp)
    modules(appModule, authModule, keepModule, expenseModule, incomeModule)
}
```

| Module | Key Provisions |
|--------|---------------|
| `appModule` | `LanguageManager`, `ModuleNavigator`, `FragmentNavigator`, `DataStoreManager`, `Analytics`, `CustomAdManager` |
| `authModule` | `AuthNetworkModule`, `AuthViewModel`, `AuthUseCase`, `AuthRepository` |
| `keepModule` | 10 ViewModels, `KeepUseCase/Repository`, `SubscriptionUseCase`, `PremiumOfferManager`, `CoachMarkManager`, Room DAOs |
| `incomeModule` | 3 ViewModels, 5 UseCases, `TransactionRepository` |
| `expenseModule` | Same structure as income |

Injection in UI: `by inject()` for singletons, `by viewModel()` / `by sharedViewModel()` for ViewModels.

---

## Localization

Supported locales: **en, hi, ta, te, kn, mr, bn**

Managed by `LanguageManager` which persists the choice in DataStore and applies it via `AppCompatDelegate`. Language selection is presented via `LanguageBottomSheetFragment` in Settings (and on first launch).

Bundle language split is **disabled** — all languages ship in every APK/AAB.

---

## Firebase Remote Config Keys

| Key | Default | Purpose |
|-----|---------|---------|
| `google_ads_enabled` | `true` | Master switch for Google ads |
| `home_page_ads_enabled` | `true` | Banner ad on home screen |
| `custom_ad_enabled` | `true` | Firebase-driven custom banner |
| `custom_ad_image_url` | (Firebase Storage URL) | Custom banner image |
| `custom_ad_redirect_url` | (Play Store URL) | Custom banner click target |
| `subscriptions_enabled` | `false` | Show/hide subscription features |
| `interstitial_ads_per_day` | `1` | Max interstitial ads per day |
| `free_user_max_staff_count` | `1` | Staff limit for free users |
| `paywall_reviews_auto_scroll_enabled` | `true` | Auto-scroll paywall reviews |

---

## Build & Release

### Signing
- Keystore at `app/keystore`, alias `dailybook`
- Release builds: `minifyEnabled = false` (ProGuard rules exist but minification is off)

### Build Variants
- **debug** — standard debug build
- **release** — signed with release keystore

### Build Commands
```bash
./gradlew assembleDebug       # Debug APK
./gradlew assembleRelease     # Release APK
./gradlew bundleRelease       # Release AAB for Play Store
./gradlew test                # Unit tests
./gradlew connectedAndroidTest # Instrumented tests
```

### SDK Targets
- **minSdk:** 24 (Android 7.0)
- **targetSdk / compileSdk:** 35
- **Gradle:** 8.9

---

## Testing

### Unit Tests
- `feature/auth` has real tests: `AuthUseCaseTest`, `AuthRepositoryTest`, `AuthViewModelTest`
- All other modules have placeholder `ExampleUnitTest.kt` only

### Instrumented Tests
- `app/src/androidTest/`: `LoginTest`, `StaffManagementTest`, `AttendanceTest`, `ReportsTest`
- Uses UiAutomator; test credentials: phone `9090909090`, OTP `4242`

### Baseline Profile
- `app/baselineprofile/` generates startup profiles via AndroidX Benchmark

---

## Key Files Quick Reference

| What | Path |
|------|------|
| App entry | `app/.../MainApp.kt` |
| Splash/routing | `app/.../RoutingActivity.kt` |
| Main shell | `feature/keep/.../BookKeepActivity.kt` |
| Auth flow | `feature/auth/.../screen/login/` |
| Staff API | `feature/keep/.../network/KeepApi.kt` |
| Transaction API | `feature/income/.../network/TransactionApi.kt` (same for expense) |
| Room DB | `feature/keep/.../database/AppDatabase.kt` |
| DataStore | `feature/base/.../datastore/DataStoreManager.kt` |
| Analytics | `feature/base/.../analytics/Analytics.kt` |
| PDF generator | `feature/base/.../utils/PdfGenerator.kt` |
| Koin modules | `app/.../di/AppModule.kt`, `feature/*/di/*Module.kt` |
| Remote Config defaults | `app/src/main/res/xml/remote_config_defaults.xml` |
| Version catalog | `gradle/libs.versions.toml` |

---

## Active Goals & Roadmap Context

1. **Rename:** All legacy references in package names, resources, and API URLs must be fully migrated to `dailybook`.
2. **UI Transformation:** The app's design and theme will undergo a complete overhaul — similar UX with a fundamentally different UI, ensuring it has its own distinct visual identity.
3. These docs (`claude.md` + `CODING_STANDARDS.md`) serve as the guiding reference for all future work.
