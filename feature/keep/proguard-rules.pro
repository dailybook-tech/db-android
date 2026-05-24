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
-keep class com.laborbook.keep.** {
    *;
}
-keepclassmembers class com.laborbook.keep.** {*;}
-keep class com.boilerplate.** {*;}
-keepclassmembers class com.boilerplate.** {*;}

-keepattributes SourceFile,LineNumberTable
-keep class com.inmobi.** { *; }
-keep public class com.google.android.gms.**
-dontwarn com.google.android.gms.**
-dontwarn com.squareup.picasso.**
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient{
     public *;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info{
     public *;
}
# skip the Picasso library classes
-keep class com.squareup.picasso.** {*;}
-dontwarn com.squareup.okhttp.**
# skip Moat classes
-keep class com.moat.** {*;}
-dontwarn com.moat.**
# skip IAB classes
-keep class com.iab.** {*;}
-dontwarn com.iab.**

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

# Keep Razorpay callback methods
-keep class com.razorpay.PaymentResultListener {*;}
-keep class com.razorpay.PaymentResultWithDataListener {*;}
-keep class com.razorpay.Checkout {*;}
-keep class com.razorpay.Razorpay {*;}
-keep class com.razorpay.PaymentData {*;}

# Keep JSONObject for Razorpay payment options
-keep class org.json.** { *; }

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

# Keep Gson serialization names
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep data class constructors and properties for Gson
-keepclassmembers class com.laborbook.keep.model.subscription.** {
    <init>(...);
    <fields>;
}

# Subscription API interfaces
-keep class com.laborbook.keep.network.KeepApi {*;}
-keepclassmembers interface com.laborbook.keep.network.KeepApi {*;}

# Subscription Repository and UseCase
-keep class com.laborbook.keep.repository.Subscription** {*;}
-keep class com.laborbook.keep.usecase.Subscription** {*;}

# Subscription ViewModel
-keep class com.laborbook.keep.screen.premium.viewmodel.SubscriptionViewModel {*;}
-keep class com.laborbook.keep.screen.premium.viewmodel.SubscriptionViewModel$** {*;}

# Premium Dialog Fragment
-keep class com.laborbook.keep.screen.premium.PremiumOfferDialogFragment {*;}
-keepclassmembers class com.laborbook.keep.screen.premium.PremiumOfferDialogFragment {
    public void onPaymentSuccess(...);
    public void onPaymentError(...);
}

# UPI App Detection
-keep class com.laborbook.keep.screen.premium.UpiAppDetector {*;}
-keep class com.laborbook.keep.screen.premium.InstalledUpiApp {*;}

# Subscription UI Components
-keep class com.laborbook.keep.screen.premium.SubscriptionPlanAdapter {*;}
-keep class com.laborbook.keep.screen.premium.SubscriptionPlanAdapter$** {*;}
-keep class com.laborbook.keep.screen.premium.SubscriptionSuccessDialogFragment {*;}
-keep class com.laborbook.keep.screen.premium.UpiSelectionBottomSheet {*;}
-keep class com.laborbook.keep.screen.premium.UpiAppAdapter {*;}
-keep class com.laborbook.keep.screen.premium.PremiumOfferManager {*;}