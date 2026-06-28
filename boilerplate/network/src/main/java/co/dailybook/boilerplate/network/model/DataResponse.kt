package co.dailybook.boilerplate.network.model

import com.google.gson.annotations.SerializedName

data class DataResponse<T>(
    @SerializedName("data") val data: T
)