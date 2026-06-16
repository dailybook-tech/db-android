package com.dailybook.income.usecase

import com.dailybook.income.repository.TransactionRepository

class GetTransactionSummaryUseCase(val transactionRepository: TransactionRepository) {
    suspend fun invoke(userId: String, month: String, year: String) =
        transactionRepository.getTransactionSummary(userId, month, year)
}