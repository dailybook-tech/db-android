package com.dailybook.auth.usecase

import com.dailybook.auth.model.request.AuthRequestBody
import com.dailybook.auth.repository.AuthRepository
import com.boilerplate.network.model.NetworkResult
import com.dailybook.auth.model.request.AuthResponse
import com.dailybook.auth.model.request.TruecallerRequestBody
import kotlinx.coroutines.flow.Flow

class AuthUseCaseImplementation(val authRepository: AuthRepository) : AuthUseCase {
    override suspend fun generateOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>> {
        return authRepository.generateOtp(authRequestBody)
    }

    override suspend fun resendOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>> {
        return authRepository.generateOtp(authRequestBody)
    }

    override suspend fun verifyOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authRepository.verifyOtp(authRequestBody)
    }

    override suspend fun truecallerLogin(truecallerRequestBody: TruecallerRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authRepository.truecallerLogin(truecallerRequestBody)
    }
}