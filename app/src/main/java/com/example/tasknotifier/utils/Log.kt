package com.example.tasknotifier.utils

import android.util.Log

object Log {
    fun a(arg: Any?) {
        Log.i("task_notifier", arg.toString())
    }
}