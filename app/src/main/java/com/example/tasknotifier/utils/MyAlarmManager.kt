package com.example.tasknotifier.utils

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.tasknotifier.MainActivity
import com.example.tasknotifier.broadcast_receivers.SendNotificationBroadcastReceiver

class MyAlarmManager {
    companion object {

        fun setExact(context: Context, requestCode: Int, mIntent: Intent, triggerAtMillis: Long) {
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, mIntent, getFlagUpdateCurrent())
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

        // TODO check all of its usages that if the intent & context being passed are always identical or not.
        fun cancel(context: Context, requestCode: Int) {
            // helpful sources: also check the comments
            // https://stackoverflow.com/a/9575569/8075004

            val mIntent = Intent(context, SendNotificationBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, mIntent, getFlagUpdateCurrent())
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        @Suppress("unused")
        fun setInexact(context: Context, requestCode: Int, mIntent: Intent, triggerAtMillis: Long) {
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, mIntent, getFlagUpdateCurrent())
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }

        @Suppress("unused")
        fun setAlarmClock(context: Context, requestCode: Int, mIntent: Intent, triggerAtMillis: Long) {
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, mIntent, getFlagUpdateCurrent())
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    // helpful source for setAlarmClock: https://stackoverflow.com/a/34699710
                    val tempIntent = Intent(context, MainActivity::class.java)
                    val pendingIntentForAlarmClockIcon = PendingIntent.getActivity(context, 0, tempIntent, getFlagUpdateCurrent())
                    val alarmClockInfo = AlarmClockInfo(triggerAtMillis, pendingIntentForAlarmClockIcon)

                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        }

        @Suppress("unused")
        fun isAlarmOff(context: Context, requestCode: Int): Boolean {
            // TODO this one is buggy
            // helpful sources:
            // https://issuetracker.google.com/issues/36909112
            val mIntent = Intent(context, SendNotificationBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, mIntent, getFlagNoCreate())
            return pendingIntent == null
        }

        private fun getFlagUpdateCurrent(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        }

        private fun getFlagNoCreate(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
        }
    }
}