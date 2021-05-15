package com.example.tasknotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SendNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("asdf i am inside onReceive")
    }
}