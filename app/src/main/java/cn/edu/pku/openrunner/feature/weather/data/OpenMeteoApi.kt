package cn.edu.pku.openrunner.feature.weather.data

import retrofit2.http.GET
import retrofit2.http.Query

internal interface OpenMeteoApi {
    @GET(
        "v1/forecast?current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code," +
            "wind_direction_10m,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min" +
            "&hourly=weather_code,wind_direction_10m&timezone=auto&wind_speed_unit=ms&forecast_days=7"
    )
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): ForecastResponse

    @GET("https://air-quality-api.open-meteo.com/v1/air-quality?current=us_aqi")
    suspend fun airQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): AirQualityResponse
}

internal data class ForecastResponse(
    val current: CurrentConditions?,
    val daily: DailyForecast?,
    val hourly: HourlyForecast?
)

internal data class CurrentConditions(
    val temperature_2m: Double?,
    val apparent_temperature: Double?,
    val relative_humidity_2m: Double?,
    val weather_code: Int?,
    val wind_direction_10m: Int?,
    val wind_speed_10m: Double?
)

internal data class DailyForecast(
    val time: List<String>?,
    val temperature_2m_max: List<Double?>?,
    val temperature_2m_min: List<Double?>?
)

internal data class HourlyForecast(
    val time: List<String>?,
    val weather_code: List<Int?>?,
    val wind_direction_10m: List<Int?>?
)

internal data class AirQualityResponse(val current: AirQuality?)
internal data class AirQuality(val us_aqi: Int?)
