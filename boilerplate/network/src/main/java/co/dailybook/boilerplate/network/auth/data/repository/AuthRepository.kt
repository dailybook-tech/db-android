package co.dailybook.boilerplate.network.auth.data.repository

import co.dailybook.boilerplate.network.auth.model.FixTokenResponse
import co.dailybook.boilerplate.network.model.NetworkResult
import retrofit2.http.Body

interface AuthRepository {

    suspend fun generateAccessToken(baseUrl : String, refreshToken: HashMap<String, String>) : NetworkResult<FixTokenResponse?>
}