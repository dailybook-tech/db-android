package com.laborbook.expense.di

import com.laborbook.expense.network.TransactionNetworkModule
import com.laborbook.expense.repository.TransactionRepository
import com.laborbook.expense.repository.TransactionRepositoryImplementation
import com.laborbook.expense.screen.cashentry.viewmodel.CashInOutViewModel
import com.laborbook.expense.screen.home.viewmodel.TransactionSummaryViewModel
import com.laborbook.expense.screen.home.viewmodel.TransactionsViewModel
import com.laborbook.expense.usecase.CreateTransactionUseCase
import com.laborbook.expense.usecase.DeleteTransactionUseCase
import com.laborbook.expense.usecase.GetTransactionSummaryUseCase
import com.laborbook.expense.usecase.GetTransactionsUseCase
import com.laborbook.expense.usecase.UpdateTransactionUseCase
import com.laborbook.expense.util.ExpenseObserverUtil
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