package com.dailybook.expense.usecase

import com.dailybook.expense.model.TransactionRequest
import com.dailybook.expense.repository.TransactionRepository

class UpdateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, id: String, transactionRequest: TransactionRequest) = transactionRepository.updateTransaction(userId, id, transactionRequest)
}