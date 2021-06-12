package com.example.tasknotifier.common

import android.content.Intent
import java.util.*

object Constants {
    const val INTENT_EXTRA_TASK_ID = "INTENT_EXTRA_TASK_ID"
    const val NOTIFICATION_CHANNEL_DEFAULT = "NOTIFICATION_CHANNEL_DEFAULT"
    val INTENT_ACTIONS_TO_RESCHEDULE_TASKS = listOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    val repeatArray = listOf<(calendar: Calendar) -> Unit>(
        {
            // 0: None selected
        },
        { calendar -> // 1: Hourly
            calendar.add(Calendar.HOUR_OF_DAY, 1)
        },
        { calendar -> // 2: Daily
            calendar.add(Calendar.DATE, 1)
        },
        { calendar -> // 3: Weekly
            calendar.add(Calendar.DATE, 7)
        },
        { calendar -> // 4: Monthly
            calendar.add(Calendar.MONTH, 1)
        },
        { calendar -> // 5: Yearly
            calendar.add(Calendar.YEAR, 1)
        },
        { calendar ->  // 6: Every 2 hours
            calendar.add(Calendar.HOUR_OF_DAY, 2)
        },
        { calendar ->  // 7: Every 3 hours
            calendar.add(Calendar.HOUR_OF_DAY, 3)
        },
        { calendar -> // 8: Every 4 hours
            calendar.add(Calendar.HOUR_OF_DAY, 4)
        },
        { calendar -> // 9: Every 2 days
            calendar.add(Calendar.DATE, 2)
        },
        { calendar -> // 10: Every 3 days
            calendar.add(Calendar.DATE, 3)
        },
        { calendar -> // 11: Every 2 weeks
            calendar.add(Calendar.DATE, 7 * 2)
        },
        { calendar -> // 12: Every 3 weeks
            calendar.add(Calendar.DATE, 7 * 3)
        },
        { calendar -> // 13: Every 4 weeks
            calendar.add(Calendar.DATE, 7 * 4)
        },
        { calendar -> // 14: Every 2 months
            calendar.add(Calendar.MONTH, 2)
        },
        { calendar -> // 15: Every 3 months
            calendar.add(Calendar.MONTH, 3)
        },
        { calendar ->// 16: Every 4 months
            calendar.add(Calendar.MONTH, 4)
        },
        { calendar -> // 17: Every minute
            calendar.add(Calendar.MINUTE, 1)
        },
        { calendar ->// 18: Every 5 minutes
            calendar.add(Calendar.MINUTE, 5)
        },
        { calendar ->// 19: Every 10 minutes
            calendar.add(Calendar.MINUTE, 10)
        },
        { calendar ->// 20: Every 15 minutes
            calendar.add(Calendar.MINUTE, 15)
        },
        { calendar ->// 21: Every 20 minutes
            calendar.add(Calendar.MINUTE, 20)
        },
        { calendar ->// 22: Every 30 minutes
            calendar.add(Calendar.MINUTE, 30)
        }
    )
    val stopAfterArray = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    fun getNextOccurrence(repeatIndex: Int): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        repeatArray[repeatIndex](calendar)

        return calendar
    }
}