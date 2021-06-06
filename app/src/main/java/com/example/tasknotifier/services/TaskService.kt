package com.example.tasknotifier.services

import android.content.Context
import com.example.tasknotifier.data.AppDatabase
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.repositories.TaskRepository

class TaskService(context: Context) {
    private val taskRepository: TaskRepository

    init {
        val taskDao = AppDatabase.getDatabase(context).taskDao()
        taskRepository = TaskRepository(taskDao)
    }

    suspend fun getOneByIdAsync(id: Int): Task? {
        return taskRepository.getOneByIdAsync(id)
    }

    suspend fun updateOneAsync(task: Task) {
        taskRepository.updateOneAsync(task)
    }
}