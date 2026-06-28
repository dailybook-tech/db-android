package co.dailybook.expense.di

import co.dailybook.expense.network.TransactionNetworkModule
import co.dailybook.expense.repository.TransactionRepository
import co.dailybook.expense.repository.TransactionRepositoryImplementation
import co.dailybook.expense.screen.cashentry.viewmodel.CashInOutViewModel
import co.dailybook.expense.screen.home.viewmodel.TransactionSummaryViewModel
import co.dailybook.expense.screen.home.viewmodel.TransactionsViewModel
import co.dailybook.expense.usecase.CreateTransactionUseCase
import co.dailybook.expense.usecase.DeleteTransactionUseCase
import co.dailybook.expense.usecase.GetTransactionSummaryUseCase
import co.dailybook.expense.usecase.GetTransactionsUseCase
import co.dailybook.expense.usecase.UpdateTransactionUseCase
import co.dailybook.expense.util.ExpenseObserverUtil
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