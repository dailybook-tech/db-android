package co.dailybook.expense.usecase

import co.dailybook.expense.model.TransactionRequest
import co.dailybook.expense.repository.TransactionRepository

class UpdateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, id: String, transactionRequest: TransactionRequest) = transactionRepository.updateTransaction(userId, id, transactionRequest)
}