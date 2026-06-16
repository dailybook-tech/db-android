package com.dailybook.keep.di

import com.dailybook.keep.database.AppDatabase
import com.dailybook.keep.network.KeepNetworkModule
import com.dailybook.keep.repository.KeepRepository
import com.dailybook.keep.repository.KeepRepositoryImplementation
import com.dailybook.keep.screen.addstaff.model.ContactDatabase
import com.dailybook.keep.screen.addstaff.viewmodel.ContactsViewModel
import com.dailybook.keep.screen.advance.viewmodel.AddAdvanceViewModel
import com.dailybook.keep.screen.calendar.utils.ObserverUtil
import com.dailybook.keep.screen.calendar.viewmodel.CalendarViewModel
import com.dailybook.keep.screen.calendar.viewmodel.OvertimeViewModel
import com.dailybook.keep.screen.deletestaff.viewmodel.DeleteStaffViewModel
import com.dailybook.keep.screen.home.viewmodel.StaffsViewModel
import com.dailybook.keep.screen.profile.viewmodel.UserProfileViewModel
import com.dailybook.keep.screen.profile.viewmodel.EditProfileViewModel
import com.dailybook.keep.screen.premium.PremiumOfferManager
import com.dailybook.keep.screen.premium.viewmodel.SubscriptionViewModel
import com.dailybook.keep.screen.teams.TeamViewModel
import com.dailybook.keep.repository.SubscriptionRepository
import com.dailybook.keep.repository.SubscriptionRepositoryImpl
import com.dailybook.keep.usecase.KeepUseCase
import com.dailybook.keep.usecase.KeepUseCaseImplementation
import com.dailybook.keep.usecase.SubscriptionUseCase
import com.dailybook.keep.usecase.SubscriptionUseCaseImpl
import com.dailybook.keep.utils.CoachMarkManager
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