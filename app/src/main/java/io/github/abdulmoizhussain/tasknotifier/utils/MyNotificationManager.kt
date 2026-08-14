package io.github.abdulmoizhussain.tasknotifier.utils

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import io.github.abdulmoizhussain.tasknotifier.ActivityViewTask
import io.github.abdulmoizhussain.tasknotifier.R
import io.github.abdulmoizhussain.tasknotifier.broadcast_receivers.NotificationDismissedBroadcastReceiver
import io.github.abdulmoizhussain.tasknotifier.common.Constants
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticLog

class MyNotificationManager {
    companion object {
        private fun pendingIntentFlags(): Int {
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }
            return flags
        }

        private fun createTaskContentIntent(context: Context, taskId: Int): PendingIntent {
            val detailIntent = Intent(context, ActivityViewTask::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("task-notifier://task/$taskId")
                putExtra(Constants.INTENT_EXTRA_TASK_ID, taskId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            return requireNotNull(
                TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(detailIntent)
                    .getPendingIntent(taskId, pendingIntentFlags())
            )
        }

        private fun createDismissIntent(context: Context, taskId: Int): PendingIntent {
            val dismissIntent = Intent(context, NotificationDismissedBroadcastReceiver::class.java).apply {
                action = Constants.INTENT_ACTION_NOTIFICATION_DISMISSED
                data = Uri.parse("task-notifier://notification/$taskId/dismissed")
                putExtra(Constants.INTENT_EXTRA_TASK_ID, taskId)
            }

            return PendingIntent.getBroadcast(context, taskId, dismissIntent, pendingIntentFlags())
        }

        private fun makePersistentUntilAcknowledged(
            builder: NotificationCompat.Builder,
            context: Context,
            notificationId: Int,
            onGoing: Boolean
        ) {
            if (onGoing) {
                builder.setOngoing(true)
                builder.setAutoCancel(false)
                builder.setDeleteIntent(createDismissIntent(context, notificationId))
            }
        }

        private fun keepTaskNotificationSeparate(
            builder: NotificationCompat.Builder,
            notificationId: Int
        ) {
            builder.setGroup("${Constants.NOTIFICATION_GROUP_TASK_PREFIX}.$notificationId")
            builder.setGroupSummary(false)
        }

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
            builder.setSmallIcon(R.drawable.ic_stat_notification)
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))

            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.priority = NotificationCompat.PRIORITY_LOW
            builder.setSilent(true)
            builder.setDefaults(0)

            makePersistentUntilAcknowledged(builder, context, notificationId, onGoing)
            keepTaskNotificationSeparate(builder, notificationId)

            builder.setWhen(setWhen)
            builder.setShowWhen(true)

            val notification = builder.build()

            postAndRecord(
                context,
                notificationId,
                Constants.NOTIFICATION_CHANNEL_SILENT,
                "UNCHECKABLE_SILENT",
                onGoing,
                notification,
            )
        }

        fun notifySilently(
            context: Context,
            notificationId: Int,
            contentTitle: String?,
            contentText: String?,
            setWhen: Long,
            onGoing: Boolean
        ) {
            val pendingIntent = createTaskContentIntent(context, notificationId)

            val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SILENT)
            builder.setContentTitle(contentTitle)
            builder.setContentText(contentText)
            builder.setSmallIcon(R.drawable.ic_stat_notification)
            builder.setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            builder.setContentIntent(pendingIntent)

            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.priority = NotificationCompat.PRIORITY_LOW
            builder.setSilent(true)
            builder.setDefaults(0)

            // TODO remove notification after some time, to show start of the service
//            builder.setTimeoutAfter(10000)

            makePersistentUntilAcknowledged(builder, context, notificationId, onGoing)
            keepTaskNotificationSeparate(builder, notificationId)

            builder.setWhen(setWhen)
            builder.setShowWhen(true)

            val notification = builder.build()

            postAndRecord(
                context,
                notificationId,
                Constants.NOTIFICATION_CHANNEL_SILENT,
                "SILENT",
                onGoing,
                notification,
            )
        }

        fun notify(
            context: Context,
            notificationId: Int,
            contentTitle: String?,
            contentText: String?,
            setWhen: Long,
            onGoing: Boolean
        ) {
            val pendingIntent = createTaskContentIntent(context, notificationId)

            val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DEFAULT)
            builder.setContentTitle(contentTitle)
            builder.setContentText(contentText)
            builder.setSmallIcon(R.drawable.ic_stat_notification)

            // source: https://stackoverflow.com/a/11756312/8075004
            builder.setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))

            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            builder.setContentIntent(pendingIntent)

            builder.setDefaults(Notification.DEFAULT_ALL)

            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.priority = NotificationCompat.PRIORITY_HIGH

            // TODO remove notification after some time, to show start of the service
//            builder.setTimeoutAfter(10000)

            makePersistentUntilAcknowledged(builder, context, notificationId, onGoing)
            keepTaskNotificationSeparate(builder, notificationId)

            builder.setWhen(setWhen)
            builder.setShowWhen(true)

            val notification = builder.build()

            postAndRecord(
                context,
                notificationId,
                Constants.NOTIFICATION_CHANNEL_DEFAULT,
                "DEFAULT",
                onGoing,
                notification,
            )
        }

        fun cancelById(context: Context, notificationId: Int) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            DiagnosticLog.record(context, "NOTIFICATION_CANCEL_REQUESTED", notificationId)
            notificationManager.cancel(notificationId)
            DiagnosticLog.record(
                context,
                "NOTIFICATION_CANCEL_RESULT",
                notificationId,
                mapOf("active" to isNotificationActive(notificationManager, notificationId)),
            )
        }

        private fun postAndRecord(
            context: Context,
            notificationId: Int,
            channelId: String,
            mode: String,
            onGoing: Boolean,
            notification: Notification,
        ) {
            val notificationManager = context
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            DiagnosticLog.record(
                context,
                "NOTIFICATION_POST_REQUESTED",
                notificationId,
                mapOf(
                    "channelId" to channelId,
                    "mode" to mode,
                    "ongoing" to onGoing,
                ),
            )
            try {
                notificationManager.notify(notificationId, notification)
                DiagnosticLog.record(
                    context,
                    "NOTIFICATION_POST_RESULT",
                    notificationId,
                    mapOf(
                        "active" to isNotificationActive(notificationManager, notificationId),
                        "activeNotificationIds" to activeNotificationIds(notificationManager),
                    ),
                )
            } catch (exception: Exception) {
                DiagnosticLog.record(
                    context,
                    "NOTIFICATION_POST_FAILED",
                    notificationId,
                    throwable = exception,
                )
                throw exception
            }
        }

        private fun isNotificationActive(
            notificationManager: NotificationManager,
            notificationId: Int,
        ): Any {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.activeNotifications.any { it.id == notificationId }
            } else {
                "unavailable_below_api_23"
            }
        }

        private fun activeNotificationIds(notificationManager: NotificationManager): Any {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.activeNotifications.map { it.id }
            } else {
                "unavailable_below_api_23"
            }
        }
    }

}
