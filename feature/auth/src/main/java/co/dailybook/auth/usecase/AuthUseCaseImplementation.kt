package co.dailybook.auth.usecase

import co.dailybook.auth.model.request.AuthRequestBody
import co.dailybook.auth.repository.AuthRepository
import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.auth.model.request.AuthResponse
import co.dailybook.auth.model.request.TruecallerRequestBody
import kotlinx.coroutines.flow.Flow

class AuthUseCaseImplementation(val authRepository: AuthRepository) : AuthUseCase {
    override suspend fun generateOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>> {
        return authRepository.generateOtp(authRequestBody)
    }

    override suspend fun resendOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>> {
        return authRepository.resendOtp(authRequestBody)
    }

    override suspend fun verifyOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authRepository.verifyOtp(authRequestBody)
    }

    override suspend fun verifyFirebaseOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authRepository.verifyFirebaseOtp(authRequestBody)
    }

    override suspend fun truecallerLogin(truecallerRequestBody: TruecallerRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authRepository.truecallerLogin(truecallerRequestBody)
    }
}
