package com.dailybook.income.util

import com.dailybook.income.model.DeleteTransactionResponseModel
import com.dailybook.income.model.Transaction

class IncomeObserverUtil {
    var onIncomeAddedOrUpdated: ((Transaction, Boolean) -> Unit)? = null
    var onIncomeDeleted: ((DeleteTransactionResponseModel) -> Unit)? = null
    var clearIncomeSearchText : ((shouldClear: Boolean) -> Unit)? = null
}