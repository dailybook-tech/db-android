package co.dailybook.keep.repository

import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.keep.model.AddAdvanceRequestBody
import co.dailybook.keep.model.AdvanceEntryRequestBody
import co.dailybook.keep.model.AdvanceEntryResponse
import co.dailybook.keep.model.AssignWorkerToTeamRequestBody
import co.dailybook.keep.model.CreateTeamRequestBody
import co.dailybook.keep.model.TeamResponse
import co.dailybook.keep.model.UpdateTeamRequestBody
import co.dailybook.keep.model.AddStaffUserRequestBody
import co.dailybook.keep.model.AddStaffUserResponse
import co.dailybook.keep.model.AddStaffUsersRequestBody
import co.dailybook.keep.model.GetUserResponse
import co.dailybook.keep.model.MarkBulkAttendanceRequestBody
import co.dailybook.keep.model.MarkSingleAttendanceRequestBody
import co.dailybook.keep.model.SalaryData
import co.dailybook.keep.model.StaffAttendanceResponse
import co.dailybook.keep.model.StaffUserResponseModel
import co.dailybook.keep.model.UpdateUserNameRequestBody
import co.dailybook.keep.network.KeepNetworkModule
import co.dailybook.keep.screen.addstaff.model.ContactDao
import co.dailybook.keep.screen.addstaff.model.ContactItem
import kotlinx.coroutines.flow.Flow
import co.dailybook.keep.model.CurrentSalaryResponse

class KeepRepositoryImplementation(val keepNetworkModule: KeepNetworkModule, private val contactDao: ContactDao): KeepRepository {

    override suspend fun getAllContacts(): List<ContactItem> = contactDao.getAllContacts()

    override suspend fun insertContacts(vararg contacts: ContactItem) = contactDao.insertContacts(*contacts)

    override suspend fun deleteAllContacts() = contactDao.deleteAllContacts()

    override suspend fun getStaffs(id : String): Flow<NetworkResult<StaffUserResponseModel?>> {
        return keepNetworkModule.getStaffUsers(id)
    }

    override suspend fun getUserAttendances(
        id: String,
        month: String,
        year: String,
    ): Flow<NetworkResult<StaffAttendanceResponse?>> {
        return keepNetworkModule.getUserAttendances(id, month, year)
    }

    override suspend fun markBulkAttendance(
        id: String,
        markAttendanceBody: MarkBulkAttendanceRequestBody,
    ): Flow<NetworkResult<String?>> {
        return keepNetworkModule.markBulkAttendance(id, markAttendanceBody)
    }

    override suspend fun markSingleAttendance(
        id: String,
        markAttendanceBody: MarkSingleAttendanceRequestBody,
    ): Flow<NetworkResult<String?>> {
        return keepNetworkModule.markSingleAttendance(id, markAttendanceBody)
    }

    override suspend fun addStaffUsers(staffUsers: AddStaffUsersRequestBody): Flow<NetworkResult<String?>> {
        return keepNetworkModule.addStaffUsers(staffUsers)
    }

    override suspend fun addStaffUser(staffUser: AddStaffUserRequestBody): Flow<NetworkResult<AddStaffUserResponse?>> {
        return keepNetworkModule.addStaffUser(staffUser)
    }

    override suspend fun deleteStaffUser(id: String): Flow<NetworkResult<String?>> {
        return keepNetworkModule.deleteStaffUser(id)
    }

    override suspend fun updateUserName(
        id: String,
        updateUserNameRequestBody: UpdateUserNameRequestBody,
    ): Flow<NetworkResult<GetUserResponse?>> {
        return keepNetworkModule.updateUserName(id, updateUserNameRequestBody)
    }

    override suspend fun addOvertime(userId: String, date: String, otMinutes: Double, otPerHour: Double, managerId: String): Flow<NetworkResult<String?>> {
        return keepNetworkModule.saveOvertime(userId, date, otMinutes, otPerHour, managerId)
    }

    override suspend fun deleteOvertime(userId: String, date: String, managerId: String): Flow<NetworkResult<String?>> {
        return keepNetworkModule.deleteOvertime(userId, date, managerId)
    }

    override suspend fun getUser(id: String): Flow<NetworkResult<GetUserResponse?>> {
        return keepNetworkModule.getUser(id)
    }

    override suspend fun addAdvance(
        id: String,
        addAdvanceRequestBody: AddAdvanceRequestBody,
    ): Flow<NetworkResult<String?>> {
        return keepNetworkModule.addAdvance(id, addAdvanceRequestBody)
    }

    override suspend fun addOrUpdateSalary(userId: String, body: co.dailybook.keep.model.AddOrUpdateSalaryRequestBody): Flow<NetworkResult<String?>> =
        keepNetworkModule.addOrUpdateSalary(userId, body)

    override suspend fun getUserSalary(userId: String, month: Int, year: Int): Flow<NetworkResult<SalaryData?>> =
        keepNetworkModule.getUserSalary(userId, month, year)

    override suspend fun getCurrentSalary(userId: String): Flow<NetworkResult<CurrentSalaryResponse?>> =
        keepNetworkModule.getCurrentSalary(userId)

    override suspend fun addAdvanceEntry(userId: String, body: AdvanceEntryRequestBody): Flow<NetworkResult<AdvanceEntryResponse?>> =
        keepNetworkModule.addAdvanceEntry(userId, body)

    override suspend fun listAdvanceEntries(userId: String, month: Int, year: Int): Flow<NetworkResult<List<AdvanceEntryResponse>?>> =
        keepNetworkModule.listAdvanceEntries(userId, month, year)

    override suspend fun deleteAdvanceEntry(advanceId: String): Flow<NetworkResult<String?>> =
        keepNetworkModule.deleteAdvanceEntry(advanceId)

    override suspend fun createTeam(body: CreateTeamRequestBody): Flow<NetworkResult<TeamResponse?>> =
        keepNetworkModule.createTeam(body)

    override suspend fun listTeams(): Flow<NetworkResult<List<TeamResponse>?>> =
        keepNetworkModule.listTeams()

    override suspend fun updateTeam(teamId: String, body: UpdateTeamRequestBody): Flow<NetworkResult<TeamResponse?>> =
        keepNetworkModule.updateTeam(teamId, body)

    override suspend fun deleteTeam(teamId: String): Flow<NetworkResult<String?>> =
        keepNetworkModule.deleteTeam(teamId)

    override suspend fun assignWorkerToTeam(userId: String, body: AssignWorkerToTeamRequestBody): Flow<NetworkResult<String?>> =
        keepNetworkModule.assignWorkerToTeam(userId, body)
}