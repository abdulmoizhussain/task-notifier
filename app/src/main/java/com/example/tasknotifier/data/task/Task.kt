package com.example.tasknotifier.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tasknotifier.common.TaskStatusEnum
import org.json.JSONObject

//@Entity(tableName = "task_table", indices = [Index(value = ["task_id"], unique = true)])
//data class Task(
//    @ColumnInfo(name = "task_id")
//    val taskId: Int,
//)

@Entity(tableName = "task_table")
data class Task(var description: String = "") {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var dateTime: Long = 0
    var repeat: Int = 0
    var stopAfter: Int = 0
    var sentCount: Int = 0
    var status: TaskStatusEnum = TaskStatusEnum.On
    var inProgress: Boolean = false

    fun toJsonObject(): JSONObject {
        val result = JSONObject()
        result.put("id", this.id)
        result.put("dateTime", this.dateTime)
        result.put("description", this.description)
        result.put("repeat", this.repeat)
        result.put("stopAfter", this.stopAfter)
        result.put("sentCount", this.sentCount)
        result.put("status", this.status.toString())
        result.put("inProgress", this.inProgress)
        return result
    }
}