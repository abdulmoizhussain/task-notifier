package com.example.tasknotifier.repositories

import androidx.lifecycle.LiveData
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.data.task.TaskDao

class TaskRepository(private val taskDao: TaskDao) {

    val readAllData: LiveData<List<Task>> = taskDao.readAllData()

    suspend fun addTask(task: Task): Long {
        return taskDao.addTask(task)
    }

    fun deleteOne(task: Task) {
        taskDao.deleteOne(task)
    }
}