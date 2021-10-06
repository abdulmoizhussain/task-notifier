package com.example.tasknotifier.android_services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.tasknotifier.services.TaskService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TaskSchedulerAndroidService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskService = TaskService(this)

        runBlocking {
            launch {

                val tasks = taskService.fetchAllWhichAreDueAndOnAsync()

                tasks.forEach { task ->
                    TaskService.createIntentAndSetExactAlarm(this@TaskSchedulerAndroidService, task.id, task.dateTime)
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}