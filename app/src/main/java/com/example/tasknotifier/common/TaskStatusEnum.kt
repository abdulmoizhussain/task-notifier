package com.example.tasknotifier.common

import java.util.*

// TODO create readable-task-status helper method.
enum class TaskStatusEnum {
    On,
    Off;

    companion object {
        fun getReadableStatus(taskStatus: TaskStatusEnum, dateTime: Long): String {
            if (taskStatus == TaskStatusEnum.Off) {
                return taskStatus.toString()
            } else if (dateTime < Date().time) {
                return "Expired"
            }
            return taskStatus.toString()
        }
    }
}
