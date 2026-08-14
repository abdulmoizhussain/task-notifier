package io.github.abdulmoizhussain.tasknotifier.services

import android.content.Context
import android.content.Intent
import io.github.abdulmoizhussain.tasknotifier.broadcast_receivers.SendNotificationBroadcastReceiver
import io.github.abdulmoizhussain.tasknotifier.common.Constants
import io.github.abdulmoizhussain.tasknotifier.data.AppDatabase
import io.github.abdulmoizhussain.tasknotifier.data.task.Task
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticLog
import io.github.abdulmoizhussain.tasknotifier.repositories.TaskRepository
import io.github.abdulmoizhussain.tasknotifier.utils.MyAlarmManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TaskService(context: Context) {
    companion object {
        fun createIntentAndSetExactAlarm(context: Context, requestCode: Int, triggerAtMillis: Long) {
            val intent = Intent(context, SendNotificationBroadcastReceiver::class.java)
            intent.putExtra(Constants.INTENT_EXTRA_TASK_ID, requestCode)

//            MyAlarmManager.setAlarmClock(context, requestCode, intent, triggerAtMillis)
            MyAlarmManager.setExact(context, requestCode, intent, triggerAtMillis)
        }
    }

    private val taskRepository: TaskRepository
    private val applicationContext = context.applicationContext

    init {
        val taskDao = AppDatabase.getDatabase(context).taskDao()
        taskRepository = TaskRepository(taskDao)
    }

    suspend fun getAllAsync(): List<Task> {
        return taskRepository.getAllAsync()
    }

    suspend fun getOneByIdAsync(id: Int): Task? {
        return taskRepository.getOneByIdAsync(id)
    }

    suspend fun updateOneAsync(task: Task): Int {
        val updatedRows = taskRepository.updateOneAsync(task)
        DiagnosticLog.record(
            applicationContext,
            "FULL_TASK_UPDATE_RESULT",
            task.id,
            mapOf(
                "updatedRows" to updatedRows,
                "source" to "TASK_SERVICE",
                "status" to task.status.name,
                "inProgress" to task.inProgress,
                "dateTime" to task.dateTime,
                "sentCount" to task.sentCount,
            ),
        )
        return updatedRows
    }

    suspend fun updateAfterAlarmIfStillOnAsync(id: Int, dateTime: Long, sentCount: Int): Boolean {
        return taskRepository.updateAfterAlarmIfStillOnAsync(id, dateTime, sentCount)
    }

    fun turnOffInProgressByTaskId(taskId: Int) {
        runBlocking {
            launch {
                val updated = taskRepository.updateInProgressAsync(taskId, false)
                DiagnosticLog.record(
                    applicationContext,
                    "IN_PROGRESS_UPDATE_RESULT",
                    taskId,
                    mapOf(
                        "newValue" to false,
                        "updated" to updated,
                        "source" to "REMOVE_NOTIFICATION",
                    ),
                )
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
