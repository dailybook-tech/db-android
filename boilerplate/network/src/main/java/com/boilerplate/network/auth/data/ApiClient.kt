package com.boilerplate.network.auth.data

import com.boilerplate.network.auth.data.remote.AuthApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient(private val baseUrl : String) {

    fun getClient() = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApi::class.java)
}