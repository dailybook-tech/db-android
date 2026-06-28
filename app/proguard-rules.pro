# ============================================================
# DailyBook ProGuard / R8 Rules
# ============================================================

# Preserve line numbers for Crashlytics stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations used by frameworks
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses,EnclosingMethod

# ===================================
# Kotlin
# ===================================
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ===================================
# Retrofit + OkHttp
# ===================================
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ===================================
# Gson (serialization/deserialization)
# ===================================
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod

# Keep ALL model/request/response data classes (Gson needs field names)
-keep class co.dailybook.*.model.** { *; }
-keep class co.dailybook.*.network.**Api { *; }
-keep class co.dailybook.boilerplate.network.** { *; }

# ===================================
# Koin DI
# ===================================
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# ===================================
# Firebase
# ===================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Crashlytics mapping upload
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# ===================================
# Glide
# ===================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder { *** rewind(); }

# ===================================
# Mixpanel
# ===================================
-dontwarn com.mixpanel.**
-keep class com.mixpanel.** { *; }

# ===================================
# Facebook SDK
# ===================================
-keep class com.facebook.** { *; }
-dontwarn com.facebook.**

# ===================================
# Truecaller SDK
# ===================================
-keep class com.truecaller.** { *; }
-dontwarn com.truecaller.**

# ===================================
# Google Ads (AdMob)
# ===================================
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ===================================
# AndroidX / Material
# ===================================
-keep class androidx.** { *; }
-dontwarn androidx.**
-keep class com.google.android.material.** { *; }

# ===================================
# Room Database
# ===================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ===================================
# DataStore
# ===================================
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# ===================================
# App-specific keeps
# ===================================

# Keep BuildConfig
-keep class co.dailybook.BuildConfig { *; }

# Keep Activities/Fragments referenced in manifests or navigation
-keep class * extends android.app.Activity
-keep class * extends androidx.fragment.app.Fragment

# Keep ViewModels (Koin injects by class reference)
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep custom views (referenced from XML)
-keep class co.dailybook.boilerplate.uikit.views.** { *; }

# Keep BroadcastReceivers (registered in manifest)
-keep class * extends android.content.BroadcastReceiver

# Keep Services
-keep class * extends android.app.Service

# Subscription models (Gson)
-keep class co.dailybook.keep.model.subscription.** { *; }

# Auth models (Gson)
-keep class co.dailybook.auth.model.** { *; }

# ===================================
# Suppress common warnings
# ===================================
-dontwarn com.squareup.picasso.**
-dontwarn com.squareup.okhttp.**
-dontwarn com.moat.**
-dontwarn com.iab.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
