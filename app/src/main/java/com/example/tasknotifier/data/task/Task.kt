package com.example.tasknotifier.data.task

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tasknotifier.common.TaskStatusEnum

@Entity(tableName = "task_table", indices = [Index(value = ["task_id"], unique = true)])
data class Task(
    @ColumnInfo(name = "task_id")
    val taskId: Int,
    val description: String,
    val dateTime: Long,
    val repeat: Int,
    val stopAfter: Int,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var sentCount: Int = 0
    var status: TaskStatusEnum = TaskStatusEnum.On
}