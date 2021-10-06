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
        createNotificationChannels()
    }

    // source: https://developer.android.com/training/notify-user/build-notification#java
    // source: https://www.youtube.com/watch?v=tTbd1Mfi-Sk
    private fun createNotificationChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_DEFAULT,
                "Default Notification Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "These will notify you about application events."
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.enableVibration(true)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel)


            val silentChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_SILENT,
                "Silent Notification Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            silentChannel.description = "These will also notify you about application events, but silently, without any sound or vibration."
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.enableVibration(false)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }
}