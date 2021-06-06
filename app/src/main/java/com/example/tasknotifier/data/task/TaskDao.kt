package com.example.tasknotifier.data.task

import androidx.lifecycle.LiveData
import androidx.room.*

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
}