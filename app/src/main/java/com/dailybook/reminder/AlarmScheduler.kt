package com.dailybook.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.boilerplate.analytics.AnalyticsPlatforms
import com.dailybook.base.analytics.Analytics
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.receiver.AttendanceReminderReceiver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.TimeZone

object AlarmScheduler : KoinComponent {

    val analytics: Analytics by inject()

    fun isAlarmSet(context: Context, requestCode: Int): Boolean {
        val intent = Intent(context, AttendanceReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }

    fun scheduleDailyAttendanceReminder(context: Context) {
        // Check if the alarm is already set
        if (!isAlarmSet(context, 0)) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AttendanceReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set the alarm for 7:30 PM IST
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
                set(Calendar.HOUR_OF_DAY, 19)  // 7 PM
                set(Calendar.MINUTE, 30)       // 30 minutes
                set(Calendar.SECOND, 0)
            }

            // If 7:30 PM has already passed today, set for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Schedule the repeating alarm every day
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            triggerImpressionEvent(ConstantEventNames.DAILY_REMINDER_SET)
        }
    }

    private fun triggerImpressionEvent(eventName: String, hashMap: HashMap<String, Any>? = null) {
        analytics.logEvent(
            eventName,
            Analytics.SYSTEM,
            listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
            hashMap
        )
    }
}