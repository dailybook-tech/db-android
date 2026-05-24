package com.boilerplate.network.auth.data.remote

import com.boilerplate.network.auth.model.FixTokenResponse
import com.boilerplate.network.model.DataResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/atom/external/v1/auth/refresh-token")
    suspend fun generateAccessToken(@Body refreshToken: HashMap<String, String>): Response<DataResponse<FixTokenResponse>>

}