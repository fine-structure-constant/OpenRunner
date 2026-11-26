package cn.edu.pku.openrunner.feature.weather.data

import cn.edu.pku.openrunner.core.orientation.wrapHeading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

data class WeatherSnapshot(
    val temperature: String,
    val feelsLike: String,
    val humidity: String,
    val condition: String,
    val wind: String,
    val windSpeed: String,
    val aqi: String,
    val forecasts: List<WeatherForecast>
)

data class WeatherForecast(
    val date: String,
    val day: String,
    val dayTemperature: String,
    val dayWind: String,
    val night: String,
    val nightTemperature: String,
    val nightWind: String
)

internal class WeatherRepository(private val api: OpenMeteoApi = WeatherClient.api) {
    suspend fun load(latitude: Double, longitude: Double): WeatherSnapshot = coroutineScope {
        // The weather service only needs a grid cell, so do not send the full device coordinates.
        val lat = (latitude * 100).roundToInt() / 100.0
        val lon = (longitude * 100).roundToInt() / 100.0
        val air = async {
            withTimeoutOrNull(4_000L) {
                try {
                    api.airQuality(lat, lon).current?.us_aqi
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
            }
        }
        val response = api.forecast(lat, lon)
        val current = requireNotNull(response.current)
        val hours = response.hourly
        val timeToHour = hours?.time.orEmpty().withIndex().associate { (index, time) -> time to index }
        val daily = response.daily
        val forecast = daily?.time.orEmpty().mapIndexed { index, date ->
            val dayHour = timeToHour["${date}T12:00"]
            val nightHour = timeToHour["${date}T21:00"]
            WeatherForecast(
                date,
                weatherDescription(dayHour?.let { hours?.weather_code?.getOrNull(it) }),
                number(daily?.temperature_2m_max?.getOrNull(index)),
                windDescription(dayHour?.let { hours?.wind_direction_10m?.getOrNull(it) }),
                weatherDescription(nightHour?.let { hours?.weather_code?.getOrNull(it) }),
                number(daily?.temperature_2m_min?.getOrNull(index)),
                windDescription(nightHour?.let { hours?.wind_direction_10m?.getOrNull(it) })
            )
        }
        WeatherSnapshot(
            temperature = number(current.temperature_2m),
            feelsLike = number(current.apparent_temperature),
            humidity = number(current.relative_humidity_2m),
            condition = weatherDescription(current.weather_code),
            wind = windDescription(current.wind_direction_10m),
            windSpeed = number(current.wind_speed_10m),
            aqi = air.await()?.toString() ?: "—",
            forecasts = forecast
        )
    }

    private fun number(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
    }
}

internal fun weatherDescription(code: Int?): String = when (code) {
    0 -> "晴"
    1, 2 -> "多云"
    3 -> "阴"
    45, 48 -> "雾"
    51, 53, 55 -> "毛毛雨"
    56, 57, 66, 67 -> "冻雨"
    61, 63, 65, 80, 81, 82 -> "雨"
    71, 73, 75, 77, 85, 86 -> "雪"
    95, 96, 99 -> "雷雨"
    else -> "未知"
}

private val windDirections = listOf("北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风")

internal fun windDescription(degrees: Int?): String =
    degrees?.let { windDirections[(wrapHeading(it.toFloat()) / 45f).roundToInt() % windDirections.size] } ?: "—"

private object WeatherClient {
    val api: OpenMeteoApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)
}
