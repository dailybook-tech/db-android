# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep class com.dailybook.** {*;}
-keepclassmembers class com.dailybook.** {*;}
-keep class com.boilerplate.** {*;}
-keepclassmembers class com.boilerplate.** {*;}

# Keep BuildConfig
-keep class com.dailybook.BuildConfig {*;}
-keep class com.dailybook.keep.BuildConfig {*;}

# ===================================
# Subscription Models ProGuard Rules
# ===================================
# Keep all subscription model classes and their members
-keep class com.dailybook.keep.model.subscription.** {*;}
-keepclassmembers class com.dailybook.keep.model.subscription.** {*;}

# Explicitly keep all subscription data classes for Gson
-keep class com.dailybook.keep.model.subscription.SubscriptionPlan {*;}
-keep class com.dailybook.keep.model.subscription.MetaData {*;}
-keep class com.dailybook.keep.model.subscription.SubscriptionPlansResponse {*;}
-keep class com.dailybook.keep.model.subscription.CreateSubscriptionRequest {*;}
-keep class com.dailybook.keep.model.subscription.Addon {*;}
-keep class com.dailybook.keep.model.subscription.AddonItem {*;}
-keep class com.dailybook.keep.model.subscription.CreateSubscriptionResponse {*;}
-keep class com.dailybook.keep.model.subscription.CancelSubscriptionResponse {*;}
-keep class com.dailybook.keep.model.subscription.UserSubscription {*;}
-keep class com.dailybook.keep.model.subscription.SubscriptionDetails {*;}
-keep class com.dailybook.keep.model.subscription.Feature {*;}
-keep class com.dailybook.keep.model.subscription.VerifySubscriptionRequest {*;}
-keep class com.dailybook.keep.model.subscription.VerifySubscriptionResponse {*;}

# Keep data class constructors and properties for Gson
-keepclassmembers class com.dailybook.keep.model.subscription.** {
    <init>(...);
    <fields>;
}