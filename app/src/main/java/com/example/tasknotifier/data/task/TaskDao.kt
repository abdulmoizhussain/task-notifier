package com.example.tasknotifier.data.task

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTask(task: Task): Long

    @Query("SELECT * FROM task_table ORDER BY id DESC")
    fun readAllData(): LiveData<List<Task>>

    @Delete
    fun deleteOne(task: Task)
}