package com.example.tasknotifier.common

import com.example.tasknotifier.utils.MyDateFormat

object Globals {
    fun createTitleForTask(setWhen: Long, sentCount: Int): String {
        val hourThen = MyDateFormat.HH_mm_ss.format(setWhen)
        val hourNow = MyDateFormat.HH_mm_ss.format(System.currentTimeMillis())
        return "($sentCount) $hourThen -> $hourNow"
    }
}