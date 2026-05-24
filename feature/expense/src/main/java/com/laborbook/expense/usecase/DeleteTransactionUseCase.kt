package com.laborbook.expense.usecase

import com.laborbook.expense.repository.TransactionRepository

class DeleteTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, id: String) = transactionRepository.deleteTransaction(userId, id)
}