package co.dailybook.boilerplate.network.auth.data.repository

import co.dailybook.boilerplate.network.NetworkHandler
import co.dailybook.boilerplate.network.NetworkResource
import co.dailybook.boilerplate.network.auth.data.remote.AuthApi
import co.dailybook.boilerplate.network.auth.model.FixTokenResponse
import co.dailybook.boilerplate.network.model.NetworkResult

class AuthRepositoryImpl : AuthRepository {

    override suspend fun generateAccessToken(baseUrl : String, refreshToken: HashMap<String, String>): NetworkResult<FixTokenResponse?> {
        val client = NetworkHandler.getInstance().getDefaultApiClient<AuthApi>()
        return NetworkResource(
            {client.generateAccessToken(refreshToken)},
            isCallingGenerateAccessToken = true
        ).queryWithoutFlow()
    }
}