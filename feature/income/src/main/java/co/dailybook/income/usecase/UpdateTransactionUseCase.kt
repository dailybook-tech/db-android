package co.dailybook.income.usecase

import co.dailybook.income.model.TransactionRequest
import co.dailybook.income.repository.TransactionRepository

class UpdateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, id: String, transactionRequest: TransactionRequest) = transactionRepository.updateTransaction(userId, id, transactionRequest)
}