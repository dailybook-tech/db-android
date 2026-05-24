package com.laborbook.expense.util

import com.laborbook.expense.model.DeleteTransactionResponseModel
import com.laborbook.expense.model.Transaction

class ExpenseObserverUtil {
    var onExpenseAddedOrUpdated: ((Transaction, Boolean) -> Unit)? = null
    var onExpenseDeleted: ((DeleteTransactionResponseModel) -> Unit)? = null
    var clearExpenseSearchText : ((shouldClear: Boolean) -> Unit)? = null
}