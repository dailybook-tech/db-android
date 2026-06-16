package com.dailybook.income.usecase

import com.dailybook.income.repository.TransactionRepository

class DeleteTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, id: String) = transactionRepository.deleteTransaction(userId, id)
}