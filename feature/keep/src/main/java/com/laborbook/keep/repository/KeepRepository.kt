package com.laborbook.keep.repository

import com.boilerplate.network.model.NetworkResult
import com.laborbook.keep.model.AddAdvanceRequestBody
import com.laborbook.keep.model.AdvanceEntryRequestBody
import com.laborbook.keep.model.AdvanceEntryResponse
import com.laborbook.keep.model.AssignWorkerToTeamRequestBody
import com.laborbook.keep.model.CreateTeamRequestBody
import com.laborbook.keep.model.TeamResponse
import com.laborbook.keep.model.UpdateTeamRequestBody
import com.laborbook.keep.model.AddStaffUserRequestBody
import com.laborbook.keep.model.AddStaffUserResponse
import com.laborbook.keep.model.AddStaffUsersRequestBody
import com.laborbook.keep.model.GetUserResponse
import com.laborbook.keep.model.MarkBulkAttendanceRequestBody
import com.laborbook.keep.model.MarkSingleAttendanceRequestBody
import com.laborbook.keep.model.SalaryData
import com.laborbook.keep.model.StaffAttendanceResponse
import com.laborbook.keep.model.StaffUserResponseModel
import com.laborbook.keep.model.UpdateUserNameRequestBody
import com.laborbook.keep.screen.addstaff.model.ContactItem
import kotlinx.coroutines.flow.Flow
import com.laborbook.keep.model.CurrentSalaryResponse

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
    suspend fun addOrUpdateSalary(userId: String, body: com.laborbook.keep.model.AddOrUpdateSalaryRequestBody): Flow<NetworkResult<String?>>
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