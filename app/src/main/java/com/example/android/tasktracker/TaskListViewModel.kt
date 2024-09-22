package com.example.android.tasktracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "TaskListViewModel"

// Creates a data class that stores the filter that will be used to filter out tasks
data class TaskFilter(
    val showImportant: Boolean = false,
    val showCompleted: Boolean = false,
    val showInProgress: Boolean = false,
    val applyImportantFilter: Boolean = false,
    val applyCompletedFilter: Boolean = false,
    val applyInProgressFilter: Boolean = false,
    val category: String? = "General"
)

class TaskListViewModel: ViewModel(){

    private val taskRepository = TaskRepository.get()

    // Uses a mutable state flow to allow for dynamic updates and become responsive to changes
    // while still ensuring that onDestroy calls wont reset any data
    private val _tasks: MutableStateFlow<List<Task>> = MutableStateFlow(emptyList())
    val tasks: StateFlow<List<Task>>
        get() = _tasks.asStateFlow()

    private val _filter: MutableStateFlow<TaskFilter> = MutableStateFlow(TaskFilter())
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    init {
        viewModelScope.launch {
            taskRepository.getTasks().collect { allTasks ->
                _tasks.value = allTasks
            }
        }
    }

    suspend fun addTask(task: Task) {
        taskRepository.addTask(task)
    }

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun setCategory(category: String) {
        _filter.value = _filter.value.copy(category = category)
    }

    // Filters out tasks based on the category and status
    val filteredTasks: StateFlow<List<Task>> = combine(_tasks, _filter) { tasks, filter ->
        tasks.filter { task ->
            (filter.category == null || task.category == filter.category) &&
                    (!filter.applyImportantFilter || task.isImportant == filter.showImportant) &&
                    (!filter.applyCompletedFilter || task.isCompleted == filter.showCompleted) &&
                    (!filter.applyInProgressFilter || task.inProgress == filter.showInProgress)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}