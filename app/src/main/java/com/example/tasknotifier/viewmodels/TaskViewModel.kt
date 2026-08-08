package com.example.tasknotifier.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.example.tasknotifier.data.AppDatabase
import com.example.tasknotifier.data.task.Task
import com.example.tasknotifier.data.task.TaskOrder
import com.example.tasknotifier.repositories.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    val readAllData: LiveData<List<Task>>
    private val taskRepository: TaskRepository
    private val taskOrder = MutableLiveData(TaskOrder.LATEST_CREATED)

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        taskRepository = TaskRepository(taskDao)
        readAllData = Transformations.switchMap(taskOrder) { order ->
            taskRepository.readAllData(order)
        }
    }

    fun setTaskOrder(order: TaskOrder) {
        if (taskOrder.value != order) {
            taskOrder.value = order
        }
    }

    suspend fun addOneAsync(task: Task): Long {
        return taskRepository.addOneAsync(task)
    }

    fun deleteOneById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteOneByIdAsync(id)
        }
    }

    fun updateOne(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.updateOneAsync(task)
        }
    }

    suspend fun updateOneAsync(task: Task) {
        taskRepository.updateOneAsync(task)
    }
    
    suspend fun getOneByIdAsync(id: Int): Task? {
        return taskRepository.getOneByIdAsync(id)
    }
}
