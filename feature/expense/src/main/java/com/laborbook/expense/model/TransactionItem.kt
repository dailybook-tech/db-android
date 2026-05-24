package com.laborbook.expense.model

/**
 * Represents items in the RecyclerView, either as an Expense, a Date Header, or an Ad.
 */
sealed class TransactionItem {
    object TableHeader : TransactionItem()
    data class TransactionItemView(val transaction: Transaction) : TransactionItem()
    data class DateHeader(val date: String, val entryCount: Int) : TransactionItem()
    data class AdItem(val adPosition: Int) : TransactionItem() // Use position to make each ad unique
}