package io.github.abdulmoizhussain.tasknotifier.utils

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import io.github.abdulmoizhussain.tasknotifier.ActivityViewTask
import io.github.abdulmoizhussain.tasknotifier.R
import io.github.abdulmoizhussain.tasknotifier.broadcast_receivers.NotificationDismissedBroadcastReceiver
import io.github.abdulmoizhussain.tasknotifier.common.Constants
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticLog
import io.github.abdulmoizhussain.tasknotifier.diagnostics.NotificationTelemetry

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
                putExtra(Constants.INTENT_EXTRA_POSTED_AT_MILLIS, System.currentTimeMillis())
            }

            // FLAG_UPDATE_CURRENT refreshes the extras, so the timestamp always
            // belongs to the most recent post of this notification id.
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
            val snapshot = NotificationTelemetry.activeSnapshot(notificationManager)
            DiagnosticLog.record(
                context,
                "NOTIFICATION_CANCEL_RESULT",
                notificationId,
                mapOf(
                    "active" to NotificationTelemetry.activeIds(snapshot).contains(notificationId),
                    "activeNotificationIds" to NotificationTelemetry.activeIds(snapshot),
                ),
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

            val attributes = LinkedHashMap<String, Any?>()
            attributes["channelId"] = channelId
            attributes["mode"] = mode
            attributes["ongoing"] = onGoing
            // NotificationCompat.getGroup handles the API 20 floor; Notification.getGroup
            // itself is above this module's minSdk of 16.
            attributes["group"] = NotificationCompat.getGroup(notification)
            attributes["whenMillis"] = notification.`when`
            attributes.putAll(NotificationTelemetry.onPostRequested(notificationId))
            attributes.putAll(NotificationTelemetry.environment(context))

            DiagnosticLog.record(context, "NOTIFICATION_POST_REQUESTED", notificationId, attributes)

            try {
                notificationManager.notify(notificationId, notification)

                // One snapshot for both fields. Reading getActiveNotifications() twice
                // produced self-contradicting events in the 2026-09 export.
                val snapshot = NotificationTelemetry.activeSnapshot(notificationManager)
                DiagnosticLog.record(
                    context,
                    "NOTIFICATION_POST_RESULT",
                    notificationId,
                    mapOf(
                        "active" to NotificationTelemetry.activeIds(snapshot).contains(notificationId),
                        "activeNotificationIds" to NotificationTelemetry.activeIds(snapshot),
                    ),
                )

                schedulePostVerification(context, notificationId, mode)
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

        /**
         * Re-checks the notification a few seconds after posting.
         *
         * The immediate read-back cannot distinguish "the post was dropped" from
         * "the post has not surfaced yet", which is exactly the ambiguity that
         * blocked the 2026-09 investigation. This delayed check can.
         *
         * Best effort: the posting process may already be gone when it is due, in
         * which case the event is simply absent from the log.
         */
        private fun schedulePostVerification(context: Context, notificationId: Int, mode: String) {
            val applicationContext = context.applicationContext
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val notificationManager = applicationContext
                        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val snapshot = NotificationTelemetry.activeSnapshot(notificationManager)
                    val active = NotificationTelemetry.activeIds(snapshot).contains(notificationId)
                    DiagnosticLog.record(
                        applicationContext,
                        if (active) "NOTIFICATION_POST_VERIFIED" else "NOTIFICATION_POST_LOST",
                        notificationId,
                        mapOf(
                            "mode" to mode,
                            "delayMillis" to NotificationTelemetry.VERIFY_DELAY_MILLIS,
                            "activeNotificationIds" to NotificationTelemetry.activeIds(snapshot),
                            "activeDetail" to NotificationTelemetry.describeActive(snapshot),
                        ),
                    )
                } catch (_: Exception) {
                    // Diagnostics must never interfere with reminder delivery.
                }
            }, NotificationTelemetry.VERIFY_DELAY_MILLIS)
        }

    }

}
