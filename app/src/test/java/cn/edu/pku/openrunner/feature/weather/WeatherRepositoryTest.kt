package cn.edu.pku.openrunner.feature.weather

import cn.edu.pku.openrunner.feature.weather.data.AirQuality
import cn.edu.pku.openrunner.feature.weather.data.AirQualityResponse
import cn.edu.pku.openrunner.feature.weather.data.CurrentConditions
import cn.edu.pku.openrunner.feature.weather.data.DailyForecast
import cn.edu.pku.openrunner.feature.weather.data.ForecastResponse
import cn.edu.pku.openrunner.feature.weather.data.HourlyForecast
import cn.edu.pku.openrunner.feature.weather.data.OpenMeteoApi
import cn.edu.pku.openrunner.feature.weather.data.WeatherRepository
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherRepositoryTest {
    @Test
    fun usesTheSuppliedLocationAndLocalForecastTimes() = runBlocking {
        val api = FakeWeatherApi()
        val weather = WeatherRepository(api).load(31.23421, 121.47621)

        assertEquals(31.23 to 121.48, api.forecastLocation)
        assertEquals(api.forecastLocation, api.airLocation)
        assertEquals("晴", weather.condition)
        assertEquals("64", weather.aqi)
        val forecast = weather.forecasts.single()
        assertEquals("多云", forecast.day)
        assertEquals("19", forecast.dayTemperature)
        assertEquals("东风", forecast.dayWind)
        assertEquals("雨", forecast.night)
        assertEquals("10", forecast.nightTemperature)
        assertEquals("北风", forecast.nightWind)
    }

    @Test
    fun missingAirQualityDoesNotHideLocalWeather() = runBlocking {
        val weather = WeatherRepository(FakeWeatherApi(airUnavailable = true)).load(39.9, 116.4)

        assertEquals("—", weather.aqi)
        assertEquals("18", weather.temperature)
    }

    private class FakeWeatherApi(private val airUnavailable: Boolean = false) : OpenMeteoApi {
        var forecastLocation: Pair<Double, Double>? = null
        var airLocation: Pair<Double, Double>? = null

        override suspend fun forecast(latitude: Double, longitude: Double): ForecastResponse {
            forecastLocation = latitude to longitude
            return ForecastResponse(
                CurrentConditions(18.0, 17.0, 56.0, 0, 90, 2.5),
                DailyForecast(listOf("2026-11-26"), listOf(19.0), listOf(10.0)),
                HourlyForecast(listOf("2026-11-26T12:00", "2026-11-26T21:00"), listOf(2, 63), listOf(90, 358))
            )
        }

        override suspend fun airQuality(latitude: Double, longitude: Double): AirQualityResponse {
            airLocation = latitude to longitude
            if (airUnavailable) throw IOException("Air quality service unavailable")
            return AirQualityResponse(AirQuality(64))
        }
    }
}
