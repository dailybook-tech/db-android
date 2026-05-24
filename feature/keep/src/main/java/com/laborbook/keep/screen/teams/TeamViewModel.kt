package com.laborbook.keep.screen.teams

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boilerplate.network.model.NetworkResultStatus
import com.laborbook.keep.model.CreateTeamRequestBody
import com.laborbook.keep.model.UpdateTeamRequestBody
import com.laborbook.keep.repository.KeepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TeamViewModel(private val keepRepository: KeepRepository) : ViewModel() {

    val uiState = MutableLiveData<TeamUiState>()

    fun loadTeams() {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.postValue(TeamUiState.Loading)
            keepRepository.listTeams().collect { result ->
                when (result.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.postValue(TeamUiState.ListSuccess(result.data ?: emptyList()))
                    }
                    NetworkResultStatus.ERROR -> {
                        uiState.postValue(TeamUiState.Error(result.message))
                    }
                    NetworkResultStatus.LOADING -> {
                        uiState.postValue(TeamUiState.Loading)
                    }
                }
            }
        }
    }

    fun createTeam(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.postValue(TeamUiState.Loading)
            keepRepository.createTeam(CreateTeamRequestBody(name)).collect { result ->
                when (result.status) {
                    NetworkResultStatus.SUCCESS -> {
                        result.data?.let { uiState.postValue(TeamUiState.CreateSuccess(it)) }
                    }
                    NetworkResultStatus.ERROR -> {
                        uiState.postValue(TeamUiState.Error(result.message))
                    }
                    else -> {}
                }
            }
        }
    }

    fun updateTeam(teamId: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.postValue(TeamUiState.Loading)
            keepRepository.updateTeam(teamId, UpdateTeamRequestBody(name)).collect { result ->
                when (result.status) {
                    NetworkResultStatus.SUCCESS -> {
                        result.data?.let { uiState.postValue(TeamUiState.UpdateSuccess(it)) }
                    }
                    NetworkResultStatus.ERROR -> {
                        uiState.postValue(TeamUiState.Error(result.message))
                    }
                    else -> {}
                }
            }
        }
    }

    fun deleteTeam(teamId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            keepRepository.deleteTeam(teamId).collect { result ->
                when (result.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.postValue(TeamUiState.DeleteSuccess)
                    }
                    NetworkResultStatus.ERROR -> {
                        uiState.postValue(TeamUiState.Error(result.message))
                    }
                    else -> {}
                }
            }
        }
    }
}
