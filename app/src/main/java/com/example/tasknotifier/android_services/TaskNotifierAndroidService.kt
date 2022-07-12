package com.example.tasknotifier.android_services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.common.Globals
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TaskNotifierAndroidService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

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

        if (intent == null || notificationReviver) {
            runBlocking {
                launch {
                    TaskService(this@TaskNotifierAndroidService).fetchAllTheInProgressAsync().forEach { task ->
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
                    TaskService(this@TaskNotifierAndroidService).fetchAllWhichAreDueAndOnAsync().forEach { task ->
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

        System.gc()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}