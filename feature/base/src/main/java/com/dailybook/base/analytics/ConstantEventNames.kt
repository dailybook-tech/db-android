package com.dailybook.base.analytics

class ConstantEventNames {
    companion object {
        const val APP_OPEN = "app_open"

        //System
        const val NOTIFICATION_PERMISSION_GRANTED = "notification_permission_granted"
        const val DAILY_REMINDER_SET = "daily_reminder_set"
        const val DAILY_REMINDER_TRIGGERED = "daily_reminder_triggered"

        //Auth
        //Impression
        const val LOGIN = "login"
        const val OTP = "otp"
        const val TRUECALLER_INSTALLED = "truecaller_installed"
        const val TRUECALLER_LOGIN_SUCCESS = "truecaller_login_success"
        const val LOGIN_SUCCESS = "login_success"
        /** Single event for Mixpanel: OTP or Truecaller login success (onboarding funnel). */
        const val MOBILE_OTP_TRUECALLER = "mobile_otp_truecaller"

        //Click
        const val REQUEST_OTP = "request_otp"
        const val VERIFY_OTP = "verify_otp"
        const val RESEND_OTP = "resend_otp"
        const val TRUECALLER_LOGIN = "truecaller_login"

        //Book Keep
        const val LABORS = "labors"
        const val SETTINGS = "settings"
        const val CALENDAR = "calendar"
        const val CONTACTS = "contacts"
        const val ATTENDANCE_BS = "attendance_bs"
        const val OVERTIME_BOTTOM_SHEET = "overtime_bottom_sheet"
        const val ADVANCE_BS = "advance_bs"
        const val MONTH_YEAR_BS = "month_year_bs"
        const val MONTHS_LIST = "months_list"
        const val TRANSACTION_STATUS = "transaction_status"
        const val LANGUAGE_BS = "language_bs"
        const val DELETE_LABOR_BS = "delete_labor_bs"
        const val UPDATE_NAME_BS = "update_name_bs"
        const val IN_APP_REVIEW = "in_app_review"
        const val REQUEST_FEATURE = "request_feature"
        const val EDIT_PROFILE = "edit_profile"

        //Click
        const val ADD_LABOR = "add_labor"
        const val LABOR_REPORTS_TAP = "labor_reports_tap"
        const val REFER_A_FRIEND = "refer_a_friend"
        const val SEARCH_LABOR = "search_labor"
        const val VIEW_LABOR_CALENDAR = "view_labor_calendar"

        const val PERMIT_CONTACTS = "permit_contacts"
        const val SHOW_MANUAL_ADD_LABOR_FORM = "show_manual_add_labor"
        const val ADD_LABOR_FROM_CONTACTS = "add_labor_from_contacts"
        const val ADD_LABOR_MANUAL = "add_labor_manual"
        const val SEARCH_CONTACTS = "search_contacts"
        const val REFRESH_CONTACTS = "refresh_contacts"

        const val VIEW_PROFILE_NAME = "view_profile_name"
        const val EDIT_PROFILE_NAME = "edit_profile_name"
        const val VIEW_PRIVACY_POLICY = "view_privacy_policy"
        const val VIEW_TERMS_AND_CONDITIONS = "view_terms_and_conditions"
        const val VIEW_PRICING = "view_pricing"
        const val VIEW_LANGUAGES = "view_languages"
        const val SET_LANGUAGE = "set_language"

        const val MARK_ATTENDANCE = "mark_attendance"
        const val VIEW_MORE_ATTENDANCE_OPTIONS = "view_more_attendance_options"
        const val EDIT_ATTENDANCE = "edit_attendance"
        const val OPEN_ADD_ADVANCE_BS = "open_add_advance_bs"
        const val SHARE_ATTENDANCE_TO_LABOR = "share_attendance_to_labor"
        const val SELECT_ATTENDANCE_FROM_BS = "select_attendance_from_bs"
        const val MARK_ATTENDANCE_FROM_BS = "mark_attendance_from_bs"
        const val REMOVE_ATTENDANCE_FROM_BS = "remove_attendance_from_bs"
        const val REMOVE_OVERTIME_FROM_BS = "remove_overtime_from_bs"
        const val ADD_ADVANCE = "add_advance"
        const val DELETE_ADVANCE = "delete_advance"
        const val CHANGE_MONTH = "change_month"
        const val SET_MONTH = "set_month"
        const val IN_APP_REVIEW_DONE = "in_app_review_done"
        const val REQUEST_FEATURE_SUBMIT = "request_feature_submit"

        //Expense
        const val EXPENSE = "expense"
        const val INCOME = "income"
        const val CASH_IN_OUT_BS = "cash_in_out_bs"
        const val EXPENSE_TRANSACTION_STATUS = "expense_transaction_status"
        const val INCOME_TRANSACTION_STATUS = "income_transaction_status"
        const val EXPENSE_TRANSACTION_DETAILS = "expense_transaction_details"
        const val INCOME_TRANSACTION_DETAILS = "income_transaction_details"

        //Click
        const val CASH_IN = "cash_in"
        const val CASH_OUT = "cash_out"
        const val CHANGE_EXPENSE_MONTH = "change_expense_month"
        const val CHANGE_INCOME_MONTH = "change_income_month"
        const val SET_EXPENSE_MONTH = "set_expense_month"
        const val SET_INCOME_MONTH = "set_income_month"
        const val VIEW_EXPENSE = "view_expense"
        const val VIEW_INCOME = "view_income"
        const val TOGGLE_PRIVACY_MODE = "toggle_privacy_mode"

        //Cash in out BS
        const val SAVE_EXPENSE = "save_expense"
        const val SAVE_INCOME = "save_income"
        const val DELETE_EXPENSE_TRY = "delete_expense_try"
        const val DELETE_INCOME_TRY = "delete_income_try"
        const val DELETE_EXPENSE_CONFIRM = "delete_expense_confirm"
        const val DELETE_INCOME_CONFIRM = "delete_income_confirm"
        const val EDIT_EXPENSE_DATE = "edit_expense_date"
        const val EDIT_INCOME_DATE = "edit_income_date"
        const val SHARE_PDF_REPORT = "share_pdf_report"
        const val SHARE_WHATSAPP_REPORT = "share_whatsapp_report"
        const val VIEW_REPORTS = "view_reports"

        //InApp Update
        const val CHECK_FOR_UPDATE = "check_for_update"
        const val UPDATE_AVAILABLE = "update_available"
        const val START_UPDATE = "start_update"

        //Settings
        const val SHOW_UPDATE_BUTTON = "show_update_button"
        const val OPEN_RATINGS = "open_ratings"
        const val OPEN_APP_UPDATE = "open_app_update"
        const val OPEN_REQUEST_FEATURE = "open_request_feature"
        const val FEATURE_REQUEST_SENT = "feature_request_sent"
        const val VIEW_DAILYBOOK_PRO = "view_dailybook_pro"
        const val PREMIUM_SETTINGS = "premium_settings"
        
        //Premium Offer Dialog
        const val PREMIUM_OFFER_DIALOG = "premium_offer_dialog"
        const val PREMIUM_OFFER_CLOSE = "premium_offer_close"
        const val SELECT_UPI_APP = "select_upi_app"
        const val START_TRIAL_CLICK = "start_trial_click"
        
        //Subscription Flow
        const val SUBSCRIPTION_PLAN_VIEWED = "subscription_plan_viewed"
        const val SUBSCRIPTION_INITIATED = "subscription_initiated"
        const val PAYMENT_STARTED = "payment_started"
        const val PAYMENT_SUCCESS = "payment_success"
        const val PAYMENT_FAILED = "payment_failed"
        const val SUBSCRIPTION_VERIFIED = "subscription_verified"
        const val SUBSCRIPTION_ACTIVATED = "subscription_activated"

        //google Ads
        const val GOOGLE_BANNER_AD = "google_banner_ad"
        const val GOOGLE_BANNER_AD_OPEN = "google_banner_ad_open"
        const val GOOGLE_BANNER_AD_CLICK = "google_banner_ad_click"
        const val GOOGLE_BANNER_AD_CLOSE = "google_banner_ad_close"
        
        //Custom Ads
        const val CUSTOM_BANNER_AD = "custom_banner_ad"
        const val CUSTOM_BANNER_AD_CLICK = "custom_banner_ad_click"
        const val CUSTOM_BANNER_AD_ERROR = "custom_banner_ad_error"
    }
}