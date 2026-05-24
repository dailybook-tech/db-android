package com.laborbook.keep.model

import com.google.gson.annotations.SerializedName

data class CreateTeamRequestBody(
    @SerializedName("name") val name: String
)

data class UpdateTeamRequestBody(
    @SerializedName("name") val name: String
)

data class AssignWorkerToTeamRequestBody(
    @SerializedName("team_id") val teamId: String?
)
