package co.dailybook.keep.screen.home.viewmodel

import androidx.lifecycle.viewModelScope
import co.dailybook.boilerplate.network.model.NetworkResultStatus
import co.dailybook.base.BaseViewModel
import co.dailybook.base.Logger
import co.dailybook.base.datastore.DataStoreManager
import co.dailybook.keep.model.StaffUser
import co.dailybook.keep.screen.home.uistate.StaffsUiState
import co.dailybook.keep.usecase.KeepUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StaffsViewModel(val keepUseCase: KeepUseCase) : BaseViewModel<StaffsUiState>(), KoinComponent {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logger.e("Caught exception: ${exception.localizedMessage}")
        uiState.value = StaffsUiState.Error(exception.localizedMessage)
    }

    private var cachedStaffList: List<StaffUser> = emptyList()
    private var activeTeamFilter: String? = null // null = "All"

    fun getUsers(id : String) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        keepUseCase.getStaffs(id).collect(collector = {
            withContext(Dispatchers.Main) {
                when (it.status) {
                    NetworkResultStatus.SUCCESS -> {
                        it.data?.users?.let { staffUsers ->
                            cachedStaffList = staffUsers
                            uiState.value = StaffsUiState.Success(applyFilter(staffUsers))
                        }
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value = StaffsUiState.Error(it.message ?: "")
                    }

                    NetworkResultStatus.LOADING -> {
                        uiState.value = StaffsUiState.Loading
                    }
                }
            }
        })
    }

    fun filterByTeam(teamId: String?) {
        activeTeamFilter = teamId
        uiState.value = StaffsUiState.Success(applyFilter(cachedStaffList))
    }

    private fun applyFilter(list: List<StaffUser>): List<StaffUser> {
        return if (activeTeamFilter == null) list
        else list.filter { it.teamId == activeTeamFilter }
    }

    fun getDistinctTeamIds(): List<String?> =
        cachedStaffList.map { it.teamId }.distinct()

    fun getStaffCount(): Int {
        return cachedStaffList.size
    }
}