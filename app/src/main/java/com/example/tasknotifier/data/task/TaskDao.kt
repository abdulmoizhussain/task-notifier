package com.example.tasknotifier.data.task

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.tasknotifier.common.TaskStatusEnum

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addOneAsync(task: Task): Long

    @Query("DELETE FROM task_table WHERE id=:id")
    suspend fun deleteOneByIdAsync(id: Int)

    @Update
    suspend fun updateOneAsync(task: Task)

    @Query("UPDATE task_table SET dateTime=:dateTime, sentCount=:sentCount, inProgress=1 WHERE id=:id AND status=:requiredStatus")
    suspend fun updateAfterAlarmIfStatusAsync(
        id: Int,
        dateTime: Long,
        sentCount: Int,
        requiredStatus: TaskStatusEnum,
    ): Int

    @Query("SELECT * FROM task_table where id=:id")
    suspend fun getOneByIdAsync(id: Int): Task?

    @Query("SELECT * FROM task_table ORDER BY dateCreated DESC, id DESC")
    fun readAllByDateCreated(): LiveData<List<Task>>

    @Query("SELECT * FROM task_table ORDER BY dateModified DESC, id DESC")
    fun readAllByDateModified(): LiveData<List<Task>>

    @Query("SELECT * FROM task_table ORDER BY id ASC")
    suspend fun readAllAsync(): List<Task>

    @Query("SELECT * FROM task_table WHERE status=:status AND dateTime>=:dateTime")
    suspend fun fetchAllByStatusWhichAreDueAsync(status: TaskStatusEnum, dateTime: Long): Array<Task>

    @Query("SELECT * FROM task_table WHERE inProgress=1")
    suspend fun fetchAllTheInProgressAsync(): Array<Task>
}
