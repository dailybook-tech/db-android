# Feature: Keep — ProGuard Rules

# Preserve line numbers
-keepattributes SourceFile,LineNumberTable

# Google Ads
-keep public class com.google.android.gms.**
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.ads.identifier.** { *; }

# Skip Picasso/OkHttp/Moat/IAB warnings
-dontwarn com.squareup.picasso.**
-dontwarn com.squareup.okhttp.**
-dontwarn com.moat.**
-dontwarn com.iab.**

# ===================================
# Subscription Models (Gson serialization)
# ===================================
-keep class co.dailybook.keep.model.subscription.** { *; }
-keepclassmembers class co.dailybook.keep.model.subscription.** {
    <init>(...);
    <fields>;
}

# Keep Gson serialized field names
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Subscription API interface (Retrofit)
-keep,allowobfuscation interface co.dailybook.keep.network.KeepApi

# Premium UI (referenced dynamically)
-keep class co.dailybook.keep.screen.premium.PremiumOfferDialogFragment { *; }
-keep class co.dailybook.keep.screen.premium.UpiAppDetector { *; }
-keep class co.dailybook.keep.screen.premium.InstalledUpiApp { *; }
