package com.example.tasknotifier

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast

class OnStartupService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Toast.makeText(this, "OnStartupService onStartCommand", Toast.LENGTH_LONG).show()

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}