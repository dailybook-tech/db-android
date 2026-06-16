package com.dailybook.keep.screen.addstaff.uistate

import com.dailybook.keep.model.Staff
import com.dailybook.keep.screen.addstaff.model.ContactItem

sealed class AddStaffUiState {
    data object Loading : AddStaffUiState()
    data class Success(val contacts: List<ContactItem>) : AddStaffUiState()
    data class StaffUserAddedSuccess(val id: String, val mobileNumber: String) : AddStaffUiState()
    data class Error(val message: String) : AddStaffUiState()
    data class StaffAddError(val message: String) : AddStaffUiState()
    data class EnableAddStaffButton(val isDetailsEntered: Boolean) : AddStaffUiState()
    data class OpenNonContactStaff(val isOpen: Boolean) : AddStaffUiState()
}