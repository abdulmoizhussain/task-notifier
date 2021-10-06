package com.example.tasknotifier.services

import android.content.Context
import android.content.Intent
import com.example.tasknotifier.broadcast_receivers.SendNotificationBroadcastReceiver
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.data.AppDatabase
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.repositories.TaskRepository
import com.example.tasknotifier.utils.MyAlarmManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TaskService(context: Context) {
    companion object {
        fun createIntentAndSetExactAlarm(context: Context, requestCode: Int, triggerAtMillis: Long) {
            val intent = Intent(context, SendNotificationBroadcastReceiver::class.java)
            intent.putExtra(Constants.INTENT_EXTRA_TASK_ID, requestCode)

            MyAlarmManager.setAlarmClock(context, requestCode, intent, triggerAtMillis)
        }
    }

    private val taskRepository: TaskRepository

    init {
        val taskDao = AppDatabase.getDatabase(context).taskDao()
        taskRepository = TaskRepository(taskDao)
    }

    suspend fun getOneByIdAsync(id: Int): Task? {
        return taskRepository.getOneByIdAsync(id)
    }

    suspend fun updateOneAsync(task: Task) {
        taskRepository.updateOneAsync(task)
    }

    fun turnOffInProgressByTaskId(taskId: Int) {
        runBlocking {
            launch {
                val task = taskRepository.getOneByIdAsync(taskId)
                if (task != null) {
                    task.inProgress = false
                    taskRepository.updateOneAsync(task)
                }
            }
        }
    }

    suspend fun fetchAllWhichAreDueAndOnAsync(): Array<Task> {
        return taskRepository.fetchAllWhichAreDueAndOnAsync()
    }

    suspend fun fetchAllTheInProgressAsync(): Array<Task> {
        return taskRepository.fetchAllTheInProgressAsync()
    }
}