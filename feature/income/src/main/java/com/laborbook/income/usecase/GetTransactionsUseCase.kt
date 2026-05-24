package com.laborbook.income.usecase

import com.laborbook.income.repository.TransactionRepository

class GetTransactionsUseCase(val transactionRepository: TransactionRepository) {
    suspend fun invoke(userId: String, month: String, year: String, pageNo: Int) =
        transactionRepository.getTransactions(userId, month, year, pageNo)
}