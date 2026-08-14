package io.github.abdulmoizhussain.tasknotifier.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import io.github.abdulmoizhussain.tasknotifier.data.AppDatabase
import io.github.abdulmoizhussain.tasknotifier.data.task.Task
import io.github.abdulmoizhussain.tasknotifier.data.task.TaskOrder
import io.github.abdulmoizhussain.tasknotifier.diagnostics.DiagnosticLog
import io.github.abdulmoizhussain.tasknotifier.repositories.TaskRepository
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
            val updatedRows = taskRepository.updateOneAsync(task)
            recordFullUpdate(task, updatedRows, "VIEW_MODEL_ASYNC")
        }
    }

    suspend fun updateOneAsync(task: Task): Int {
        val updatedRows = taskRepository.updateOneAsync(task)
        recordFullUpdate(task, updatedRows, "VIEW_MODEL_AWAITED")
        return updatedRows
    }

    suspend fun updateInProgressAsync(id: Int, inProgress: Boolean): Boolean {
        val updated = taskRepository.updateInProgressAsync(id, inProgress)
        DiagnosticLog.record(
            getApplication(),
            "IN_PROGRESS_UPDATE_RESULT",
            id,
            mapOf(
                "newValue" to inProgress,
                "updated" to updated,
                "source" to "VIEW_MODEL",
            ),
        )
        return updated
    }
    
    suspend fun getOneByIdAsync(id: Int): Task? {
        return taskRepository.getOneByIdAsync(id)
    }

    private fun recordFullUpdate(task: Task, updatedRows: Int, source: String) {
        DiagnosticLog.record(
            getApplication(),
            "FULL_TASK_UPDATE_RESULT",
            task.id,
            mapOf(
                "updatedRows" to updatedRows,
                "source" to source,
                "status" to task.status.name,
                "inProgress" to task.inProgress,
                "dateTime" to task.dateTime,
                "sentCount" to task.sentCount,
            ),
        )
    }
}
