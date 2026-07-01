package co.dailybook.auth.network

import co.dailybook.boilerplate.network.NetworkHandler
import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.auth.model.request.AuthRequestBody
import co.dailybook.auth.model.request.AuthResponse
import co.dailybook.auth.model.request.TruecallerRequestBody
import co.dailybook.base.BaseConstants
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
