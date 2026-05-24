package com.boilerplate.network.model

enum class NetworkResultStatus {
    SUCCESS,
    ERROR,
    LOADING
}

data class NetworkResult<out T>(val status: NetworkResultStatus, val data: T?, val message: String?, val isFromServer: Boolean = false, val code : Int? = 0 ) {
    companion object {
        fun <T> loading(data: T?): NetworkResult<T> {
            return  NetworkResult(status = NetworkResultStatus.LOADING, data = data, message = null)
        }
        
        fun <T> success(data: T,code : Int? = 0, isFromServer: Boolean = false): NetworkResult<T> {
            return NetworkResult(status = NetworkResultStatus.SUCCESS, data = data, message = null, isFromServer,code = code)
        }
        
        fun <T> error(data: T,message: String?,code : Int?,isFromServer: Boolean = true): NetworkResult<T> =
            NetworkResult(status = NetworkResultStatus.ERROR, data = data, message = message,code = code, isFromServer = isFromServer)

    }
}