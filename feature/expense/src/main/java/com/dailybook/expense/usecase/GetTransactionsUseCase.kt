package com.dailybook.expense.usecase

import com.dailybook.expense.repository.TransactionRepository

class GetTransactionsUseCase(val transactionRepository: TransactionRepository) {
    suspend fun invoke(userId: String, month: String, year: String, pageNo: Int) =
        transactionRepository.getTransactions(userId, month, year, pageNo)
}