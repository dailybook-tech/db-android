package com.dailybook.auth.network

import com.boilerplate.network.model.DataResponse
import com.dailybook.auth.model.request.AuthRequestBody
import com.dailybook.auth.model.request.AuthResponse
import com.dailybook.auth.model.request.TruecallerRequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    companion object {
        const val API_VERSION = "api/v1"
        const val GENERATE_OTP = "/create-otp"
        const val RESENT_OTP = "/resend-otp"
        const val VERIFY_OTP = "/verify-otp"
        const val TRUECALLER_LOGIN = "/login/truecaller"
    }

    @POST(API_VERSION + GENERATE_OTP)
    suspend fun generateOtp(@Body authRequestBody: AuthRequestBody): Response<DataResponse<Unit>>

    @POST(API_VERSION + RESENT_OTP)
    suspend fun resendOtp(@Body authRequestBody: AuthRequestBody): Response<DataResponse<Unit>>

    @POST(API_VERSION + VERIFY_OTP)
    suspend fun verifyOtp(@Body authRequestBody: AuthRequestBody): Response<DataResponse<AuthResponse>>

    @POST(API_VERSION + TRUECALLER_LOGIN)
    suspend fun truecallerLogin(@Body truecallerRequestBody: TruecallerRequestBody): Response<DataResponse<AuthResponse>>
}