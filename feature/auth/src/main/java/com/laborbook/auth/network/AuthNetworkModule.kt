package com.laborbook.auth.network

import com.boilerplate.network.NetworkHandler
import com.boilerplate.network.model.NetworkResult
import com.laborbook.auth.model.request.AuthRequestBody
import com.laborbook.auth.model.request.AuthResponse
import com.laborbook.auth.model.request.TruecallerRequestBody
import com.laborbook.base.BaseConstants
import kotlinx.coroutines.flow.Flow

open class AuthNetworkModule {

    private val baseUrl = if (BaseConstants.DEBUG) BaseConstants.BASE_URL_SBOX else BaseConstants.BASE_URL
    var networkHandler = NetworkHandler.getInstance()
    private val api: AuthApi = networkHandler.getApiClient<AuthApi>(baseUrl)

    // Use the API to call endpoints
    suspend fun generateOtp(authRequestBody: AuthRequestBody) : Flow<NetworkResult<Unit?>> {
        return networkHandler.getData {
            api.generateOtp(authRequestBody)
        }
    }

    suspend fun resendOtp(authRequestBody: AuthRequestBody) : Flow<NetworkResult<Unit?>> {
        return networkHandler.getData {
            api.resendOtp(authRequestBody)
        }
    }

    suspend fun verifyOtp(authRequestBody: AuthRequestBody) : Flow<NetworkResult<AuthResponse?>> {
        return networkHandler.getData {
            api.verifyOtp(authRequestBody)
        }
    }

    suspend fun verifyFirebaseOtp(authRequestBody: AuthRequestBody) : Flow<NetworkResult<AuthResponse?>> {
        return networkHandler.getData {
            api.verifyFirebaseOtp(authRequestBody)
        }
    }

    suspend fun truecallerLogin(truecallerRequestBody: TruecallerRequestBody) : Flow<NetworkResult<AuthResponse?>> {
        return networkHandler.getData {
            api.truecallerLogin(truecallerRequestBody)
        }
    }
}
