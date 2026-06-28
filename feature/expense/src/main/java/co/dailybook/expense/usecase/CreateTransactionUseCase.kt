package co.dailybook.expense.usecase

import co.dailybook.expense.model.TransactionRequest
import co.dailybook.expense.repository.TransactionRepository

class CreateTransactionUseCase(private val transactionRepository: TransactionRepository) {
    suspend operator fun invoke(userId: String, transactionRequest: TransactionRequest) = transactionRepository.createTransaction(userId, transactionRequest)
}