package com.example.tasknotifier

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder

class MyNotificationManager {
    companion object {
        fun notify(
            context: Context,
            notificationId: Int,
            contentTitle: String?,
            contentText: String?,
            setWhen: Long,
            onGoing: Boolean
        ) {
            val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).let { intentMainActivity ->
                val taskStackBuilder = TaskStackBuilder.create(context)
                taskStackBuilder.addParentStack(MainActivity::class.java)
                taskStackBuilder.addNextIntent(intentMainActivity)

                taskStackBuilder.getPendingIntent(notificationId, PendingIntent.FLAG_CANCEL_CURRENT) as PendingIntent
            }

            val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DEFAULT)
            builder.setContentTitle(contentTitle)
            builder.setContentText(contentText)
            builder.setSmallIcon(R.drawable.ic_launcher_background)
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            builder.setDefaults(Notification.DEFAULT_ALL)
            builder.setContentIntent(pendingIntent)
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.priority = NotificationCompat.PRIORITY_HIGH
            builder.setAutoCancel(true)
            builder.setOngoing(onGoing)
            builder.setWhen(setWhen)
            builder.setShowWhen(true)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, builder.build())
        }

    }

}