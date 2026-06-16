package com.dailybook.auth.repository

import com.boilerplate.network.NetworkHandler
import com.boilerplate.network.model.NetworkResult
import com.boilerplate.network.model.NetworkResultStatus
import com.dailybook.auth.model.request.AuthRequestBody
import com.dailybook.auth.network.AuthNetworkModule
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AuthRepositoryTest {
    private lateinit var networkModule: AuthNetworkModule
    private lateinit var repo: AuthRepository
    private lateinit var networkHandler: NetworkHandler

    @Before
    fun setup() {
        networkModule = mock(AuthNetworkModule::class.java)
        networkHandler = mock(NetworkHandler::class.java)
        networkModule.networkHandler = this.networkHandler
        repo = AuthRepositoryImplementation(networkModule)
    }

    @Test
    fun testGenerateOtp_Success() = runTest{
        val requestBody = AuthRequestBody("91","9090909090")
        val successFlow = flow {
            emit(NetworkResult(NetworkResultStatus.SUCCESS, Unit, null))
        }

        `when`(networkModule.generateOtp(requestBody)).thenReturn(successFlow)

        //When
        val resultFlow = repo.generateOtp(requestBody)

        //Then
        resultFlow.collect { result ->
            assertEquals(NetworkResult(NetworkResultStatus.SUCCESS, Unit, null), result)
        }

        verify(networkModule, times(1)).generateOtp(requestBody)
    }

    @Test
    fun testGenerateOtp_Failure() = runTest{
        val requestBody = AuthRequestBody("91","9090909090")
        val failureFlow = flow {
            emit(NetworkResult(NetworkResultStatus.ERROR, Unit, "Invalid mobile number"))
        }

        `when`(networkModule.generateOtp(requestBody)).thenReturn(failureFlow)

        //When
        val resultFlow = repo.generateOtp(requestBody)

        //Then
        resultFlow.collect { result ->
            assertEquals(NetworkResult(NetworkResultStatus.ERROR, Unit, "Invalid mobile number"), result)
        }

        verify(networkModule, times(1)).generateOtp(requestBody)
    }
}