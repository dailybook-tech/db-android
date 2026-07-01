package com.laborbook.auth.screen.login.viewmodel

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthProvider
import com.boilerplate.network.model.NetworkResultStatus
import com.laborbook.auth.model.request.AuthRequestBody
import com.laborbook.auth.model.request.TruecallerRequestBody
import com.laborbook.auth.screen.login.uistate.UiState
import com.laborbook.auth.usecase.AuthUseCase
import com.laborbook.base.BaseViewModel
import com.laborbook.base.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(val authUseCase: AuthUseCase) : BaseViewModel<UiState>() {

    val PHONE_NUMBER_REQUEST = 12
    private var isFirebaseOtpFlow = false
    private var firebaseVerificationId: String? = null
    private var firebaseResendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var prefilledOtpCode: String? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logger.e("Caught exception: ${exception.localizedMessage}")
        uiState.value = UiState.Error(exception.localizedMessage)
    }

    fun setOtpProvider(useFirebase: Boolean) {
        isFirebaseOtpFlow = useFirebase
        if (!useFirebase) {
            firebaseVerificationId = null
            firebaseResendToken = null
            prefilledOtpCode = null
        }
    }

    fun usesFirebaseOtp(): Boolean = isFirebaseOtpFlow

    fun setFirebaseOtpSession(
        verificationId: String,
        resendToken: PhoneAuthProvider.ForceResendingToken?,
        otpCode: String? = null,
    ) {
        isFirebaseOtpFlow = true
        firebaseVerificationId = verificationId
        firebaseResendToken = resendToken
        prefilledOtpCode = otpCode
    }

    fun getFirebaseVerificationId(): String? = firebaseVerificationId

    fun getFirebaseResendToken(): PhoneAuthProvider.ForceResendingToken? = firebaseResendToken

    fun consumePrefilledOtpCode(): String? {
        val otpCode = prefilledOtpCode
        prefilledOtpCode = null
        return otpCode
    }

    fun expectedOtpLength(): Int = if (isFirebaseOtpFlow) 6 else 4

    fun showOtpSent(message: String) {
        uiState.value = UiState.OtpSent(message)
    }

    fun showError(message: String?) {
        uiState.value = UiState.Error(message)
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

    fun verifyFirebaseOtp(authRequestBody: AuthRequestBody) = viewModelScope.launch(exceptionHandler + Dispatchers.IO){
        authUseCase.verifyFirebaseOtp(authRequestBody).collect(collector = {
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
                uiState.value = UiState.OtpEntered(s.toString().length == expectedOtpLength())
            }

        }
        textView.addTextChangedListener(textWatcher)
    }
}
