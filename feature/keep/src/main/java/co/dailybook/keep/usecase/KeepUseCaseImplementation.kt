package co.dailybook.keep.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.keep.model.AddAdvanceRequestBody
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
import co.dailybook.keep.repository.KeepRepository
import co.dailybook.keep.screen.addstaff.model.ContactItem
import kotlinx.coroutines.flow.Flow
import co.dailybook.keep.model.CurrentSalaryResponse

class KeepUseCaseImplementation(val keepRepository: KeepRepository) : KeepUseCase {

    override suspend fun getAllContacts(): List<ContactItem> = keepRepository.getAllContacts()

    override suspend fun insertContacts(vararg contacts: ContactItem) = keepRepository.insertContacts(*contacts)

    override suspend fun deleteAllContacts() = keepRepository.deleteAllContacts()

    override suspend fun loadContacts(context: Context, shouldHardRefresh: Boolean): List<ContactItem> {
        var contactsList = keepRepository.getAllContacts()
        if (contactsList.isEmpty() || shouldHardRefresh) {
            contactsList = fetchContactsFromDevice(context)
            if(shouldHardRefresh){
                keepRepository.deleteAllContacts()
            }
            keepRepository.insertContacts(*contactsList.toTypedArray())
        }
        return contactsList
    }

    @SuppressLint("Range")
    private fun fetchContactsFromDevice(context: Context): List<ContactItem> {
        val contacts = mutableListOf<ContactItem>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                if (it.getInt(it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id), null)

                    phoneCursor?.use { pc ->
                        while (pc.moveToNext()) {
                            val phoneNumber = pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            contacts.add(ContactItem(0, name, phoneNumber))
                        }
                    }
                }
            }
        }
        return contacts
    }

    override suspend fun getStaffs(id : String): Flow<NetworkResult<StaffUserResponseModel?>> {
        return keepRepository.getStaffs(id)
    }

    override suspend fun getUserAttendances(
        id: String,
        month: String,
        year: String,
    ): Flow<NetworkResult<StaffAttendanceResponse?>> {
        return keepRepository.getUserAttendances(id, month, year)
    }

    override suspend fun markBulkAttendance(
        id: String,
        markAttendanceBody: MarkBulkAttendanceRequestBody,
    ): Flow<NetworkResult<String?>> {
        return keepRepository.markBulkAttendance(id, markAttendanceBody)
    }

    override suspend fun markSingleAttendance(
        id: String,
        markAttendanceBody: MarkSingleAttendanceRequestBody,
    ): Flow<NetworkResult<String?>> {
        return keepRepository.markSingleAttendance(id, markAttendanceBody)
    }

    override suspend fun addStaffUsers(staffUsers: AddStaffUsersRequestBody): Flow<NetworkResult<String?>> {
        return keepRepository.addStaffUsers(staffUsers)
    }

    override suspend fun addStaffUser(staffUser: AddStaffUserRequestBody): Flow<NetworkResult<AddStaffUserResponse?>> {
        return keepRepository.addStaffUser(staffUser)
    }

    override suspend fun deleteStaffUser(id: String): Flow<NetworkResult<String?>> {
        return keepRepository.deleteStaffUser(id)
    }

    override suspend fun updateUserName(
        id: String,
        updateUserNameRequestBody: UpdateUserNameRequestBody,
    ): Flow<NetworkResult<GetUserResponse?>> {
        return keepRepository.updateUserName(id, updateUserNameRequestBody)
    }

    override suspend fun getUser(id: String): Flow<NetworkResult<GetUserResponse?>> {
        return keepRepository.getUser(id)
    }

    override suspend fun addAdvance(
        id: String,
        addAdvanceRequestBody: AddAdvanceRequestBody,
    ): Flow<NetworkResult<String?>> {
        return keepRepository.addAdvance(id, addAdvanceRequestBody)
    }

    override suspend fun addOvertime(userId: String, date: String, otMinutes: Double, otPerHour: Double, managerId: String): Flow<NetworkResult<String?>> {
        return keepRepository.addOvertime(userId, date, otMinutes, otPerHour, managerId)
    }

    override suspend fun deleteOvertime(userId: String, date: String, managerId: String): Flow<NetworkResult<String?>> {
        return keepRepository.deleteOvertime(userId, date, managerId)
    }

    override suspend fun addOrUpdateSalary(userId: String, body: co.dailybook.keep.model.AddOrUpdateSalaryRequestBody): Flow<NetworkResult<String?>> {
        return keepRepository.addOrUpdateSalary(userId, body)
    }

    override suspend fun getUserSalary(userId: String, month: Int, year: Int): Flow<NetworkResult<SalaryData?>> {
        return keepRepository.getUserSalary(userId, month, year)
    }

    override suspend fun getCurrentSalary(userId: String): Flow<NetworkResult<CurrentSalaryResponse?>> {
        return keepRepository.getCurrentSalary(userId)
    }
}