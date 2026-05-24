package com.laborbook.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.laborbook.R
import com.laborbook.RoutingActivity
import com.laborbook.base.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

class MyFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val dataStoreManager: DataStoreManager by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if the message contains a notification payload.
        remoteMessage.notification?.let {
            // Build the notification and pass the title and body.
            showNotification(it.title, it.body)
        }
    }

    private fun showNotification(title: String?, message: String?) {
        val channelId = "communication_channel"

        // Intent to open RoutingActivity when the notification is tapped
        val intent = Intent(this, RoutingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Create PendingIntent for the notification
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_small)  // Your app's notification icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)  // Close the notification when tapped
            .setContentIntent(pendingIntent)  // Add the pending intent to the notification
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // Show notification as a priority one

        // Get Notification Manager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel for Android O and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Communication Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification
        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dataStoreManager.write(DataStoreManager.FCM_TOKEN, token)
            }catch (e: Exception){}
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}