package com.example.tasknotifier.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.tasknotifier.data.AppDatabase
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.repositories.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    val readAllData: LiveData<List<Task>>
    private val repository: TaskRepository

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        readAllData = repository.readAllData
    }

    fun addTask(task: Task): Long {
        var taskId: Long = 0
        viewModelScope.launch(Dispatchers.IO) {
            taskId = repository.addTask(task)
        }
        return taskId
    }

    fun deleteOne(task: Task) {
        repository.deleteOne(task)
    }
}