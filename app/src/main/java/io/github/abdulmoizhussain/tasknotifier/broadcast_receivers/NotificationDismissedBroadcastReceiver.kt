package io.github.abdulmoizhussain.tasknotifier.broadcast_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import io.github.abdulmoizhussain.tasknotifier.common.Constants
import io.github.abdulmoizhussain.tasknotifier.common.Globals
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticLog
import io.github.abdulmoizhussain.tasknotifier.diagnostics.NotificationTelemetry
import io.github.abdulmoizhussain.tasknotifier.services.TaskService
import io.github.abdulmoizhussain.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationDismissedBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.INTENT_ACTION_NOTIFICATION_DISMISSED) {
            return
        }

        val taskId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, -1)
        if (taskId < 1) {
            return
        }

        val pendingResult = goAsync()
        val applicationContext = context.applicationContext
        val postedAtMillis = intent.getLongExtra(Constants.INTENT_EXTRA_POSTED_AT_MILLIS, 0L)
        val ageMillis = if (postedAtMillis > 0L) System.currentTimeMillis() - postedAtMillis else null
        DiagnosticLog.record(
            applicationContext,
            "NOTIFICATION_DISMISSED_RECEIVED",
            taskId,
            LinkedHashMap<String, Any?>().apply {
                put("postedAtMillis", if (postedAtMillis > 0L) postedAtMillis else null)
                put("ageMillis", ageMillis)
                put("ageHours", ageMillis?.let { "%.4f".format(it / 3_600_000.0) })
                // A system reap lands very close to a whole number of hours; a user
                // swipe does not. See docs/NOTIFICATION_LOSS_INVESTIGATION.md.
                put("looksLikeSystemReap", ageMillis?.let { it > 60 * 60 * 1000L })
                putAll(NotificationTelemetry.environment(applicationContext))
            },
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = TaskService(applicationContext).getOneByIdAsync(taskId)
                if (task != null && task.inProgress) {
                    DiagnosticLog.record(
                        applicationContext,
                        "DISMISSED_NOTIFICATION_RESTORING",
                        taskId,
                        mapOf("status" to task.status.name, "inProgress" to task.inProgress),
                    )
                    MyNotificationManager.notifySilently(
                        applicationContext,
                        task.id,
                        Globals.createTitleForTask(task.dateTime, task.sentCount),
                        task.description,
                        task.dateTime,
                        true,
                    )
                } else {
                    DiagnosticLog.record(
                        applicationContext,
                        "DISMISSED_NOTIFICATION_NOT_RESTORED",
                        taskId,
                        mapOf(
                            "taskFound" to (task != null),
                            "inProgress" to task?.inProgress,
                        ),
                    )
                }
            } catch (exception: Exception) {
                DiagnosticLog.record(
                    applicationContext,
                    "NOTIFICATION_DISMISS_HANDLER_FAILED",
                    taskId,
                    throwable = exception,
                )
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        applicationContext,
                        exception.localizedMessage ?: exception.toString(),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
