package com.laborbook.keep.model

import com.google.gson.annotations.SerializedName

data class TeamResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("company_id") val companyId: String
)
