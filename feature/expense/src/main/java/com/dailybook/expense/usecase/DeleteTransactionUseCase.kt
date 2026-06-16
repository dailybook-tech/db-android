package com.dailybook.expense.usecase

import com.dailybook.expense.repository.TransactionRepository

class DeleteTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, id: String) = transactionRepository.deleteTransaction(userId, id)
}