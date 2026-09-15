package cn.edu.pku.openrunner.feature.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.pku.openrunner.core.network.TaskDto
import cn.edu.pku.openrunner.feature.tasks.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskListUiState(
    val loading: Boolean = false,
    val tasks: List<TaskDto> = emptyList(),
    val errorMessage: String? = null
)

class TaskListViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    fun refresh() {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            runCatching { repository.loadVisibleTasks() }
                .onSuccess { tasks ->
                    _uiState.value = TaskListUiState(tasks = tasks)
                }
                .onFailure { error ->
                    _uiState.value = TaskListUiState(
                        errorMessage = error.message ?: "任务加载失败"
                    )
                }
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskListViewModel(repository) as T
        }
    }
}
