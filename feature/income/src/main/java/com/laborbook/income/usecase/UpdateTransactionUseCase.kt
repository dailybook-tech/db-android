package com.laborbook.income.usecase

import com.laborbook.income.model.TransactionRequest
import com.laborbook.income.repository.TransactionRepository

class UpdateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, id: String, transactionRequest: TransactionRequest) = transactionRepository.updateTransaction(userId, id, transactionRequest)
}