package com.boilerplate.network.auth.callback

interface DefaultAuthenticationCallback {

    fun onNewAccessTokenGenerated(accessToken : String?, refreshToken : String?, expiresIn : Long?)

    fun onRefreshTokenFailed()
}