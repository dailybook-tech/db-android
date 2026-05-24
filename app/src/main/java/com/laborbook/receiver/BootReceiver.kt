package com.laborbook.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.laborbook.reminder.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule the attendance reminder after reboot
            AlarmScheduler.scheduleDailyAttendanceReminder(context)
        }
    }
}