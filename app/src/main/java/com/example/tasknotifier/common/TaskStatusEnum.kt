package com.example.tasknotifier.common

import android.content.Context
import com.example.tasknotifier.utils.MyAlarmManager

enum class TaskStatusEnum {
    On,
    Off;

    companion object {
        fun getReadableStatus(context: Context, id: Int, taskStatus: TaskStatusEnum, dateTime: Long): String {
            if (MyAlarmManager.isAlarmOff(context, id)) {
                return Off.toString()
            }
//            if (taskStatus == Off) {
//                return Off.toString()
//            }
            else if (dateTime < System.currentTimeMillis()) {
                return "Expired"
            }
            return taskStatus.toString()
        }
    }
}
