package com.dailybook.base.analytics

class ConstantEventAttributes {
    companion object {
        const val EVENT_TYPE = "event_type"
        const val EVENT_VALUE = "event_value"

        //Super properties
        const val USER_ID = "user_id"
        const val USER_MOBILE_NUMBER = "user_mobile_number"
        const val USER_NAME = "user_name"
        const val USER_TYPE = "user_type"
        const val APP_VERSION = "app_version"
        const val APP_OPEN_COUNT = "app_open_count"
        const val SELECTED_LANGUAGE = "selected_language"
        const val INSTALL_SOURCE = "install_source"

        //Custom properties
        const val SOURCE = "source"
        const val STATUS = "status"
        const val PERMISSION_GRANTED = "permission_granted"
        const val LANGUAGE = "language"
        const val CURRENT_MONTH = "current_month"
        const val CHOSEN_MONTH = "chosen_month"
        const val LABOR_NAME = "labor_name"
        const val MESSAGE = "message"
        const val AMOUNT = "amount"
        const val EXPENSE_TYPE = "expense_type"
        const val INCOME_TYPE = "income_type"
        const val IS_UPDATE_EXPENSE = "is_update_expense"
        const val IS_UPDATE_INCOME = "is_update_income"
        const val UPDATE_TYPE = "update_type"
        const val DATE = "date"
        const val PRIVACY_MODE = "privacy_mode"
    }
}