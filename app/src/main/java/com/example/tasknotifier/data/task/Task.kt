package com.example.tasknotifier.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_table")
data class Task(
    val description: String,
    val dateTime: Float,
    val repeat: Int,
    val stopAfter: Int,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}