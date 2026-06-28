package co.dailybook.auth.usecase

import co.dailybook.auth.model.request.AuthRequestBody
import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.auth.model.request.AuthResponse
import co.dailybook.auth.model.request.TruecallerRequestBody
import kotlinx.coroutines.flow.Flow

interface AuthUseCase {
    suspend fun generateOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>>
    suspend fun resendOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>>
    suspend fun verifyOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>>
    suspend fun truecallerLogin(truecallerRequestBody: TruecallerRequestBody): Flow<NetworkResult<AuthResponse?>>
}