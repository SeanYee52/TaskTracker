package com.example.android.tasktracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.android.tasktracker.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TaskDetailViewModel(taskID: UUID) : ViewModel() {
    private val taskRepository = TaskRepository.get()

    // Uses a mutable state flow to allow for dynamic updates and become responsive to changes
    // while still ensuring that onDestroy calls wont reset any data
    private val _task: MutableStateFlow<Task?> = MutableStateFlow(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    // Gets the task from the task repository
    init {
        viewModelScope.launch {
            _task.value = taskRepository.getTask(taskID)
        }
    }

    // Updates the task according to the function that is provided by the task detail fragment
    fun updateTask (onUpdate: (Task) -> Task){
        _task.update { oldTask ->
            oldTask?.let {onUpdate(it)}
        }
    }

    // Deletes the task through the repository
    suspend fun deleteTask (){
        task.value?.let { taskRepository.deleteTask(it) }
    }

    // Makes sure to update the task once the user exits the task detail fragment
    override fun onCleared() {
        super.onCleared()
        task.value?.let { taskRepository.updateTask(it) }
    }
}

// Creates a viewmodel based on the provided task id
class TaskDetailViewModelFactory (
    private val taskID: UUID
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskDetailViewModel(taskID) as T
    }
}