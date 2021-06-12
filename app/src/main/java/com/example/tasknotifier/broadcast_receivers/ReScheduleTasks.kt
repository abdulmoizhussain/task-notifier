package com.example.tasknotifier.broadcast_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.services.TaskService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ReScheduleTasks : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        if (!Constants.INTENT_ACTIONS_TO_RESCHEDULE_TASKS.contains(intent.action)) {
            return
        }

        val taskService = TaskService(context)

        runBlocking {
            GlobalScope.launch {
                val tasks = taskService.fetchAllWhichAreDueAndOnAsync()

                tasks.forEach { task ->
                    TaskService.createIntentAndSetExactAlarm(context, task.id, task.dateTime)
                }
            }
        }
    }
}