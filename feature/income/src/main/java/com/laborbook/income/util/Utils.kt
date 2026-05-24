package com.laborbook.income.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class Utils {
    companion object {
        fun getCurrentTimeInISOFormat(): String {
            // Create a Date object representing the current date and time
            val currentTime = Date()

            // Set up the SimpleDateFormat with the desired pattern for ISO 8601
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

            // Format the current date to the ISO 8601 format string
            return isoFormat.format(currentTime)
        }
    }
}