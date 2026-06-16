package com.dailybook.expense.usecase

import com.dailybook.expense.model.TransactionRequest
import com.dailybook.expense.repository.TransactionRepository

class CreateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, transactionRequest: TransactionRequest) = transactionRepository.createTransaction(userId, transactionRequest)
}