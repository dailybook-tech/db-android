package com.boilerplate.uikit

import androidx.appcompat.app.AppCompatDelegate
import java.util.Calendar

object ThemeUtils {

    fun applyTheme() {
        if (isNightTime()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun isNightTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val amPm = calendar.get(Calendar.AM_PM)

        // Check if the time is between 6 PM (18:00) and 5 AM (05:00)
        return if (hourOfDay >= 18 || hourOfDay < 5) {
            true
        } else if (hourOfDay == 12 && amPm == Calendar.AM) {
            true // Midnight (12 AM) is considered as night time
        } else {
            false
        }
    }
}