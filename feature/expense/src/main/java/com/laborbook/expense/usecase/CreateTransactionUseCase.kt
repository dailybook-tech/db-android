package com.laborbook.expense.usecase

import com.laborbook.expense.model.TransactionRequest
import com.laborbook.expense.repository.TransactionRepository

class CreateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, transactionRequest: TransactionRequest) = transactionRepository.createTransaction(userId, transactionRequest)
}