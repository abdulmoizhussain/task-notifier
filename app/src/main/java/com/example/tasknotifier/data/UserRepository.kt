package com.example.tasknotifier.data

import androidx.lifecycle.LiveData

class UserRepository(private val userDao: UserDao) {

    val readAllData: LiveData<List<Task>> = userDao.readAllData()

    suspend fun addUser(task: Task) {
        userDao.addUser(task)
    }
}