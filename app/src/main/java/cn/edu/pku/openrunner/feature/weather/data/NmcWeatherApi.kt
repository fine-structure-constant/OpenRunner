package cn.edu.pku.openrunner.feature.weather.data

import retrofit2.http.GET
import retrofit2.http.Query

interface NmcWeatherApi {
    @GET("rest/weather")
    suspend fun getWeather(@Query("stationid") stationId: String): NmcWeatherResponse
}

data class NmcWeatherResponse(
    val code: Int = -1,
    val msg: String? = null,
    val data: NmcWeatherData? = null
)

data class NmcWeatherData(
    val real: NmcReal? = null,
    val predict: NmcPredict? = null,
    val air: NmcAir? = null
)

data class NmcReal(
    val station: NmcStation? = null,
    val publish_time: String? = null,
    val weather: NmcCurrentWeather? = null,
    val wind: NmcWind? = null
)

data class NmcPredict(val detail: List<NmcForecast> = emptyList())

data class NmcStation(val city: String? = null)

data class NmcCurrentWeather(
    val temperature: Double = 9999.0,
    val feelst: Double = 9999.0,
    val humidity: Double = 9999.0,
    val info: String? = null
)

data class NmcWind(
    val direct: String? = null,
    val power: String? = null,
    val speed: Double = 9999.0
)

data class NmcAir(val aqi: Int = 0, val text: String? = null)

data class NmcForecast(
    val date: String? = null,
    val day: NmcForecastPart? = null,
    val night: NmcForecastPart? = null
)

data class NmcForecastPart(
    val weather: NmcForecastWeather? = null,
    val wind: NmcWind? = null
)

data class NmcForecastWeather(
    val info: String? = null,
    val temperature: String? = null
)
