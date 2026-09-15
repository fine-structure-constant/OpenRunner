package cn.edu.pku.openrunner.feature.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.pku.openrunner.feature.weather.data.WeatherRepository
import cn.edu.pku.openrunner.feature.weather.data.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val loading: Boolean = false,
    val snapshot: WeatherSnapshot? = null,
    val errorMessage: String? = null
)

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    fun refresh() {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            runCatching { repository.load() }
                .onSuccess { _uiState.value = WeatherUiState(snapshot = it) }
                .onFailure {
                    _uiState.value = WeatherUiState(errorMessage = it.message ?: "天气加载失败")
                }
        }
    }

    class Factory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WeatherViewModel(repository) as T
        }
    }
}
