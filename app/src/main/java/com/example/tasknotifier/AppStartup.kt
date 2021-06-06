package com.example.tasknotifier

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.tasknotifier.common.Constants

class AppStartup : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    // source: https://developer.android.com/training/notify-user/build-notification#java
    // source: https://www.youtube.com/watch?v=tTbd1Mfi-Sk
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_DEFAULT,
                "App Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "These will notify you about application events."
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}