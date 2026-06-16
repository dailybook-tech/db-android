package com.dailybook.auth.di

import com.dailybook.auth.network.AuthNetworkModule
import com.dailybook.auth.repository.AuthRepository
import com.dailybook.auth.repository.AuthRepositoryImplementation
import com.dailybook.auth.screen.login.viewmodel.AuthViewModel
import com.dailybook.auth.usecase.AuthUseCase
import com.dailybook.auth.usecase.AuthUseCaseImplementation
import com.dailybook.base.datastore.DataStoreManager
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