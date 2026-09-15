package cn.edu.pku.openrunner.feature.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.weather.data.WeatherForecast
import cn.edu.pku.openrunner.feature.weather.data.WeatherRepository
import kotlinx.coroutines.launch

class WeatherFragment : Fragment() {
    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModel.Factory(WeatherRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_weather, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val loading = view.findViewById<View>(R.id.weather_loading)
        val error = view.findViewById<TextView>(R.id.weather_error)
        val content = view.findViewById<View>(R.id.weather_content)
        val city = view.findViewById<TextView>(R.id.weather_city)
        val current = view.findViewById<TextView>(R.id.weather_current)
        val details = view.findViewById<TextView>(R.id.weather_details)
        val forecasts = view.findViewById<LinearLayout>(R.id.weather_forecasts)
        view.findViewById<View>(R.id.weather_refresh).setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    loading.visibility = if (state.loading) View.VISIBLE else View.GONE
                    error.visibility = if (state.errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE
                    error.text = state.errorMessage.orEmpty()
                    content.visibility = if (state.snapshot == null) View.GONE else View.VISIBLE
                    state.snapshot?.let { snapshot ->
                        city.text = snapshot.city
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
