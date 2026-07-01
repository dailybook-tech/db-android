package com.laborbook.auth.model.request

import com.google.gson.annotations.SerializedName

data class AuthRequestBody(
    @SerializedName("country_code")
    val countryCode: String? = null,
    @SerializedName("mobile_number")
    val mobileNumber: String? = null,
    @SerializedName("otp")
    val otp: String? = null,
    @SerializedName("firebase_id_token")
    val firebaseIdToken: String? = null,
    @SerializedName("install_source")
    val installSource: String? = null, // "meta_ads" or "organic", set from Meta Install Referrer
    @SerializedName("install_referrer_payload")
    val installReferrerPayload: String? = null, // Raw payload for backend decryption later
)

data class AuthResponse(
    @SerializedName("auth_token") val authToken: String?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("user") val user: User?,
)

data class User(
    @SerializedName("user_id") val userId: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_type") val userType: String?,
    @SerializedName("mobile_number") val mobileNumber: String?,
    @SerializedName("company_id") val companyId: String?,
)

data class TruecallerRequestBody(
    @SerializedName("authorization_code")
    val authorizationCode: String,
    @SerializedName("code_verifier")
    val codeVerifier: String,
    @SerializedName("install_source")
    val installSource: String? = null, // "meta_ads" or "organic", set from Meta Install Referrer
    @SerializedName("install_referrer_payload")
    val installReferrerPayload: String? = null, // Raw payload for backend decryption later
)
