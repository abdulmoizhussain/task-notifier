package com.example.tasknotifier.data

import androidx.lifecycle.LiveData

class UserRepository(private val taskDao: TaskDao) {

    val readAllData: LiveData<List<Task>> = taskDao.readAllData()

    suspend fun addTask(task: Task) {
        taskDao.addTask(task)
    }
}