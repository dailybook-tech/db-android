package co.dailybook.keep.usecase

import android.content.Context
import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.keep.model.AddAdvanceRequestBody
import co.dailybook.keep.model.AddStaffUserRequestBody
import co.dailybook.keep.model.AddStaffUserResponse
import co.dailybook.keep.model.AddStaffUsersRequestBody
import co.dailybook.keep.model.GetUserResponse
import co.dailybook.keep.model.MarkBulkAttendanceRequestBody
import co.dailybook.keep.model.MarkSingleAttendanceRequestBody
import co.dailybook.keep.model.StaffAttendanceResponse
import co.dailybook.keep.model.StaffUserResponseModel
import co.dailybook.keep.model.UpdateUserNameRequestBody
import co.dailybook.keep.screen.addstaff.model.ContactItem
import kotlinx.coroutines.flow.Flow
import co.dailybook.keep.model.CurrentSalaryResponse

interface KeepUseCase {
    suspend fun getAllContacts(): List<ContactItem>
    suspend fun insertContacts(vararg contacts: ContactItem)
    suspend fun loadContacts(context: Context, shouldHardRefresh: Boolean): List<ContactItem>
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
    suspend fun addOrUpdateSalary(userId: String, body: co.dailybook.keep.model.AddOrUpdateSalaryRequestBody): Flow<NetworkResult<String?>>
    suspend fun getUserSalary(userId: String, month: Int, year: Int): Flow<NetworkResult<co.dailybook.keep.model.SalaryData?>>
    suspend fun getCurrentSalary(userId: String): Flow<NetworkResult<CurrentSalaryResponse?>>
}