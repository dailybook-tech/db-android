package com.dailybook.auth.repository

import com.dailybook.auth.model.request.AuthRequestBody
import com.boilerplate.network.model.NetworkResult
import com.dailybook.auth.model.request.AuthResponse
import com.dailybook.auth.model.request.TruecallerRequestBody
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun generateOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>>
    suspend fun resendOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>>
    suspend fun verifyOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>>
    suspend fun truecallerLogin(truecallerRequestBody: TruecallerRequestBody): Flow<NetworkResult<AuthResponse?>>
}