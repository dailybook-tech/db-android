package co.dailybook.base

import android.util.Log

object Logger {

    private const val TAG = "LBLOG"

    private fun getCallerClassName(): String {
        val stackTrace = Thread.currentThread().stackTrace
        if (stackTrace.size > 4) {
            return stackTrace[4].className
        }
        return "Unknown"
    }

    fun d(message: String) {
        if (BaseConstants.DEBUG) {
            Log.d(TAG, "[${getCallerClassName()}] $message")
        }
    }

    fun e(message: String) {
        if (BaseConstants.DEBUG) {
            Log.e(TAG, "[${getCallerClassName()}] $message")
        }
    }

    fun i(message: String) {
        if (BaseConstants.DEBUG) {
            Log.i(TAG, "[${getCallerClassName()}] $message")
        }
    }

    fun w(message: String) {
        if (BaseConstants.DEBUG) {
            Log.w(TAG, "[${getCallerClassName()}] $message")
        }
    }
}