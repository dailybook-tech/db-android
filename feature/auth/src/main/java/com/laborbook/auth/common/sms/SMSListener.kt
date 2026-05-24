package com.laborbook.auth.common.sms

interface SMSListener {
    fun onSuccess(message: String?)

    fun onError(message: String?)
}