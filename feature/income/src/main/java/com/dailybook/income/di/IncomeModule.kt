package com.dailybook.income.di

import com.dailybook.income.network.TransactionNetworkModule
import com.dailybook.income.repository.TransactionRepository
import com.dailybook.income.repository.TransactionRepositoryImplementation
import com.dailybook.income.screen.cashentry.viewmodel.CashInOutViewModel
import com.dailybook.income.screen.home.viewmodel.TransactionSummaryViewModel
import com.dailybook.income.screen.home.viewmodel.TransactionsViewModel
import com.dailybook.income.usecase.CreateTransactionUseCase
import com.dailybook.income.usecase.DeleteTransactionUseCase
import com.dailybook.income.usecase.GetTransactionSummaryUseCase
import com.dailybook.income.usecase.GetTransactionsUseCase
import com.dailybook.income.usecase.UpdateTransactionUseCase
import com.dailybook.income.util.IncomeObserverUtil
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val incomeModule = module {
    single { TransactionNetworkModule() }
    single { IncomeObserverUtil() }
    //ViewModel Inject
    viewModel { TransactionsViewModel(get(), get()) }
    viewModel { TransactionSummaryViewModel(get(), get()) }
    viewModel { CashInOutViewModel(get(), get(), get(), get()) }
    //Use Case Inject
    single { GetTransactionsUseCase(get()) }
    single { GetTransactionSummaryUseCase(get()) }
    single { CreateTransactionUseCase(get()) }
    single { UpdateTransactionUseCase(get()) }
    single { DeleteTransactionUseCase(get()) }
    //Repository Inject
    factory<TransactionRepository> { TransactionRepositoryImplementation(get()) }
}