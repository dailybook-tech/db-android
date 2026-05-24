package com.laborbook.keep.screen.teams

import com.laborbook.keep.model.TeamResponse

sealed class TeamUiState {
    object Loading : TeamUiState()
    data class ListSuccess(val teams: List<TeamResponse>) : TeamUiState()
    data class CreateSuccess(val team: TeamResponse) : TeamUiState()
    data class UpdateSuccess(val team: TeamResponse) : TeamUiState()
    object DeleteSuccess : TeamUiState()
    data class Error(val message: String?) : TeamUiState()
}
