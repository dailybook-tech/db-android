package com.laborbook.auth.repository

import com.laborbook.auth.model.request.AuthRequestBody
import com.laborbook.auth.network.AuthNetworkModule
import com.boilerplate.network.model.NetworkResult
import com.laborbook.auth.model.request.AuthResponse
import com.laborbook.auth.model.request.TruecallerRequestBody
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImplementation(val authNetworkModule: AuthNetworkModule) : AuthRepository{
    override suspend fun generateOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>> {
        return authNetworkModule.generateOtp(authRequestBody)
    }

    override suspend fun resendOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>> {
        return authNetworkModule.generateOtp(authRequestBody)
    }

    override suspend fun verifyOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authNetworkModule.verifyOtp(authRequestBody)
    }

    override suspend fun truecallerLogin(truecallerRequestBody: TruecallerRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authNetworkModule.truecallerLogin(truecallerRequestBody)
    }
}