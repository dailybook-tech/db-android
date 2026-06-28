package co.dailybook.auth.di

import co.dailybook.auth.network.AuthNetworkModule
import co.dailybook.auth.repository.AuthRepository
import co.dailybook.auth.repository.AuthRepositoryImplementation
import co.dailybook.auth.screen.login.viewmodel.AuthViewModel
import co.dailybook.auth.usecase.AuthUseCase
import co.dailybook.auth.usecase.AuthUseCaseImplementation
import co.dailybook.base.datastore.DataStoreManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val authModule = module {

    //Network Module
    single { AuthNetworkModule() }

    //ViewModel Inject
    viewModel { AuthViewModel(get()) }

    //Use Case Inject
    factory<AuthUseCase> { AuthUseCaseImplementation(get()) }

    //Repository Inject
    factory<AuthRepository> { AuthRepositoryImplementation(get()) }
}