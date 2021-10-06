package com.example.tasknotifier.android_services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.utils.MyNotificationManager

class NotificationSenderAndroidService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId: Int = intent?.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, -1) ?: -1

        if (intent != null && taskId > -1) {
            val contentTitle = intent.getStringExtra(Constants.INTENT_EXTRA_CONTENT_TITLE)
            val description = intent.getStringExtra(Constants.INTENT_EXTRA_DESCRIPTION)
            val setWhen = intent.getLongExtra(Constants.INTENT_EXTRA_SET_WHEN, 0L)
            val onGoing = intent.getBooleanExtra(Constants.INTENT_EXTRA_ON_GOING, false)

            MyNotificationManager.notify(
                this,
                taskId,
                contentTitle,
                description,
                setWhen,
                onGoing
            )
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}