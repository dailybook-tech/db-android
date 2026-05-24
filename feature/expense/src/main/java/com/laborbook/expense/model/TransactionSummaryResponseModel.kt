package com.laborbook.expense.model

import com.google.gson.annotations.SerializedName

data class TransactionSummaryResponseModel(
    @SerializedName("total_debit")
    val totalDebit: Double,
    @SerializedName("total_credit")
    val totalCredit: Double,
    @SerializedName("total_entries_count")
    val totalEntriesCount: Int,
)
