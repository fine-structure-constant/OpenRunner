package cn.edu.pku.openrunner.feature.weather.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.weather.data.WeatherLocation
import cn.edu.pku.openrunner.feature.weather.data.WeatherRepository
import cn.edu.pku.openrunner.feature.weather.data.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class WeatherUiState(
    @get:StringRes val loadingMessage: Int? = null,
    @get:StringRes val errorMessage: Int? = null,
    val snapshot: WeatherSnapshot? = null,
    val place: String? = null
)

internal class WeatherViewModel(
    private val repository: WeatherRepository,
    private val location: WeatherLocation
) : ViewModel() {
    private val state = MutableStateFlow(WeatherUiState())
    val uiState = state.asStateFlow()
    val hasLocationPermission: Boolean get() = location.hasPermission
    private var refreshJob: Job? = null

    fun refresh() {
        if (state.value.loadingMessage != null) return
        refreshJob?.cancel()
        if (!location.hasPermission) {
            state.value = WeatherUiState(errorMessage = R.string.weather_location_permission)
            return
        }
        if (!location.isEnabled) {
            state.value = WeatherUiState(errorMessage = R.string.weather_location_off)
            return
        }

        state.value = WeatherUiState(loadingMessage = R.string.weather_locating)
        refreshJob = viewModelScope.launch {
            val fix = location.current()
            if (fix == null) {
                state.value = WeatherUiState(errorMessage = R.string.weather_location_unavailable)
                return@launch
            }
            state.value = WeatherUiState(loadingMessage = R.string.weather_loading)
            val snapshot = try {
                repository.load(fix.latitude, fix.longitude)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = WeatherUiState(errorMessage = R.string.weather_load_failed)
                return@launch
            }
            state.value = WeatherUiState(snapshot = snapshot)
            location.placeName(fix)?.let { state.value = WeatherUiState(snapshot = snapshot, place = it) }
        }
    }

    class Factory(private val repository: WeatherRepository, private val location: WeatherLocation) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeatherViewModel(repository, location) as T
    }
}
