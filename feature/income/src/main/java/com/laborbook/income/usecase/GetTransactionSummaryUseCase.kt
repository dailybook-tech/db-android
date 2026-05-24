package com.laborbook.income.usecase

import com.laborbook.income.repository.TransactionRepository

class GetTransactionSummaryUseCase(val transactionRepository: TransactionRepository) {
    suspend fun invoke(userId: String, month: String, year: String) =
        transactionRepository.getTransactionSummary(userId, month, year)
}