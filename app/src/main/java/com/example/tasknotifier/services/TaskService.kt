package com.example.tasknotifier.services

import android.content.Context
import android.content.Intent
import com.example.tasknotifier.SendNotificationBroadcastReceiver
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.data.AppDatabase
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.repositories.TaskRepository
import com.example.tasknotifier.utils.MyAlarmManager

class TaskService(context: Context) {
    companion object {
        fun createIntentAndSetExactAlarm(context: Context, requestCode: Int, triggerAtMillis: Long) {
            val intent = Intent(context.applicationContext, SendNotificationBroadcastReceiver::class.java)
            intent.putExtra(Constants.INTENT_EXTRA_TASK_ID, requestCode)

            MyAlarmManager.setExact(context, requestCode, intent, triggerAtMillis)
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

    suspend fun fetchAllWhichAreDueAndOnAsync(): Array<Task> {
        return taskRepository.fetchAllWhichAreDueAndOnAsync()
    }
}