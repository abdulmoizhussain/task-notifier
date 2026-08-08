package com.example.tasknotifier.broadcast_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tasknotifier.android_services.TaskNotifierAndroidService
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.common.Globals
import com.example.tasknotifier.common.TaskStatusEnum
import com.example.tasknotifier.services.TaskService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SendNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val taskService = TaskService(context)

        runBlocking {
            // GlobalScope.launch was here
            launch {

                val taskId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, 0)

                val task = taskService.getOneByIdAsync(taskId)
                if (task == null || task.status != TaskStatusEnum.On) {
                    return@launch
                }

                val description = task.description
                val setWhen = task.dateTime
                val sentCount = task.sentCount + 1

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
                    triggerAtMillis = Constants.getNextOccurrence(task.repeat).timeInMillis

//                    TaskService.createIntentAndSetExactAlarm(context, taskId, triggerAtMillis)
                }

                if (!taskService.updateAfterAlarmIfStillOnAsync(taskId, triggerAtMillis, sentCount)) {
                    return@launch
                }

                val contentTitle = Globals.createTitleForTask(setWhen, sentCount)

                Intent(context, TaskNotifierAndroidService::class.java).let { serviceIntent ->
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_TASK_ID, taskId)
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_CONTENT_TITLE, contentTitle)
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_DESCRIPTION, description)
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_SET_WHEN, setWhen)
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_ON_GOING, true)

                    context.startService(serviceIntent)
                }

                Intent(context, TaskNotifierAndroidService::class.java).let { mIntent ->
                    mIntent.putExtra(Constants.INTENT_EXTRA_TASK_SCHEDULER_SERVICE, true)
                    context.startService(mIntent)
                }
            }
        }
    }
}
