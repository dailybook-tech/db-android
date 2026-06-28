package co.dailybook.expense.usecase

import co.dailybook.expense.repository.TransactionRepository

class GetTransactionSummaryUseCase(val transactionRepository: TransactionRepository) {
    suspend fun invoke(userId: String, month: String, year: String) =
        transactionRepository.getTransactionSummary(userId, month, year)
}