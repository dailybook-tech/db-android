package co.dailybook.income.di

import co.dailybook.income.network.TransactionNetworkModule
import co.dailybook.income.repository.TransactionRepository
import co.dailybook.income.repository.TransactionRepositoryImplementation
import co.dailybook.income.screen.cashentry.viewmodel.CashInOutViewModel
import co.dailybook.income.screen.home.viewmodel.TransactionSummaryViewModel
import co.dailybook.income.screen.home.viewmodel.TransactionsViewModel
import co.dailybook.income.usecase.CreateTransactionUseCase
import co.dailybook.income.usecase.DeleteTransactionUseCase
import co.dailybook.income.usecase.GetTransactionSummaryUseCase
import co.dailybook.income.usecase.GetTransactionsUseCase
import co.dailybook.income.usecase.UpdateTransactionUseCase
import co.dailybook.income.util.IncomeObserverUtil
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