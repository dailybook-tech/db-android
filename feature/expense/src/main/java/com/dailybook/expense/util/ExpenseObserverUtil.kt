package com.dailybook.expense.util

import com.dailybook.expense.model.DeleteTransactionResponseModel
import com.dailybook.expense.model.Transaction

class ExpenseObserverUtil {
    var onExpenseAddedOrUpdated: ((Transaction, Boolean) -> Unit)? = null
    var onExpenseDeleted: ((DeleteTransactionResponseModel) -> Unit)? = null
    var clearExpenseSearchText : ((shouldClear: Boolean) -> Unit)? = null
}