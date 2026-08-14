package io.github.abdulmoizhussain.tasknotifier.android_services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import io.github.abdulmoizhussain.tasknotifier.common.Constants
import io.github.abdulmoizhussain.tasknotifier.common.Globals
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticLog
import io.github.abdulmoizhussain.tasknotifier.services.TaskService
import io.github.abdulmoizhussain.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TaskNotifierAndroidService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {

        DiagnosticLog.record(
            this,
            "SERVICE_ON_START",
            attributes = mapOf(
                "startId" to startId,
                "flags" to flags,
                "intentWasNull" to (intent == null),
            ),
        )

//        MyNotificationManager.notifyWithUnClickable(
//            this,
//            999999,
//            null,
//            "Back service started",
//            System.currentTimeMillis(),
//            false,
//        )

        var notificationReviver = false
        var taskScheduler = false
        var taskId: Int = -1

        if (intent != null) {
            notificationReviver = intent.getBooleanExtra(Constants.INTENT_EXTRA_NOTIFICATION_REVIVER_SERVICE, false)
            taskScheduler = intent.getBooleanExtra(Constants.INTENT_EXTRA_TASK_SCHEDULER_SERVICE, false)
            taskId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, -1)
        }
        DiagnosticLog.record(
            this,
            "SERVICE_COMMAND_PARSED",
            if (taskId > -1) taskId else null,
            mapOf(
                "notificationReviver" to notificationReviver,
                "taskScheduler" to taskScheduler,
            ),
        )

        if (intent == null || notificationReviver) {
            runBlocking {
                launch {
                    val tasks = TaskService(this@TaskNotifierAndroidService).fetchAllTheInProgressAsync()
                    DiagnosticLog.record(
                        this@TaskNotifierAndroidService,
                        "REVIVER_QUERY_RESULT",
                        attributes = mapOf(
                            "count" to tasks.size,
                            "taskIds" to tasks.map { it.id },
                            "taskStates" to tasks.map {
                                "${it.id}:${it.status.name}:${it.inProgress}:${it.dateTime}:${it.sentCount}"
                            },
                        ),
                    )
                    tasks.forEach { task ->
                        DiagnosticLog.record(
                            this@TaskNotifierAndroidService,
                            "REVIVING_NOTIFICATION",
                            task.id,
                            mapOf(
                                "status" to task.status.name,
                                "inProgress" to task.inProgress,
                                "dateTime" to task.dateTime,
                                "sentCount" to task.sentCount,
                            ),
                        )
                        MyNotificationManager.notifySilently(
                            this@TaskNotifierAndroidService,
                            task.id,
                            Globals.createTitleForTask(task.dateTime, task.sentCount),
                            task.description,
                            task.dateTime,
                            true,
                        )
                    }
                }
            }
        }

        if (intent == null || taskScheduler) {
            runBlocking {
                launch {
                    val tasks = TaskService(this@TaskNotifierAndroidService).fetchAllWhichAreDueAndOnAsync()
                    DiagnosticLog.record(
                        this@TaskNotifierAndroidService,
                        "SCHEDULER_QUERY_RESULT",
                        attributes = mapOf(
                            "count" to tasks.size,
                            "taskIds" to tasks.map { it.id },
                        ),
                    )
                    tasks.forEach { task ->
                        TaskService.createIntentAndSetExactAlarm(this@TaskNotifierAndroidService, task.id, task.dateTime)
                    }
                }
            }
        }

        if (intent != null && taskId > -1) {
            val contentTitle = intent.getStringExtra(Constants.INTENT_EXTRA_CONTENT_TITLE)
            val description = intent.getStringExtra(Constants.INTENT_EXTRA_DESCRIPTION)
            val setWhen = intent.getLongExtra(Constants.INTENT_EXTRA_SET_WHEN, 0L)
            val onGoing = intent.getBooleanExtra(Constants.INTENT_EXTRA_ON_GOING, false)

            MyNotificationManager.notify(
                this,
                taskId,
                contentTitle,
                description,
                setWhen,
                onGoing
            )
        }

        } catch (exception: Exception) {
            DiagnosticLog.record(this, "SERVICE_FAILED", throwable = exception)
            Toast.makeText(
                applicationContext,
                exception.localizedMessage ?: exception.toString(),
                Toast.LENGTH_LONG,
            ).show()
        } finally {
            DiagnosticLog.record(
                this,
                "SERVICE_FINISHED",
                attributes = mapOf("startId" to startId),
            )
            stopSelfResult(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}
