package com.laborbook.auth.screen.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.boilerplate.network.model.NetworkResult
import com.boilerplate.network.model.NetworkResultStatus
import com.laborbook.auth.common.getOrAwaitValue
import com.laborbook.auth.model.request.AuthRequestBody
import com.laborbook.auth.model.request.AuthResponse
import com.laborbook.auth.model.request.TruecallerRequestBody
import com.laborbook.auth.model.request.User
import com.laborbook.auth.screen.login.uistate.UiState
import com.laborbook.auth.screen.login.viewmodel.AuthViewModel
import com.laborbook.auth.usecase.AuthUseCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AuthViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule() // Ensures LiveData can be tested

    private lateinit var viewModel: AuthViewModel
    private val authUseCase: AuthUseCase = mock(AuthUseCase::class.java)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(authUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGenerateOtp_Success() = runTest {
        // Given
        val requestBody = AuthRequestBody("91","9090909090")
        val successFlow = flow {
            emit(NetworkResult(NetworkResultStatus.SUCCESS, Unit, null))
        }

        `when`(authUseCase.generateOtp(requestBody)).thenReturn(successFlow)

        // When
        viewModel.generateOtp(requestBody)

        // Then
        assertEquals(UiState.OtpSent("Otp Sent"), viewModel.uiState().getOrAwaitValue())
    }

    @Test
    fun testGenerateOtp_Error() = runTest {
        // Given
        val requestBody = AuthRequestBody("91","90909090900")
        val errorFlow = flow {
            emit(NetworkResult(NetworkResultStatus.ERROR, null, "Invalid mobile number"))
        }

        `when`(authUseCase.generateOtp(requestBody)).thenReturn(errorFlow)

        // When
        viewModel.generateOtp(requestBody)

        // Then
        assertEquals(UiState.Error("Invalid mobile number"), viewModel.uiState().getOrAwaitValue())
    }

    @Test
    fun testResendOtp_Success() = runTest {
        // Given
        val requestBody = AuthRequestBody("91","9090909090")
        val successFlow = flow {
            emit(NetworkResult(NetworkResultStatus.SUCCESS, Unit, null))
        }

        `when`(authUseCase.resendOtp(requestBody)).thenReturn(successFlow)

        // When
        viewModel.resendOtp(requestBody)

        // Then
        assertEquals(UiState.OtpSent("Otp Re-Sent"), viewModel.uiState().getOrAwaitValue())
    }

    @Test
    fun testResendOtp_Error() = runTest {
        // Given
        val requestBody = AuthRequestBody("91","90909090900")
        val errorFlow = flow {
            emit(NetworkResult(NetworkResultStatus.ERROR, null, "Invalid mobile number"))
        }

        `when`(authUseCase.generateOtp(requestBody)).thenReturn(errorFlow)

        // When
        viewModel.generateOtp(requestBody)

        // Then
        assertEquals(UiState.Error("Invalid mobile number"), viewModel.uiState().getOrAwaitValue())
    }

    @Test
    fun testVerifyOtp_Success() = runTest {
        //Given
        val requestBody = AuthRequestBody("91","9090909090","4242")
        val responseBody = AuthResponse("authtoken",
            "BEARER", User("userid", "Hari", "MANAGER", "9090909090","companyid"))
        val successFlow = flow {
            emit(NetworkResult(NetworkResultStatus.SUCCESS, responseBody, ""))
        }

        `when`(authUseCase.verifyOtp(requestBody)).thenReturn(successFlow)

        //When
        viewModel.verifyOtp(requestBody)

        //Then
        assertEquals(UiState.OtpVerified(responseBody), viewModel.uiState().getOrAwaitValue())
    }

    @Test
    fun `verifyOtp error`() = runTest {
        //Given
        val requestBody = AuthRequestBody("91","9090909090","1234")
        val errorFlow = flow {
            emit(NetworkResult(NetworkResultStatus.ERROR, null, "Invalid OTP"))
        }

        `when`(authUseCase.verifyOtp(requestBody)).thenReturn(errorFlow)

        //When
        viewModel.verifyOtp(requestBody)

        //Then
        assertEquals(UiState.Error("Invalid OTP"), viewModel.uiState().getOrAwaitValue())
    }

    @Test
    fun `truecallerLogin success`() = runTest {
        // Given
        val requestBody = TruecallerRequestBody("abcd","abcd")
        val responseBody = AuthResponse("authtoken",
            "BEARER", User("userid", "Hari", "MANAGER", "9090909090","companyid"))
        val successFlow = flow {
            emit(NetworkResult(NetworkResultStatus.SUCCESS, responseBody, ""))
        }

        `when`(authUseCase.truecallerLogin(requestBody)).thenReturn(successFlow)

        // When
        viewModel.truecallerLogin(requestBody)

        // Then
        assertEquals(UiState.TrueCallerLoginSuccess(responseBody), viewModel.uiState().getOrAwaitValue())
    }

    @Test
    fun `truecallerLogin error`() = runTest {
        //Given
        val requestBody = TruecallerRequestBody("abcdef","abcdef")
        val errorFlow = flow {
            emit(NetworkResult(NetworkResultStatus.ERROR, null, "Verification Failed"))
        }

        `when`(authUseCase.truecallerLogin(requestBody)).thenReturn(errorFlow)

        //When
        viewModel.truecallerLogin(requestBody)

        //Then
        assertEquals(UiState.Error("Verification Failed"), viewModel.uiState().getOrAwaitValue())
    }
}