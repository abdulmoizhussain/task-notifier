package io.github.abdulmoizhussain.tasknotifier.broadcast_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.abdulmoizhussain.tasknotifier.android_services.TaskNotifierAndroidService
import io.github.abdulmoizhussain.tasknotifier.common.Constants
import io.github.abdulmoizhussain.tasknotifier.common.Globals
import io.github.abdulmoizhussain.tasknotifier.common.TaskStatusEnum
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticLog
import io.github.abdulmoizhussain.tasknotifier.services.TaskService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SendNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val taskService = TaskService(context)

        runBlocking {
            // GlobalScope.launch was here
            launch {

                val taskId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, 0)
                DiagnosticLog.record(context, "ALARM_BROADCAST_RECEIVED", taskId)

                val task = taskService.getOneByIdAsync(taskId)
                if (task == null || task.status != TaskStatusEnum.On) {
                    DiagnosticLog.record(
                        context,
                        "ALARM_BROADCAST_IGNORED",
                        taskId,
                        mapOf("taskFound" to (task != null), "status" to task?.status?.name),
                    )
                    return@launch
                }

                val description = task.description
                val setWhen = task.dateTime
                val sentCount = task.sentCount + 1

                var triggerAtMillis = task.dateTime

                // Making sure task.repeat && task.stopAfter have the valid indices.
                if (task.repeat < 0 || task.stopAfter < 0 || task.repeat >= Constants.repeatArray.size || task.stopAfter >= Constants.stopAfterArray.size) {
                    // fail safe (overkill). just ignore for now..
                    DiagnosticLog.record(
                        context,
                        "ALARM_BROADCAST_INVALID_REPEAT_CONFIGURATION",
                        taskId,
                        mapOf("repeat" to task.repeat, "stopAfter" to task.stopAfter),
                    )
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
                    DiagnosticLog.record(
                        context,
                        "ALARM_DATABASE_UPDATE_SKIPPED",
                        taskId,
                        mapOf("triggerAtMillis" to triggerAtMillis, "sentCount" to sentCount),
                    )
                    return@launch
                }
                DiagnosticLog.record(
                    context,
                    "ALARM_DATABASE_UPDATED",
                    taskId,
                    mapOf(
                        "inProgress" to true,
                        "triggerAtMillis" to triggerAtMillis,
                        "sentCount" to sentCount,
                    ),
                )

                val contentTitle = Globals.createTitleForTask(setWhen, sentCount)

                Intent(context, TaskNotifierAndroidService::class.java).let { serviceIntent ->
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_TASK_ID, taskId)
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_CONTENT_TITLE, contentTitle)
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_DESCRIPTION, description)
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_SET_WHEN, setWhen)
                    serviceIntent.putExtra(Constants.INTENT_EXTRA_ON_GOING, true)

                    context.startService(serviceIntent)
                    DiagnosticLog.record(context, "ALARM_NOTIFICATION_SERVICE_REQUESTED", taskId)
                }

                Intent(context, TaskNotifierAndroidService::class.java).let { mIntent ->
                    mIntent.putExtra(Constants.INTENT_EXTRA_TASK_SCHEDULER_SERVICE, true)
                    context.startService(mIntent)
                    DiagnosticLog.record(context, "ALARM_SCHEDULER_SERVICE_REQUESTED", taskId)
                }
            }
        }
    }
}
