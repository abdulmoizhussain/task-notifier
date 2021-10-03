package com.example.tasknotifier.android_services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.tasknotifier.common.Constants
import com.example.tasknotifier.utils.MyNotificationManager

class NotificationService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        MyNotificationManager.notify(
            this,
            1011,
            "contentTitle",
            "description",
            0L,
            true
        )

        if (intent != null) {
            val taskId = intent.getIntExtra(Constants.INTENT_EXTRA_TASK_ID, 0)
            val contentTitle = intent.getStringExtra(Constants.INTENT_EXTRA_CONTENT_TITLE)
            val description = intent.getStringExtra(Constants.INTENT_EXTRA_DESCRIPTION)
            val setWhen = intent.getLongExtra(Constants.INTENT_EXTRA_SET_WHEN, 0L)
            val onGoing = intent.getBooleanExtra(Constants.INTENT_EXTRA_ON_GOING, false)

            //                testing in progress
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