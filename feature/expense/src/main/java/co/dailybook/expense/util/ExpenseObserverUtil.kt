package co.dailybook.expense.util

import co.dailybook.expense.model.DeleteTransactionResponseModel
import co.dailybook.expense.model.Transaction

class ExpenseObserverUtil {
    var onExpenseAddedOrUpdated: ((Transaction, Boolean) -> Unit)? = null
    var onExpenseDeleted: ((DeleteTransactionResponseModel) -> Unit)? = null
    var clearExpenseSearchText : ((shouldClear: Boolean) -> Unit)? = null
}