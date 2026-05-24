package com.laborbook.income.usecase

import com.laborbook.income.model.TransactionRequest
import com.laborbook.income.repository.TransactionRepository

class CreateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, transactionRequest: TransactionRequest) = transactionRepository.createTransaction(userId, transactionRequest)
}