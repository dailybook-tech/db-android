package com.dailybook.expense.di

import com.dailybook.expense.network.TransactionNetworkModule
import com.dailybook.expense.repository.TransactionRepository
import com.dailybook.expense.repository.TransactionRepositoryImplementation
import com.dailybook.expense.screen.cashentry.viewmodel.CashInOutViewModel
import com.dailybook.expense.screen.home.viewmodel.TransactionSummaryViewModel
import com.dailybook.expense.screen.home.viewmodel.TransactionsViewModel
import com.dailybook.expense.usecase.CreateTransactionUseCase
import com.dailybook.expense.usecase.DeleteTransactionUseCase
import com.dailybook.expense.usecase.GetTransactionSummaryUseCase
import com.dailybook.expense.usecase.GetTransactionsUseCase
import com.dailybook.expense.usecase.UpdateTransactionUseCase
import com.dailybook.expense.util.ExpenseObserverUtil
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val expenseModule = module {
    single { TransactionNetworkModule() }
    single { ExpenseObserverUtil() }
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