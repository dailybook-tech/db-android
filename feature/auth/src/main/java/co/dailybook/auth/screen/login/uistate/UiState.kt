package co.dailybook.auth.screen.login.uistate

import co.dailybook.auth.model.request.AuthResponse

sealed class UiState {
    data object Loading : UiState()
    data class NumberEntered(val isValidNumber : Boolean) : UiState()
    data class OtpSent(val message : String) : UiState()
    data class OtpEntered(val isValidOtp : Boolean) : UiState()
    data class OtpVerified(val authResponse: AuthResponse?) : UiState()
    data class TrueCallerLoginSuccess(val authResponse: AuthResponse?) : UiState()
    data class Error(val message: String?) : UiState()
}