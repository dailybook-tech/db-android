package com.boilerplate.network.auth.data.repository

import com.boilerplate.network.auth.model.FixTokenResponse
import com.boilerplate.network.model.NetworkResult
import retrofit2.http.Body

interface AuthRepository {

    suspend fun generateAccessToken(baseUrl : String, refreshToken: HashMap<String, String>) : NetworkResult<FixTokenResponse?>
}