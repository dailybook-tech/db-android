package com.laborbook.keep.model

import com.google.gson.annotations.SerializedName

data class GetUserResponse(
    val id: String,
    val name: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("company_id")
    val companyId: String,
    @SerializedName("user_type")
    val userType: String,
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("team_id")
    val teamId: String? = null,
)