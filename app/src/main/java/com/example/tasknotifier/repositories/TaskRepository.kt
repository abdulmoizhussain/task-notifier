package com.example.tasknotifier.repositories

import androidx.lifecycle.LiveData
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.data.task.TaskDao

class TaskRepository(private val taskDao: TaskDao) {

    val readAllData: LiveData<List<Task>> = taskDao.readAllData()

    fun getOneById(id: Int): LiveData<Task> {
        return taskDao.getOneById(id)
    }

    suspend fun addOneAsync(task: Task): Long {
        return taskDao.addOneAsync(task)
    }

    suspend fun deleteOneByIdAsync(id: Int) {
        taskDao.deleteOneByIdAsync(id)
    }

    fun updateOne(task: Task) {
        taskDao.updateOne(task)
    }
}