package co.dailybook.boilerplate.network.auth.data.remote

import co.dailybook.boilerplate.network.auth.model.FixTokenResponse
import co.dailybook.boilerplate.network.model.DataResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/atom/external/v1/auth/refresh-token")
    suspend fun generateAccessToken(@Body refreshToken: HashMap<String, String>): Response<DataResponse<FixTokenResponse>>

}