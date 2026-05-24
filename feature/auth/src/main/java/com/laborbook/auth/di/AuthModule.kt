package com.laborbook.auth.di

import com.laborbook.auth.network.AuthNetworkModule
import com.laborbook.auth.repository.AuthRepository
import com.laborbook.auth.repository.AuthRepositoryImplementation
import com.laborbook.auth.screen.login.viewmodel.AuthViewModel
import com.laborbook.auth.usecase.AuthUseCase
import com.laborbook.auth.usecase.AuthUseCaseImplementation
import com.laborbook.base.datastore.DataStoreManager
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