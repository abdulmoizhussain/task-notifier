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

// TODO in progress
//                if (task.repeat > 0) {
//                    Constants.repeatDurationByIndex()
//                }

                task.status = TaskStatusEnum.Expired
                taskService.updateOneAsync(task)
            }
        }
    }
}