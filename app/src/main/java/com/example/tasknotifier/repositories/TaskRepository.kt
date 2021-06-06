package com.example.tasknotifier.repositories

import androidx.lifecycle.LiveData
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.data.task.TaskDao

class TaskRepository(private val taskDao: TaskDao) {

    val readAllData: LiveData<List<Task>> = taskDao.readAllData()

    suspend fun getOneByIdAsync(id: Int): Task? {
        return taskDao.getOneByIdAsync(id)
    }

    suspend fun addOneAsync(task: Task): Long {
        return taskDao.addOneAsync(task)
    }

    suspend fun deleteOneByIdAsync(id: Int) {
        taskDao.deleteOneByIdAsync(id)
    }

    suspend fun updateOneAsync(task: Task) {
        taskDao.updateOneAsync(task)
    }
}