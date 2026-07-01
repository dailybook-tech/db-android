package com.laborbook.auth.repository

import com.laborbook.auth.model.request.AuthRequestBody
import com.boilerplate.network.model.NetworkResult
import com.laborbook.auth.model.request.AuthResponse
import com.laborbook.auth.model.request.TruecallerRequestBody
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun generateOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>>
    suspend fun resendOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>>
    suspend fun verifyOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>>
    suspend fun verifyFirebaseOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>>
    suspend fun truecallerLogin(truecallerRequestBody: TruecallerRequestBody): Flow<NetworkResult<AuthResponse?>>
}
