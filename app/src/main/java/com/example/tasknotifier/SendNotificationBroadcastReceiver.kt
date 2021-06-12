package com.example.tasknotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.common.TaskStatusEnum
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.utils.MyDateFormat
import com.example.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class SendNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, 0)
        val description = intent.getStringExtra(Constants.INTENT_EXTRA_TASK_DESCRIPTION)
        val setWhen = intent.getLongExtra(Constants.INTENT_EXTRA_SET_WHEN, 0)

        // TODO maybe temporary :P
        val hourThen = MyDateFormat.HH_mm_ss.format(setWhen)
        val hourNow = MyDateFormat.HH_mm_ss.format(Date())

        // TODO $sentCount
//        val contentTitle = "($sentCount) $hourThen -> $hourNow"
        val contentTitle = "$hourThen -> $hourNow"

        MyNotificationManager.notify(
            context,
            taskId,
            contentTitle,
            description,
            setWhen,
            true
        )

        // expire this task now.
        // TODO: but before that, apply repetition logic when this task is repeatable.
        val taskService = TaskService(context)
        runBlocking {
            GlobalScope.launch {
                val task = taskService.getOneByIdAsync(taskId) ?: return@launch

                task.sentCount += 1

                // TODO in progress
                // Making sure task.repeat && task.stopAfter have the valid indices.
                if (task.repeat < 0 || task.stopAfter < 0 || task.repeat >= Constants.repeatArray.size || task.stopAfter >= Constants.stopAfterArray.size) {
                    // fail safe
                    // just ignore
                }
                // When "Repeat: None" is selected.
                else if (task.repeat == 0) {
                    // "Repeat: None" logic here
                }
                // When a repeat duration is selected along with "Never Stop" option.
                else if (task.repeat > 0 && task.stopAfter == 0) {
                    // keep incrementing the sentCount and never stop sending.
                    val calendar = Constants.getNextOccurrence(task.repeat)
                }
                // When a repeat duration is selected along with a "Stop After" option (other than Never Stop option).
                else if (task.repeat > 0 && Constants.stopAfterArray[task.stopAfter] < task.sentCount) {
                    val calendar = Constants.getNextOccurrence(task.repeat)
                }

                task.status = TaskStatusEnum.Expired

                taskService.updateOneAsync(task)
            }
        }
    }
}