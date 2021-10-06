package com.example.tasknotifier.broadcast_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tasknotifier.android_services.NotificationReviverAndroidService
import com.example.tasknotifier.android_services.TaskSchedulerAndroidService
import com.example.tasknotifier.common.Constants

class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Constants.INTENT_ACTIONS_TO_RESCHEDULE_TASKS.contains(intent.action)) {
            context.startService(Intent(context, NotificationReviverAndroidService::class.java))
            context.startService(Intent(context, TaskSchedulerAndroidService::class.java))
        }
    }
}