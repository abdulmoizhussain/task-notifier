package com.example.tasknotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*

class SendNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, 0)
        val description = intent.getStringExtra(Constants.INTENT_EXTRA_TASK_DESCRIPTION)
        val setWhen = intent.getLongExtra(Constants.INTENT_EXTRA_SET_WHEN, 0)

        MyNotificationManager.notify(
            context,
            taskId,
            SimpleDateFormat("dd/MMM HH:mm", Locale.getDefault()).format(setWhen),
            description,
            setWhen,
            false
        )
    }
}