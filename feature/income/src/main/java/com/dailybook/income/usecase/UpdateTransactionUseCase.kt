package com.dailybook.income.usecase

import com.dailybook.income.model.TransactionRequest
import com.dailybook.income.repository.TransactionRepository

class UpdateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, id: String, transactionRequest: TransactionRequest) = transactionRepository.updateTransaction(userId, id, transactionRequest)
}