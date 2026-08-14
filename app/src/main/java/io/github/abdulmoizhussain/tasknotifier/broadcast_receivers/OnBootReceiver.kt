package io.github.abdulmoizhussain.tasknotifier.broadcast_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.abdulmoizhussain.tasknotifier.android_services.TaskNotifierAndroidService
import io.github.abdulmoizhussain.tasknotifier.common.Constants

class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Constants.INTENT_ACTIONS_TO_RESCHEDULE_TASKS.contains(intent.action)) {
            Intent(context, TaskNotifierAndroidService::class.java).let { mIntent ->
                mIntent.putExtra(Constants.INTENT_EXTRA_NOTIFICATION_REVIVER_SERVICE, true)
                mIntent.putExtra(Constants.INTENT_EXTRA_TASK_SCHEDULER_SERVICE, true)

                context.startService(mIntent)
            }
        }
    }
}