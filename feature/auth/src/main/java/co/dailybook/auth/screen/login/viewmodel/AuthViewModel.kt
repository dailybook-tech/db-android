package co.dailybook.auth.screen.login.viewmodel

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import co.dailybook.boilerplate.network.model.NetworkResultStatus
import co.dailybook.auth.model.request.AuthRequestBody
import co.dailybook.auth.model.request.TruecallerRequestBody
import co.dailybook.auth.screen.login.uistate.UiState
import co.dailybook.auth.usecase.AuthUseCase
import co.dailybook.base.BaseViewModel
import co.dailybook.base.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(val authUseCase: AuthUseCase) : BaseViewModel<UiState>() {

    val PHONE_NUMBER_REQUEST = 12

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logger.e("Caught exception: ${exception.localizedMessage}")
        uiState.value = UiState.Error(exception.localizedMessage)
    }

    fun generateOtp(authRequestBody: AuthRequestBody) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        authUseCase.generateOtp(authRequestBody).collect(collector = {
            withContext(Dispatchers.Main) {
                when (it.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = UiState.OtpSent("Otp Sent")
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value = UiState.Error(it.message)
                    }

                    NetworkResultStatus.LOADING -> {
                        uiState.value = UiState.Loading
                    }
                }
            }
        })
    }

    fun resendOtp(authRequestBody: AuthRequestBody) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        authUseCase.resendOtp(authRequestBody).collect(collector = {
            withContext(Dispatchers.Main) {
                when (it.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = UiState.OtpSent("Otp Re-Sent")
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value = UiState.Error(it.message)
                    }

                    NetworkResultStatus.LOADING -> {
                        uiState.value = UiState.Loading
                    }
                }
            }
        })
    }

    fun verifyOtp(authRequestBody: AuthRequestBody) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        authUseCase.verifyOtp(authRequestBody).collect(collector = {
            withContext(Dispatchers.Main) {
                when (it.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = UiState.OtpVerified(it.data)
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value = UiState.Error(it.message)
                    }

                    NetworkResultStatus.LOADING -> {
                        uiState.value = UiState.Loading
                    }
                }
            }
        })
    }

    fun truecallerLogin(truecallerRequestBody: TruecallerRequestBody) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        authUseCase.truecallerLogin(truecallerRequestBody).collect(collector = {
            withContext(Dispatchers.Main) {
                when (it.status) {
                    NetworkResultStatus.SUCCESS -> {
                        uiState.value = UiState.TrueCallerLoginSuccess(it.data)
                    }

                    NetworkResultStatus.ERROR -> {
                        uiState.value = UiState.Error(it.message)
                    }

                    NetworkResultStatus.LOADING -> {
                        uiState.value = UiState.Loading
                    }
                }
            }
        })
    }

    fun attachPhoneNumberTextWatcher(textView : TextView){
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                uiState.value = UiState.NumberEntered(s.toString().length == 10 && Regex("^\\d+\$").matches(s.toString().trim()))
            }

        }
        textView.addTextChangedListener(textWatcher)
    }

    fun attachOtpTextWatcher(textView : TextView){
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                uiState.value = UiState.OtpEntered(s.toString().length == 4)
            }

        }
        textView.addTextChangedListener(textWatcher)
    }
}