package com.laborbook.keep.di

import com.laborbook.keep.database.AppDatabase
import com.laborbook.keep.network.KeepNetworkModule
import com.laborbook.keep.repository.KeepRepository
import com.laborbook.keep.repository.KeepRepositoryImplementation
import com.laborbook.keep.screen.addstaff.model.ContactDatabase
import com.laborbook.keep.screen.addstaff.viewmodel.ContactsViewModel
import com.laborbook.keep.screen.advance.viewmodel.AddAdvanceViewModel
import com.laborbook.keep.screen.calendar.utils.ObserverUtil
import com.laborbook.keep.screen.calendar.viewmodel.CalendarViewModel
import com.laborbook.keep.screen.calendar.viewmodel.OvertimeViewModel
import com.laborbook.keep.screen.deletestaff.viewmodel.DeleteStaffViewModel
import com.laborbook.keep.screen.home.viewmodel.StaffsViewModel
import com.laborbook.keep.screen.profile.viewmodel.UserProfileViewModel
import com.laborbook.keep.screen.profile.viewmodel.EditProfileViewModel
import com.laborbook.keep.screen.premium.PremiumOfferManager
import com.laborbook.keep.screen.premium.viewmodel.SubscriptionViewModel
import com.laborbook.keep.screen.teams.TeamViewModel
import com.laborbook.keep.repository.SubscriptionRepository
import com.laborbook.keep.repository.SubscriptionRepositoryImpl
import com.laborbook.keep.usecase.KeepUseCase
import com.laborbook.keep.usecase.KeepUseCaseImplementation
import com.laborbook.keep.usecase.SubscriptionUseCase
import com.laborbook.keep.usecase.SubscriptionUseCaseImpl
import com.laborbook.keep.utils.CoachMarkManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val keepModule = module {

    //Network Module
    single { KeepNetworkModule(AppDatabase.getDatabase(androidContext())) }
    single { ObserverUtil() }
    single { CoachMarkManager() }
    single { PremiumOfferManager(get()) }
    //ViewModel Inject
    viewModel { ContactsViewModel(get()) }
    viewModel { StaffsViewModel(get()) }
    viewModel { CalendarViewModel(get()) }
    viewModel { UserProfileViewModel(get()) }
    viewModel { AddAdvanceViewModel(get()) }
    viewModel { DeleteStaffViewModel(get()) }
    viewModel { OvertimeViewModel(get()) }
    viewModel { EditProfileViewModel(get()) }
    viewModel { SubscriptionViewModel(get(), get(), get()) }
    viewModel { TeamViewModel(get()) }
    
    //Use Case Inject
    factory<KeepUseCase> { KeepUseCaseImplementation(get()) }
    factory<SubscriptionUseCase> { SubscriptionUseCaseImpl(get()) }
    
    //Repository Inject
    factory<KeepRepository> { KeepRepositoryImplementation(get(), ContactDatabase.getDatabase(androidContext()).contactDao()) }
    factory<SubscriptionRepository> { SubscriptionRepositoryImpl(get()) }
}