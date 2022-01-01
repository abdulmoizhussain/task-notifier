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

    @Query("SELECT * FROM task_table where id=:id")
    suspend fun getOneByIdAsync(id: Int): Task?

    @Query("SELECT * FROM task_table ORDER BY id DESC")
    fun readAllData(): LiveData<List<Task>>

    @Query("SELECT * FROM task_table ORDER BY id ASC")
    suspend fun readAllAsync(): List<Task>

    @Query("SELECT * FROM task_table WHERE status=:status AND dateTime>=:dateTime")
    suspend fun fetchAllByStatusWhichAreDueAsync(status: TaskStatusEnum, dateTime: Long): Array<Task>

    @Query("SELECT * FROM task_table WHERE inProgress=1")
    suspend fun fetchAllTheInProgressAsync(): Array<Task>
}