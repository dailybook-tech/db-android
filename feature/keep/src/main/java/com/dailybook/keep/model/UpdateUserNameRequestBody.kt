package com.dailybook.keep.model

data class UpdateUserNameRequestBody(
    val name: String,
    val mobile_number: String? = null
)