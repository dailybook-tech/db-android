package co.dailybook.auth.repository

import co.dailybook.auth.model.request.AuthRequestBody
import co.dailybook.auth.network.AuthNetworkModule
import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.auth.model.request.AuthResponse
import co.dailybook.auth.model.request.TruecallerRequestBody
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImplementation(val authNetworkModule: AuthNetworkModule) : AuthRepository{
    override suspend fun generateOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>> {
        return authNetworkModule.generateOtp(authRequestBody)
    }

    override suspend fun resendOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<Unit?>> {
        return authNetworkModule.resendOtp(authRequestBody)
    }

    override suspend fun verifyOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authNetworkModule.verifyOtp(authRequestBody)
    }

    override suspend fun verifyFirebaseOtp(authRequestBody: AuthRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authNetworkModule.verifyFirebaseOtp(authRequestBody)
    }

    override suspend fun truecallerLogin(truecallerRequestBody: TruecallerRequestBody): Flow<NetworkResult<AuthResponse?>> {
        return authNetworkModule.truecallerLogin(truecallerRequestBody)
    }
}
