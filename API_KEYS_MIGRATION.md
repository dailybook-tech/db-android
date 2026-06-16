# API Keys & Service Credentials Migration

This document lists all API keys, secrets, and service credentials in the DailyBook project and their migration status.

---

## 1. Firebase

| Item | Value | Location |
|------|-------|----------|
| Firebase API Key | `AIzaSyCarGRA4VqToPYmjM4OUHszQvXdK-Usjtw` | `app/google-services.json` |
| Firebase Project Number | `767874889550` | `app/google-services.json` |
| Firebase Project ID | `dailybook-14` | `app/google-services.json` |
| Firebase Storage Bucket | `dailybook-14.firebasestorage.app` | `app/google-services.json` |
| Firebase Mobile SDK App ID | `1:767874889550:android:70db16499294005411501f` | `app/google-services.json` |

**Services used:** Analytics, Crashlytics, Cloud Messaging (FCM), Remote Config, Storage

**Status:** MIGRATED — New `google-services.json` with `dailybook-14` project and `com.dailybook` package in place. Build passes.

---

## 2. Google AdMob

| Item | Value | Location |
|------|-------|----------|
| AdMob App ID (Test) | `ca-app-pub-3940256099942544~3347511713` | `app/src/main/AndroidManifest.xml` |
| Banner (Test) | `ca-app-pub-3940256099942544/9214589741` | `AdUnitConstants.kt`, layout XMLs |
| Native (Test) | `ca-app-pub-3940256099942544/2247696110` | `AdUnitConstants.kt` |
| Interstitial (Test) | `ca-app-pub-3940256099942544/1033173712` | `AdUnitConstants.kt` |

**Status:** MIGRATED — Using Google-provided test ad unit IDs. Replace with production IDs when new AdMob account is set up.

---

## 3. InMobi (Ad Mediation)

**Status:** REMOVED — InMobi SDK and all references removed from the project.

---

## 4. Razorpay (Payments)

**Status:** DISABLED — Razorpay SDK dependency removed from `feature/keep/build.gradle`. `RAZORPAY_KEY_ID` BuildConfig field removed. All Razorpay code is **commented out** (not deleted) with `// RAZORPAY DISABLED:` markers for easy restoration. Proguard rules, manifest activity registration, and checkout theme cleaned up. Subscription UI (plans, UPI app selection) still renders but payment initiation shows a "Payments are currently disabled" toast.

**To re-enable:** Add `implementation 'com.razorpay:customui:X.Y.Z'` back to `feature/keep/build.gradle`, restore `RAZORPAY_KEY_ID` BuildConfig field, uncomment all `// RAZORPAY DISABLED:` blocks in `BookKeepActivity.kt` and `PremiumOfferDialogFragment.kt`.

---

## 5. Mixpanel (Analytics)

| Item | Value | Location |
|------|-------|----------|
| Mixpanel Token | `47eacbaeea3ca1a4e030dba59a5e8017` | `feature/base/src/main/java/com/dailybook/base/BaseConstants.kt` |

**Status:** MIGRATED

---

## 6. Facebook SDK (Analytics / Attribution)

| Item | Value | Location |
|------|-------|----------|
| Facebook App ID | `1985990635613774` | `app/src/main/res/values/strings.xml` |
| Facebook App Secret | `65830fb242a63cfa382ade619586115b` | `app/src/main/res/values/strings.xml` (as `facebook_secret`) |
| Facebook Client Token | `ec8738047f80434550257a829eced173` | `app/src/main/res/values/strings.xml` |

**Referenced in:** `app/src/main/AndroidManifest.xml` (via `@string/facebook_app_id` and `@string/facebook_client_token`)

**Status:** MIGRATED — App ID and Secret updated. Client Token retained from prior config (update if new token is available).

---

## 7. Truecaller SDK (Phone Verification)

| Item | Value | Location |
|------|-------|----------|
| Truecaller Client ID | `6tjrnz50zrypljwxt_o2qz54tcmtxrwhld7nl1y8xfo` | All 7 `strings.xml` locale files |

**Referenced in:** `app/src/main/AndroidManifest.xml` (via `@string/truecaller_client_id`)

**Status:** MIGRATED — Updated across all 7 locale files.

---

## 8. App Signing Keystore

| Item | Value | Location |
|------|-------|----------|
| Keystore File | `dailybook-keystore.jks` | `local.properties` (`KEYSTORE_FILE`) |
| Store Password | (in local.properties) | `local.properties` (`KEYSTORE_PASSWORD`) |
| Key Alias | (in local.properties) | `local.properties` (`KEY_ALIAS`) |
| Key Password | (in local.properties) | `local.properties` (`KEY_PASSWORD`) |

**Status:** MIGRATED — New keystore generated (RSA 2048-bit, 10,000-day validity, `dailybook` alias). Credentials stored in `local.properties` (gitignored), read by `app/build.gradle` at build time. Keystore `.jks` files also gitignored. Old `keystore` file retained but no longer referenced.

---

## 9. Backend API

| Item | Value | Location |
|------|-------|----------|
| Base URL | `https://api.dailybook.co.in/` | `BaseConstants.kt` |
| Sandbox URL | `https://api.dailybook.co.in/` | `BaseConstants.kt` |
| Terms & Conditions | `https://dailybook.co.in/terms-of-service` | `SettingsFragment.kt`, `LoginFragment.kt` |
| Privacy Policy | `https://dailybook.co.in/privacy-policy` | `SettingsFragment.kt`, `LoginFragment.kt` |
| Pricing | `https://dailybook.co.in/pricing` | `SettingsFragment.kt` |

**Status:** MIGRATED

---

## 10. Deep Links, Sharing & Remote Config

| Item | Old Value | New Value | Location |
|------|-----------|-----------|----------|
| Custom ad image URL | `laborbook-14.appspot.com/...` | `dailybook-14.firebasestorage.app/...` | `app/src/main/res/xml/remote_config_defaults.xml` |
| Custom ad redirect URL | Internal test link | `https://play.google.com/store/apps/details?id=com.dailybook` | `remote_config_defaults.xml` |
| Referral share messages | Already `com.dailybook` | No change needed | All 7 locale `strings.xml` in `feature/keep` and `feature/base` |
| Deep links / intent filters | None configured | N/A | N/A |
| `share_image.jpeg` | Generic (no "Laborbook" text) | Replace with new branding (Goal 3) | `feature/base/src/main/res/raw/` |

**Status:** MIGRATED — Firebase Storage URL, Play Store redirect, and share messages all point to DailyBook. Old release artifacts (`app-release.apk`, `.aab`) deleted.

---

## Migration Summary

| Service | Status |
|---------|--------|
| Firebase | MIGRATED |
| AdMob | MIGRATED (test IDs) |
| InMobi | REMOVED |
| Razorpay | DISABLED |
| Mixpanel | MIGRATED |
| Facebook SDK | MIGRATED |
| Truecaller | MIGRATED |
| Keystore | MIGRATED |
| Backend API | MIGRATED |
| Deep links & sharing | MIGRATED |
