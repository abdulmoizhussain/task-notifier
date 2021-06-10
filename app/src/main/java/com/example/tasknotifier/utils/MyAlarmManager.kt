package com.example.tasknotifier.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.tasknotifier.SendNotificationBroadcastReceiver

class MyAlarmManager {
    companion object {

        fun setInexact(context: Context, requestCode: Int, intent: Intent, triggerAtMillis: Long) {
            val pendingIntent = PendingIntent.getBroadcast(context.applicationContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }

        fun setExact(context: Context, requestCode: Int, intent: Intent, triggerAtMillis: Long) {
            val pendingIntent = PendingIntent.getBroadcast(context.applicationContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        }

        fun cancelByRequestCode(context: Context, requestCode: Int) {
            val pendingIntent = Intent(context.applicationContext, SendNotificationBroadcastReceiver::class.java).let { intent ->
                PendingIntent.getBroadcast(context.applicationContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}