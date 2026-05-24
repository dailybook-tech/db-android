package com.boilerplate.network.auth.data.repository

import com.boilerplate.network.NetworkHandler
import com.boilerplate.network.NetworkResource
import com.boilerplate.network.auth.data.remote.AuthApi
import com.boilerplate.network.auth.model.FixTokenResponse
import com.boilerplate.network.model.NetworkResult

class AuthRepositoryImpl : AuthRepository {

    override suspend fun generateAccessToken(baseUrl : String, refreshToken: HashMap<String, String>): NetworkResult<FixTokenResponse?> {
        val client = NetworkHandler.getInstance().getDefaultApiClient<AuthApi>()
        return NetworkResource(
            {client.generateAccessToken(refreshToken)},
            isCallingGenerateAccessToken = true
        ).queryWithoutFlow()
    }
}