package com.example.tasknotifier.android_services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.tasknotifier.common.Globals
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NotificationReviverAndroidService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskService = TaskService(this)

        runBlocking {
            launch {
                val tasks = taskService.fetchAllTheInProgressAsync()

                tasks.forEach { task ->
                    val taskTitle = Globals.createTitleForTask(task.dateTime, task.sentCount)

                    MyNotificationManager.notifySilently(
                        this@NotificationReviverAndroidService,
                        task.id,
                        taskTitle,
                        task.description,
                        0,
                        true,
                    )
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}