package com.laborbook.income.util

import com.laborbook.income.model.DeleteTransactionResponseModel
import com.laborbook.income.model.Transaction

class IncomeObserverUtil {
    var onIncomeAddedOrUpdated: ((Transaction, Boolean) -> Unit)? = null
    var onIncomeDeleted: ((DeleteTransactionResponseModel) -> Unit)? = null
    var clearIncomeSearchText : ((shouldClear: Boolean) -> Unit)? = null
}