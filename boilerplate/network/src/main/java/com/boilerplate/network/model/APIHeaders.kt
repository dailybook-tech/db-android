package com.boilerplate.network.model

data class APIHeaders(
    var deviceId: String,
    var systemId: String,
    var appVersion: String,
    var accessToken: String = "",
    var userId: String = "",
    var additionalHeaders : HashMap<String, String> = hashMapOf()
)
