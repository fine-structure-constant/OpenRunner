package cn.edu.pku.openrunner.feature.weather.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.weather.data.WeatherForecast
import cn.edu.pku.openrunner.feature.weather.data.WeatherLocation
import cn.edu.pku.openrunner.feature.weather.data.WeatherRepository
import cn.edu.pku.openrunner.feature.weather.data.WeatherSnapshot
import kotlinx.coroutines.launch

class WeatherFragment : Fragment(R.layout.fragment_weather) {
    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModel.Factory(WeatherRepository(), WeatherLocation(requireContext().applicationContext))
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refresh()
    }
    private val settings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refresh()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val loading = view.findViewById<TextView>(R.id.weather_loading)
        val error = view.findViewById<TextView>(R.id.weather_error)
        val content = view.findViewById<View>(R.id.weather_content)
        val city = view.findViewById<TextView>(R.id.weather_city)
        val current = view.findViewById<TextView>(R.id.weather_current)
        val details = view.findViewById<TextView>(R.id.weather_details)
        val forecasts = view.findViewById<LinearLayout>(R.id.weather_forecasts)
        val refresh = view.findViewById<Button>(R.id.weather_refresh)
        view.findViewById<TextView>(R.id.weather_source).movementMethod = LinkMovementMethod.getInstance()
        refresh.setOnClickListener {
            when {
                !viewModel.hasLocationPermission -> permission.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                )
                viewModel.uiState.value.errorMessage == R.string.weather_location_off ->
                    settings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                else -> viewModel.refresh()
            }
        }
        var rendered: WeatherSnapshot? = null

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    loading.isVisible = state.loadingMessage != null
                    state.loadingMessage?.let(loading::setText)
                    error.isVisible = state.errorMessage != null
                    state.errorMessage?.let(error::setText)
                    content.isVisible = state.snapshot != null
                    refresh.isEnabled = state.loadingMessage == null
                    refresh.setText(when {
                        state.errorMessage == R.string.weather_location_permission -> R.string.weather_enable_location
                        state.errorMessage == R.string.weather_location_off -> R.string.weather_open_settings
                        else -> R.string.weather_refresh
                    })
                    state.snapshot?.let { snapshot ->
                        city.text = state.place ?: getString(R.string.weather_current_location)
                        if (rendered === snapshot) return@let
                        rendered = snapshot
                        current.text = getString(
                            R.string.weather_current,
                            snapshot.temperature,
                            snapshot.condition
                        )
                        details.text = getString(
                            R.string.weather_details,
                            snapshot.feelsLike,
                            snapshot.humidity,
                            snapshot.wind,
                            snapshot.windSpeed,
                            snapshot.aqi
                        )
                        renderForecasts(forecasts, snapshot.forecasts)
                    }
                }
            }
        }
        viewModel.refresh()
    }

    private fun renderForecasts(container: LinearLayout, items: List<WeatherForecast>) {
        container.removeAllViews()
        items.forEach { item ->
            val card = layoutInflater.inflate(
                R.layout.item_weather_forecast,
                container,
                false
            )
            card.findViewById<TextView>(R.id.weather_forecast_text).text = getString(
                R.string.weather_forecast_item,
                item.date,
                item.day,
                item.dayTemperature,
                item.dayWind,
                item.night,
                item.nightTemperature,
                item.nightWind
            )
            container.addView(card)
        }
    }
}
