package com.dailybook.income.usecase

import com.dailybook.income.model.TransactionRequest
import com.dailybook.income.repository.TransactionRepository

class CreateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, transactionRequest: TransactionRequest) = transactionRepository.createTransaction(userId, transactionRequest)
}