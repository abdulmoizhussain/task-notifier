package com.example.tasknotifier.common

enum class TaskStatusEnum {
    On,
    Off;

    companion object {
        fun getReadableStatus(taskStatus: TaskStatusEnum, dateTime: Long): String {
            if (taskStatus == Off) {
                return Off.toString()
            } else if (dateTime < System.currentTimeMillis()) {
                return "Expired"
            }
            return taskStatus.toString()
        }
    }
}
