package co.dailybook.keep.di

import co.dailybook.keep.database.AppDatabase
import co.dailybook.keep.network.KeepNetworkModule
import co.dailybook.keep.repository.KeepRepository
import co.dailybook.keep.repository.KeepRepositoryImplementation
import co.dailybook.keep.screen.addstaff.model.ContactDatabase
import co.dailybook.keep.screen.addstaff.viewmodel.ContactsViewModel
import co.dailybook.keep.screen.advance.viewmodel.AddAdvanceViewModel
import co.dailybook.keep.screen.calendar.utils.ObserverUtil
import co.dailybook.keep.screen.calendar.viewmodel.CalendarViewModel
import co.dailybook.keep.screen.calendar.viewmodel.OvertimeViewModel
import co.dailybook.keep.screen.deletestaff.viewmodel.DeleteStaffViewModel
import co.dailybook.keep.screen.home.viewmodel.StaffsViewModel
import co.dailybook.keep.screen.profile.viewmodel.UserProfileViewModel
import co.dailybook.keep.screen.profile.viewmodel.EditProfileViewModel
import co.dailybook.keep.screen.premium.PremiumOfferManager
import co.dailybook.keep.screen.premium.viewmodel.SubscriptionViewModel
import co.dailybook.keep.screen.teams.TeamViewModel
import co.dailybook.keep.repository.SubscriptionRepository
import co.dailybook.keep.repository.SubscriptionRepositoryImpl
import co.dailybook.keep.usecase.KeepUseCase
import co.dailybook.keep.usecase.KeepUseCaseImplementation
import co.dailybook.keep.usecase.SubscriptionUseCase
import co.dailybook.keep.usecase.SubscriptionUseCaseImpl
import co.dailybook.keep.utils.CoachMarkManager
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