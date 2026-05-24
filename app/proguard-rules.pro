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
-keep class com.laborbook.** {*;}
-keepclassmembers class com.laborbook.** {*;}
-keep class com.boilerplate.** {*;}
-keepclassmembers class com.boilerplate.** {*;}

# ===================================
# Razorpay SDK ProGuard Rules
# ===================================
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepattributes JavascriptInterface
-keepattributes *Annotation*

-dontwarn com.razorpay.**
-keep class com.razorpay.** {*;}

-optimizations !method/inlining/*

-keepclasseswithmembers class * {
  public void onPayment*(...);
}

# Keep Razorpay callback interfaces
-keep class com.razorpay.PaymentResultListener {*;}
-keep class com.razorpay.PaymentResultWithDataListener {*;}
-keep class com.razorpay.Checkout {*;}
-keep class com.razorpay.PaymentData {*;}
-keep class com.razorpay.CheckoutActivity {*;}

# Keep JSONObject for Razorpay payment options
-keep class org.json.** { *; }

# Keep BuildConfig (required by Razorpay)
-keep class com.laborbook.BuildConfig {*;}
-keep class com.laborbook.keep.BuildConfig {*;}

# ===================================
# Subscription Models ProGuard Rules
# ===================================
# Keep all subscription model classes and their members
-keep class com.laborbook.keep.model.subscription.** {*;}
-keepclassmembers class com.laborbook.keep.model.subscription.** {*;}

# Explicitly keep all subscription data classes for Gson
-keep class com.laborbook.keep.model.subscription.SubscriptionPlan {*;}
-keep class com.laborbook.keep.model.subscription.MetaData {*;}
-keep class com.laborbook.keep.model.subscription.SubscriptionPlansResponse {*;}
-keep class com.laborbook.keep.model.subscription.CreateSubscriptionRequest {*;}
-keep class com.laborbook.keep.model.subscription.Addon {*;}
-keep class com.laborbook.keep.model.subscription.AddonItem {*;}
-keep class com.laborbook.keep.model.subscription.CreateSubscriptionResponse {*;}
-keep class com.laborbook.keep.model.subscription.CancelSubscriptionResponse {*;}
-keep class com.laborbook.keep.model.subscription.UserSubscription {*;}
-keep class com.laborbook.keep.model.subscription.SubscriptionDetails {*;}
-keep class com.laborbook.keep.model.subscription.Feature {*;}
-keep class com.laborbook.keep.model.subscription.VerifySubscriptionRequest {*;}
-keep class com.laborbook.keep.model.subscription.VerifySubscriptionResponse {*;}

# Keep data class constructors and properties for Gson
-keepclassmembers class com.laborbook.keep.model.subscription.** {
    <init>(...);
    <fields>;
}