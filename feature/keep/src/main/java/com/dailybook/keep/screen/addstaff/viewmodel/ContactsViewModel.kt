package com.dailybook.keep.screen.addstaff.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import com.boilerplate.network.model.NetworkResultStatus
import com.dailybook.base.BaseViewModel
import com.dailybook.base.Logger
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.keep.model.AddStaffUserRequestBody
import com.dailybook.keep.model.AddStaffUsersRequestBody
import com.dailybook.keep.model.Staff
import com.dailybook.keep.screen.addstaff.model.ContactItem
import com.dailybook.keep.screen.addstaff.uistate.AddStaffUiState
import com.dailybook.keep.usecase.KeepUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ContactsViewModel(val keepUseCase: KeepUseCase) : BaseViewModel<AddStaffUiState>(), KoinComponent {

    private var isNameEntered: Boolean = false
    private var isMobileNumberEntered: Boolean = false

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logger.e("Caught exception: ${exception.localizedMessage}")
        uiState.value = AddStaffUiState.Error(exception.localizedMessage)
    }

    fun loadContacts(context: Context, shouldHardRefresh: Boolean) {
        uiState.value = AddStaffUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contacts = fetchContacts(context, shouldHardRefresh)
                withContext(Dispatchers.Main) {
                    try {
                        uiState.postValue(AddStaffUiState.Success(contacts))
                    } catch (e: Exception) {
                        uiState.postValue(AddStaffUiState.Error("Failed to load contacts"))
                    }
                }
            } catch (e : Exception){
                uiState.postValue(AddStaffUiState.Error("Failed to load contacts"))
            }
        }
    }

    fun addStaffUser(addStaffRequestBody: AddStaffUserRequestBody) =
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            keepUseCase.addStaffUser(addStaffRequestBody).collect(collector = {
                withContext(Dispatchers.Main) {
                    when (it.status) {
                        NetworkResultStatus.SUCCESS -> {
                            uiState.value = it.data?.let { data ->
                                AddStaffUiState.StaffUserAddedSuccess(
                                    data.id,
                                    data.mobileNumber
                                )
                            }
                        }

                        NetworkResultStatus.ERROR -> {
                            uiState.value = AddStaffUiState.StaffAddError(it.message ?: "")
                        }

                        NetworkResultStatus.LOADING -> {
                            uiState.value = AddStaffUiState.Loading
                        }
                    }
                }
            })
        }

    fun attachNameTextWatcher(textView : TextView){
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                isNameEntered = s.toString().isNotEmpty()
                if(isNameEntered && isMobileNumberEntered){
                    uiState.value = AddStaffUiState.EnableAddStaffButton(true)
                } else {
                    uiState.value = AddStaffUiState.EnableAddStaffButton(false)
                }
            }

        }
        textView.addTextChangedListener(textWatcher)
    }

    fun attachMobileNumberTextWatcher(textView : TextView){
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                isMobileNumberEntered = s.toString().length == 10 && Regex("^\\d+\$").matches(s.toString().trim())
                if(isNameEntered && isMobileNumberEntered){
                    uiState.value = AddStaffUiState.EnableAddStaffButton(true)
                } else {
                    uiState.value = AddStaffUiState.EnableAddStaffButton(false)
                }
            }
        }
        textView.addTextChangedListener(textWatcher)
    }

    @SuppressLint("Range")
    private suspend fun fetchContacts(context: Context, shouldHardRefresh: Boolean): List<ContactItem> {
        return keepUseCase.loadContacts(context, shouldHardRefresh)
    }

    fun openNonContactStaff() {
        uiState.value = AddStaffUiState.OpenNonContactStaff(true)
    }
}