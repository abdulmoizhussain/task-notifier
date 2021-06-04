package com.example.tasknotifier.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.tasknotifier.data.AppDatabase
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.repositories.TaskRepository

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    val readAllData: LiveData<List<Task>>
    private val repository: TaskRepository

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        readAllData = repository.readAllData
    }

    suspend fun addOneAsync(task: Task): Long {
        return repository.addOneAsync(task)
    }

    suspend fun deleteOneByIdAsync(id: Int) {
        repository.deleteOneByIdAsync(id)
    }

    fun getOneById(id: Int): LiveData<Task> {
        return repository.getOneById(id)
    }
}