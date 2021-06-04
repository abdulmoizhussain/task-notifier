package com.example.tasknotifier.data.task

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addOneAsync(task: Task): Long

    @Query("DELETE FROM task_table WHERE id=:id")
    suspend fun deleteOneByIdAsync(id: Int)

    @Query("SELECT * FROM task_table WHERE id=:id")
    fun getOneById(id: Int): LiveData<Task>

    @Query("SELECT * FROM task_table ORDER BY id DESC")
    fun readAllData(): LiveData<List<Task>>
}