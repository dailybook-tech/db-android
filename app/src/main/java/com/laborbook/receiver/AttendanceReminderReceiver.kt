package com.laborbook.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.boilerplate.analytics.AnalyticsPlatforms
import com.laborbook.R
import com.laborbook.RoutingActivity
import com.laborbook.base.analytics.Analytics
import com.laborbook.base.analytics.ConstantEventNames
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AttendanceReminderReceiver : BroadcastReceiver(), KoinComponent {

    val analytics: Analytics by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        // Check if we have the necessary notification permissions
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            // Create the notification channel if necessary
            createNotificationChannel(context)

            // Create an intent to open RoutingActivity when the notification is clicked
            val activityIntent = Intent(context, RoutingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build the notification
            val notification = NotificationCompat.Builder(context, "attendance_channel_id")
                .setSmallIcon(R.drawable.ic_notification_small) // Set your small icon
                .setContentTitle(context.getString(R.string.attendance_reminder_title))
                .setContentText(context.getString(R.string.attendance_reminder_content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)  // Set the intent to open RoutingActivity
                .build()

            // Trigger the notification
            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(0, notification)
                triggerImpressionEvent(ConstantEventNames.DAILY_REMINDER_TRIGGERED)
            } catch (e: SecurityException) {
                e.printStackTrace()
                // Handle the case where the notification permission is not available
            }
        } else {
            // Handle the case where notification permission is not granted
            // Optionally, request permission if needed
        }
    }

    private fun createNotificationChannel(context: Context) {
        // Only create the notification channel on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "attendance_channel_id"
            val channelName = "Attendance Reminder Channel"
            val channelDescription = "Channel for daily attendance reminder notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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