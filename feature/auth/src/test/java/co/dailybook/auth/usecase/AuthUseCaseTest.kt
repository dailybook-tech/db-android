package co.dailybook.auth.usecase

import co.dailybook.boilerplate.network.model.NetworkResult
import co.dailybook.boilerplate.network.model.NetworkResultStatus
import co.dailybook.auth.model.request.AuthRequestBody
import co.dailybook.auth.repository.AuthRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
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
class AuthUseCaseTest {

    private lateinit var repo: AuthRepository
    private lateinit var useCase: AuthUseCase

    @Before
    fun setUp() {
        repo = mock(AuthRepository::class.java)
        useCase = AuthUseCaseImplementation(repo)
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGenerateOtp_Success() = runTest {
        //Given
        val requestBody = AuthRequestBody("91","9090909090")
        val successFlow = flow {
            emit(NetworkResult(NetworkResultStatus.SUCCESS, Unit, null))
        }

        `when`(repo.generateOtp(requestBody)).thenReturn(successFlow)

        //When
        val resultFlow = useCase.generateOtp(requestBody)

        //Then
        resultFlow.collect { result ->
            assertEquals(NetworkResult(NetworkResultStatus.SUCCESS, Unit, null), result)
        }

        verify(repo, times(1)).generateOtp(requestBody)
    }

    @Test
    fun testGenerateOtp_Failure() = runTest {
        //Given
        val requestBody = AuthRequestBody("91","9090909090")
        val successFlow = flow {
            emit(NetworkResult(NetworkResultStatus.ERROR, Unit, "Invalid mobile number"))
        }

        `when`(repo.generateOtp(requestBody)).thenReturn(successFlow)

        //When
        val resultFlow = useCase.generateOtp(requestBody)

        //Then
        resultFlow.collect { result ->
            assertEquals(NetworkResult(NetworkResultStatus.ERROR, Unit, "Invalid mobile number"), result)
        }

        verify(repo, times(1)).generateOtp(requestBody)
    }
}