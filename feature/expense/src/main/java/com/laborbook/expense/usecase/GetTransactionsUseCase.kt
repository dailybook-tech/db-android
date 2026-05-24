package com.laborbook.expense.usecase

import com.laborbook.expense.repository.TransactionRepository

class GetTransactionsUseCase(val transactionRepository: TransactionRepository) {
    suspend fun invoke(userId: String, month: String, year: String, pageNo: Int) =
        transactionRepository.getTransactions(userId, month, year, pageNo)
}