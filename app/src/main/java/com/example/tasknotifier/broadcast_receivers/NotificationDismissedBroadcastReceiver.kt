package com.example.tasknotifier.broadcast_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.common.Globals
import com.example.tasknotifier.services.TaskService
import com.example.tasknotifier.utils.MyNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationDismissedBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.INTENT_ACTION_NOTIFICATION_DISMISSED) {
            return
        }

        val taskId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, -1)
        if (taskId < 1) {
            return
        }

        val pendingResult = goAsync()
        val applicationContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = TaskService(applicationContext).getOneByIdAsync(taskId)
                if (task != null && task.inProgress) {
                    MyNotificationManager.notifySilently(
                        applicationContext,
                        task.id,
                        Globals.createTitleForTask(task.dateTime, task.sentCount),
                        task.description,
                        task.dateTime,
                        true,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
