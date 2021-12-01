package com.example.tasknotifier.utils

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.example.tasknotifier.ActivityViewTask
import com.example.tasknotifier.MainActivity
import com.example.tasknotifier.R
import com.example.tasknotifier.common.Constants

class MyNotificationManager {
    companion object {
        fun notifyWithUnClickable(
            context: Context,
            notificationId: Int,
            contentTitle: String?,
            contentText: String?,
            setWhen: Long,
            onGoing: Boolean
        ) {
            val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SILENT)
            builder.setContentTitle(contentTitle)
            builder.setContentText(contentText)
            builder.setSmallIcon(R.drawable.ic_launcher_background)
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))

            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.priority = NotificationCompat.PRIORITY_LOW

            if (onGoing) {
                builder.setOngoing(onGoing)
                builder.setAutoCancel(false)
            }

            builder.setWhen(setWhen)
            builder.setShowWhen(true)

            val notification = builder.build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        }

        fun notifySilently(
            context: Context,
            notificationId: Int,
            contentTitle: String?,
            contentText: String?,
            setWhen: Long,
            onGoing: Boolean
        ) {
            val pendingIntent: PendingIntent = Intent(context, ActivityViewTask::class.java).let { intentMainActivity ->

                // TODO PUT EXTRAS DYNAMICALLY...
                intentMainActivity.putExtra(Constants.INTENT_EXTRA_TASK_ID, notificationId)

                val taskStackBuilder = TaskStackBuilder.create(context)
                taskStackBuilder.addParentStack(MainActivity::class.java)
                taskStackBuilder.addNextIntent(intentMainActivity)

                taskStackBuilder.getPendingIntent(notificationId, PendingIntent.FLAG_CANCEL_CURRENT) as PendingIntent
            }

            val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SILENT)
            builder.setContentTitle(contentTitle)
            builder.setContentText(contentText)
            builder.setSmallIcon(R.drawable.ic_launcher_background)
            builder.setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            builder.setContentIntent(pendingIntent)

            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.priority = NotificationCompat.PRIORITY_LOW

            // TODO remove notification after some time, to show start of the service
//            builder.setTimeoutAfter(10000)

            if (onGoing) {
                builder.setOngoing(onGoing)
                builder.setAutoCancel(false)
            }

            builder.setWhen(setWhen)
            builder.setShowWhen(true)

            val notification = builder.build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        }

        fun notify(
            context: Context,
            notificationId: Int,
            contentTitle: String?,
            contentText: String?,
            setWhen: Long,
            onGoing: Boolean
        ) {
            val pendingIntent: PendingIntent = Intent(context, ActivityViewTask::class.java).let { intentMainActivity ->

                // TODO PUT EXTRAS DYNAMICALLY...
                intentMainActivity.putExtra(Constants.INTENT_EXTRA_TASK_ID, notificationId)

                val taskStackBuilder = TaskStackBuilder.create(context)
                taskStackBuilder.addParentStack(MainActivity::class.java)
                taskStackBuilder.addNextIntent(intentMainActivity)

                taskStackBuilder.getPendingIntent(notificationId, PendingIntent.FLAG_CANCEL_CURRENT) as PendingIntent
            }

            val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DEFAULT)
            builder.setContentTitle(contentTitle)
            builder.setContentText(contentText)
            builder.setSmallIcon(R.drawable.ic_launcher_background)
            builder.setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            builder.setContentIntent(pendingIntent)

            builder.setDefaults(Notification.DEFAULT_ALL)

            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.priority = NotificationCompat.PRIORITY_HIGH

            // TODO remove notification after some time, to show start of the service
//            builder.setTimeoutAfter(10000)

            if (onGoing) {
                builder.setOngoing(onGoing)
                builder.setAutoCancel(false)
            }

            builder.setWhen(setWhen)
            builder.setShowWhen(true)

            val notification = builder.build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        }

        fun cancelById(context: Context, notificationId: Int) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }
    }

}