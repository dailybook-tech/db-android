package com.laborbook.income.di

import com.laborbook.income.network.TransactionNetworkModule
import com.laborbook.income.repository.TransactionRepository
import com.laborbook.income.repository.TransactionRepositoryImplementation
import com.laborbook.income.screen.cashentry.viewmodel.CashInOutViewModel
import com.laborbook.income.screen.home.viewmodel.TransactionSummaryViewModel
import com.laborbook.income.screen.home.viewmodel.TransactionsViewModel
import com.laborbook.income.usecase.CreateTransactionUseCase
import com.laborbook.income.usecase.DeleteTransactionUseCase
import com.laborbook.income.usecase.GetTransactionSummaryUseCase
import com.laborbook.income.usecase.GetTransactionsUseCase
import com.laborbook.income.usecase.UpdateTransactionUseCase
import com.laborbook.income.util.IncomeObserverUtil
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