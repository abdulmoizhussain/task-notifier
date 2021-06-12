package com.example.tasknotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.utils.MyDateFormat
import com.example.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class SendNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val taskService = TaskService(context)

        runBlocking {
            GlobalScope.launch {

                val taskId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, 0)

                val task = taskService.getOneByIdAsync(taskId)

                var description = ""
                var setWhen = 0L
                var sentCount = 0

                if (task != null) {
                    description = task.description
                    setWhen = task.dateTime
                    sentCount = task.sentCount

                    sentCount += 1
                }

                // TODO maybe temporary :P
                val hourThen = MyDateFormat.HH_mm_ss.format(setWhen)
                val hourNow = MyDateFormat.HH_mm_ss.format(Date())
                val contentTitle = "($sentCount) $hourThen -> $hourNow"

                MyNotificationManager.notify(
                    context,
                    taskId,
                    contentTitle,
                    description,
                    setWhen,
                    true
                )

                if (task == null) {
                    return@launch
                }

                var triggerAtMillis = task.dateTime

                // Making sure task.repeat && task.stopAfter have the valid indices.
                if (task.repeat < 0 || task.stopAfter < 0 || task.repeat >= Constants.repeatArray.size || task.stopAfter >= Constants.stopAfterArray.size) {
                    // fail safe (overkill). just ignore for now..
                    return@launch
                }
                // When "Repeat: None" is selected.
                else if (task.repeat == 0) {
                    // "Repeat: None" logic here
                    // Do nothing && Do not reschedule.

                    // return@launch
                    // not returning from here and letting it go to the statement updateOneAsync so that db will be updated
                    // and then list will be updated with its status.
                }
                // When a repeat duration is selected along with "Never Stop" option.
                else if (task.stopAfter == 0 || sentCount < Constants.stopAfterArray[task.stopAfter]) {
                    // Keep incrementing the sentCount and never stop rescheduling, when stop after is set to: "Never Stop"
                    // OR
                    // When a repeat duration is selected along with a "Stop After" option (other than Never Stop option).
                    // Reschedule this task at its next occurrence.
                    triggerAtMillis = Constants.getNextOccurrence(task.repeat).time.time

                    TaskService.createIntentAndSetExactAlarm(context, taskId, triggerAtMillis)
                }

                val taskToUpdate = Task(
                    task.description,
                    triggerAtMillis,
                    task.repeat,
                    task.stopAfter
                )
                taskToUpdate.id = taskId
                taskToUpdate.sentCount = sentCount

                taskService.updateOneAsync(taskToUpdate)
            }
        }
    }
}