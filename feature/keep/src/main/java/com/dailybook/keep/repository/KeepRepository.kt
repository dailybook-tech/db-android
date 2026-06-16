package com.dailybook.keep.repository

import com.boilerplate.network.model.NetworkResult
import com.dailybook.keep.model.AddAdvanceRequestBody
import com.dailybook.keep.model.AdvanceEntryRequestBody
import com.dailybook.keep.model.AdvanceEntryResponse
import com.dailybook.keep.model.AssignWorkerToTeamRequestBody
import com.dailybook.keep.model.CreateTeamRequestBody
import com.dailybook.keep.model.TeamResponse
import com.dailybook.keep.model.UpdateTeamRequestBody
import com.dailybook.keep.model.AddStaffUserRequestBody
import com.dailybook.keep.model.AddStaffUserResponse
import com.dailybook.keep.model.AddStaffUsersRequestBody
import com.dailybook.keep.model.GetUserResponse
import com.dailybook.keep.model.MarkBulkAttendanceRequestBody
import com.dailybook.keep.model.MarkSingleAttendanceRequestBody
import com.dailybook.keep.model.SalaryData
import com.dailybook.keep.model.StaffAttendanceResponse
import com.dailybook.keep.model.StaffUserResponseModel
import com.dailybook.keep.model.UpdateUserNameRequestBody
import com.dailybook.keep.screen.addstaff.model.ContactItem
import kotlinx.coroutines.flow.Flow
import com.dailybook.keep.model.CurrentSalaryResponse

interface KeepRepository {

    suspend fun getAllContacts(): List<ContactItem>
    suspend fun insertContacts(vararg contacts: ContactItem)
    suspend fun deleteAllContacts()

    suspend fun getStaffs(id : String): Flow<NetworkResult<StaffUserResponseModel?>>
    suspend fun getUserAttendances(id: String, month: String, year: String): Flow<NetworkResult<StaffAttendanceResponse?>>
    suspend fun markBulkAttendance(id: String, markAttendanceBody: MarkBulkAttendanceRequestBody): Flow<NetworkResult<String?>>
    suspend fun markSingleAttendance(id: String, markAttendanceBody: MarkSingleAttendanceRequestBody): Flow<NetworkResult<String?>>
    suspend fun addStaffUsers(staffUsers: AddStaffUsersRequestBody): Flow<NetworkResult<String?>>
    suspend fun addStaffUser(staffUser: AddStaffUserRequestBody): Flow<NetworkResult<AddStaffUserResponse?>>
    suspend fun deleteStaffUser(id: String): Flow<NetworkResult<String?>>
    suspend fun updateUserName(id: String, updateUserNameRequestBody: UpdateUserNameRequestBody): Flow<NetworkResult<GetUserResponse?>>
    suspend fun getUser(id: String): Flow<NetworkResult<GetUserResponse?>>
    suspend fun addAdvance(id: String, addAdvanceRequestBody: AddAdvanceRequestBody): Flow<NetworkResult<String?>>
    suspend fun addOvertime(userId: String, date: String, otMinutes: Double, otPerHour: Double, managerId: String): Flow<NetworkResult<String?>>
    suspend fun deleteOvertime(userId: String, date: String, managerId: String): Flow<NetworkResult<String?>>
    suspend fun addOrUpdateSalary(userId: String, body: com.dailybook.keep.model.AddOrUpdateSalaryRequestBody): Flow<NetworkResult<String?>>
    suspend fun getUserSalary(userId: String, month: Int, year: Int): Flow<NetworkResult<SalaryData?>>
    suspend fun getCurrentSalary(userId: String): Flow<NetworkResult<CurrentSalaryResponse?>>
    suspend fun addAdvanceEntry(userId: String, body: AdvanceEntryRequestBody): Flow<NetworkResult<AdvanceEntryResponse?>>
    suspend fun listAdvanceEntries(userId: String, month: Int, year: Int): Flow<NetworkResult<List<AdvanceEntryResponse>?>>
    suspend fun deleteAdvanceEntry(advanceId: String): Flow<NetworkResult<String?>>
    suspend fun createTeam(body: CreateTeamRequestBody): Flow<NetworkResult<TeamResponse?>>
    suspend fun listTeams(): Flow<NetworkResult<List<TeamResponse>?>>
    suspend fun updateTeam(teamId: String, body: UpdateTeamRequestBody): Flow<NetworkResult<TeamResponse?>>
    suspend fun deleteTeam(teamId: String): Flow<NetworkResult<String?>>
    suspend fun assignWorkerToTeam(userId: String, body: AssignWorkerToTeamRequestBody): Flow<NetworkResult<String?>>
}