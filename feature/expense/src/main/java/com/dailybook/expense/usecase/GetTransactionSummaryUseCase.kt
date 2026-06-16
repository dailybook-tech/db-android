package com.dailybook.expense.usecase

import com.dailybook.expense.repository.TransactionRepository

class GetTransactionSummaryUseCase(val transactionRepository: TransactionRepository) {
    suspend fun invoke(userId: String, month: String, year: String) =
        transactionRepository.getTransactionSummary(userId, month, year)
}