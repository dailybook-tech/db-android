package com.laborbook.keep.model

import com.google.gson.annotations.SerializedName

data class AddStaffUsersRequestBody(
    val users: List<Staff>,
    @SerializedName("created_by")
    val createdBy: String,
)

data class Staff(
    val name: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("user_type")
    val userType: String = "STAFF",
)

data class AddStaffUserRequestBody(
    val name: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("created_by")
    val createdBy: String,
    val category: String? = null,
)

data class AddStaffUserResponse(
    val id: String,
    val name: String,
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @SerializedName("created_by")
    val createdBy: String,
)