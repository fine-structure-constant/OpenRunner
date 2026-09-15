package cn.edu.pku.openrunner.feature.weather.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class WeatherSnapshot(
    val city: String,
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

class WeatherRepository(
    private val api: NmcWeatherApi = WeatherClient.api
) {
    suspend fun load(): WeatherSnapshot {
        val response = api.getWeather(STATION_ID)
        val real = response.data?.real ?: error(response.msg ?: "天气服务返回为空")
        val current = real.weather
        val wind = real.wind
        return WeatherSnapshot(
            city = real.station?.city ?: "北京",
            temperature = number(current?.temperature),
            feelsLike = number(current?.feelst),
            humidity = number(current?.humidity),
            condition = current?.info ?: "未知",
            wind = wind?.direct ?: "—",
            windSpeed = number(wind?.speed),
            aqi = response.data.air?.let { "${it.text ?: "未知"}（${it.aqi}）" } ?: "未知",
            forecasts = response.data.predict?.detail.orEmpty().map { forecast ->
                WeatherForecast(
                    date = forecast.date ?: "—",
                    day = forecast.day?.weather?.info ?: "未知",
                    dayTemperature = forecast.day?.weather?.temperature ?: "—",
                    dayWind = forecast.day?.wind?.direct ?: "—",
                    night = forecast.night?.weather?.info ?: "未知",
                    nightTemperature = forecast.night?.weather?.temperature ?: "—",
                    nightWind = forecast.night?.wind?.direct ?: "—"
                )
            }
        )
    }

    private fun number(value: Double?): String {
        if (value == null || value == 9999.0) return "—"
        return if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
    }

    companion object {
        private const val STATION_ID = "fElIR"
    }
}

private object WeatherClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://nmc.cn/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: NmcWeatherApi = retrofit.create(NmcWeatherApi::class.java)
}
