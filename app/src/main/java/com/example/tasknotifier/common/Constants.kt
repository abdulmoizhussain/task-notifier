package com.example.tasknotifier.common

import java.util.*

object Constants {
    const val INTENT_EXTRA_TASK_DESCRIPTION = "INTENT_EXTRA_TASK_DESCRIPTION"
    const val INTENT_EXTRA_SET_WHEN = "INTENT_EXTRA_SET_WHEN"
    const val INTENT_EXTRA_TASK_ID = "INTENT_EXTRA_TASK_ID"
    const val NOTIFICATION_CHANNEL_DEFAULT = "NOTIFICATION_CHANNEL_DEFAULT"

    fun getNextOccurrence(repeatIndex: Int): Calendar? {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        when (repeatIndex) {
            // None selected
            0 -> return null
            // Hourly
            1 -> {
                calendar.add(Calendar.HOUR_OF_DAY, 1)
                return calendar
            }
            // Daily
            2 -> {
                calendar.add(Calendar.DATE, 1)
                return calendar
            }
            // Weekly
            3 -> {
                calendar.add(Calendar.DATE, 7)
                return calendar
            }
            // Monthly
            4 -> {
                calendar.add(Calendar.MONTH, 1)
                return calendar
            }
            // Yearly
            5 -> {
                calendar.add(Calendar.YEAR, 1)
                return calendar
            }
            // Every 2 hours
            6 -> {
                calendar.add(Calendar.HOUR_OF_DAY, 2)
                return calendar
            }
            // Every 3 hours
            7 -> {
                calendar.add(Calendar.HOUR_OF_DAY, 3)
                return calendar
            }
            // Every 4 hours
            8 -> {
                calendar.add(Calendar.HOUR_OF_DAY, 4)
                return calendar
            }
            // Every 2 days
            9 -> {
                calendar.add(Calendar.DATE, 2)
                return calendar
            }
            // Every 3 days
            10 -> {
                calendar.add(Calendar.DATE, 3)
                return calendar
            }
            // Every 2 weeks
            11 -> {
                calendar.add(Calendar.DATE, 7 * 2)
                return calendar
            }
            // Every 3 weeks
            12 -> {
                calendar.add(Calendar.DATE, 7 * 3)
                return calendar
            }
            // Every 4 weeks
            13 -> {
                calendar.add(Calendar.DATE, 7 * 4)
                return calendar
            }
            // Every 2 months
            14 -> {
                calendar.add(Calendar.MONTH, 2)
                return calendar
            }
            // Every 3 months
            15 -> {
                calendar.add(Calendar.MONTH, 3)
                return calendar
            }
            // Every 4 months
            16 -> {
                calendar.add(Calendar.MONTH, 4)
                return calendar
            }
            // Every minute
            17 -> {
                calendar.add(Calendar.MINUTE, 1)
                return calendar
            }
            // Every 5 minutes
            18 -> {
                calendar.add(Calendar.MINUTE, 5)
                return calendar
            }
            // Every 10 minutes
            19 -> {
                calendar.add(Calendar.MINUTE, 10)
                return calendar
            }
            // Every 15 minutes
            20 -> {
                calendar.add(Calendar.MINUTE, 15)
                return calendar
            }
            // Every 20 minutes
            21 -> {
                calendar.add(Calendar.MINUTE, 20)
                return calendar
            }
            // Every 30 minutes
            22 -> {
                calendar.add(Calendar.MINUTE, 30)
                return calendar
            }
            // Index out of range.
            else -> return null
        }
    }
}