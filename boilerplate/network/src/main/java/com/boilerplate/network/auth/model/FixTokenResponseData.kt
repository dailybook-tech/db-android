package com.boilerplate.network.auth.model

import com.google.gson.annotations.SerializedName

data class FixTokenResponseData(
    val data: FixTokenResponse?
)

data class FixTokenResponse(
    val token: FixToken?
)

data class FixToken(
    @field:SerializedName("access_token")
    val accessToken: String?,
    @field:SerializedName("refresh_token")
    val refreshToken: String?,
    @field:SerializedName("expires_in")
    val expiresIn: Long?,
    @field:SerializedName("refresh_expires_in")
    val refreshExpiresIn: Long?,
    @field:SerializedName("not_before_policy")
    val notBeforePolicy: Long?,
    @field:SerializedName("token_type")
    val tokenType: String?,
): java.io.Serializable