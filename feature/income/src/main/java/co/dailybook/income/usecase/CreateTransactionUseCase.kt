package co.dailybook.income.usecase

import co.dailybook.income.model.TransactionRequest
import co.dailybook.income.repository.TransactionRepository

class CreateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, transactionRequest: TransactionRequest) = transactionRepository.createTransaction(userId, transactionRequest)
}