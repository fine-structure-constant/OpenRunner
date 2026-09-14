package cn.edu.pku.pkurunner.Model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.gson.Gson;
import org.junit.Test;

public class WeatherTest {

    private static final String RESPONSE = "{"
            + "\"code\":0,\"msg\":\"success\",\"data\":{"
            + "\"real\":{"
            + "\"station\":{\"code\":\"54511\",\"province\":\"北京市\",\"city\":\"北京\",\"url\":\"\"},"
            + "\"publish_time\":\"2026-09-13 19:00\","
            + "\"weather\":{\"temperature\":25.5,\"temperatureDiff\":1.0,\"airpressure\":1002.0,"
            + "\"humidity\":60.0,\"rain\":0.0,\"info\":\"晴\",\"img\":\"0\",\"feelst\":26.0},"
            + "\"wind\":{\"direct\":\"东南风\",\"degree\":135.0,\"power\":\"3级\",\"speed\":3.2}"
            + "},"
            + "\"predict\":{\"publish_time\":\"2026-09-13 08:00\",\"detail\":[{"
            + "\"date\":\"2026-09-13\",\"pt\":\"day\",\"precipitation\":0.0,"
            + "\"day\":{\"weather\":{\"info\":\"晴\",\"img\":\"0\",\"temperature\":\"30\"},"
            + "\"wind\":{\"direct\":\"南风\",\"power\":\"2级\",\"degree\":180.0,\"speed\":2.0}},"
            + "\"night\":{\"weather\":{\"info\":\"多云\",\"img\":\"1\",\"temperature\":\"18\"},"
            + "\"wind\":{\"direct\":\"北风\",\"power\":\"1级\",\"degree\":0.0,\"speed\":1.0}}"
            + "}]},"
            + "\"air\":{\"aqi\":42,\"aq\":1,\"text\":\"优\"}"
            + "}}";

    private final Gson gson = new Gson();

    private NmcWeatherResponse response() {
        return gson.fromJson(RESPONSE, NmcWeatherResponse.class);
    }

    @Test
    public void fromNmc_mapsCurrentConditions() {
        Weather weather = Weather.fromNmc(response());
        assertThat(weather.getCity()).isEqualTo("北京");
        assertThat(weather.getCurrentDescription()).isEqualTo("晴");
        assertThat(weather.getCurrentIconCode()).isEqualTo("0");
        assertThat(weather.getCurrentTemperature()).isEqualTo("25.5");
        assertThat(weather.getFeelsLike()).isEqualTo("26.0");
        assertThat(weather.getHumidity()).isEqualTo("60.0");
    }

    @Test
    public void fromNmc_mapsWind() {
        Weather weather = Weather.fromNmc(response());
        assertThat(weather.getWindDirection()).isEqualTo("东南风");
        assertThat(weather.getWindPower()).isEqualTo("3级");
        assertThat(weather.getWindSpeed()).isEqualTo("3.2");
    }

    @Test
    public void fromNmc_mapsAirQuality() {
        Weather weather = Weather.fromNmc(response());
        assertThat(weather.getAqiValue()).isEqualTo("42");
        assertThat(weather.getAqiQuality()).isEqualTo("优");
    }

    @Test
    public void fromNmc_mapsDailyForecast() {
        Weather weather = Weather.fromNmc(response());
        assertThat(weather.getForecastCount()).isEqualTo(1);
        assertThat(weather.getForecastDate(0)).isEqualTo("2026-09-13");
        assertThat(weather.getForecastWeather(0)).isEqualTo("晴");
        assertThat(weather.getForecastNightWeather(0)).isEqualTo("多云");
        assertThat(weather.getForecastDayIconCode(0)).isEqualTo("0");
        assertThat(weather.getForecastNightIconCode(0)).isEqualTo("1");
        assertThat(weather.getForecastMaxTemperature(0)).isEqualTo("30");
        assertThat(weather.getForecastMinTemperature(0)).isEqualTo("18");
        assertThat(weather.getForecastWind(0)).isEqualTo("南风");
        assertThat(weather.getForecastNightWind(0)).isEqualTo("北风");
    }

    @Test
    public void fromNmc_sentinelValue9999BecomesNull() {
        NmcWeatherResponse response = response();
        response.data.real.weather.temperature = 9999.0d;
        assertThat(Weather.fromNmc(response).getCurrentTemperature()).isNull();
    }

    @Test
    public void fromNmc_withoutWindSection_fallsBackToEmpty() {
        NmcWeatherResponse response = response();
        response.data.real.wind = null;
        Weather weather = Weather.fromNmc(response);
        assertThat(weather.getWindDirection()).isEmpty();
        assertThat(weather.getWindPower()).isNull();
        assertThat(weather.getWindSpeed()).isNull();
    }

    @Test
    public void fromNmc_withoutAirSection_fallsBackToZeroAndUnknown() {
        NmcWeatherResponse response = response();
        response.data.air = null;
        Weather weather = Weather.fromNmc(response);
        assertThat(weather.getAqiValue()).isEqualTo("0");
        assertThat(weather.getAqiQuality()).isEqualTo("未知");
    }

    @Test
    public void fromNmc_withoutStationSection_fallsBackToBeijing() {
        NmcWeatherResponse response = response();
        response.data.real.station = null;
        assertThat(Weather.fromNmc(response).getCity()).isEqualTo("北京");
    }

    @Test
    public void fromNmc_withoutForecast_leavesForecastEmpty() {
        NmcWeatherResponse response = response();
        response.data.predict = null;
        assertThat(Weather.fromNmc(response).getForecastCount()).isEqualTo(0);
    }

    @Test
    public void fromNmc_rejectsNullResponse() {
        assertRejected(null);
    }

    @Test
    public void fromNmc_rejectsNonZeroCode() {
        NmcWeatherResponse response = response();
        response.code = 1;
        assertRejected(response);
    }

    @Test
    public void fromNmc_rejectsMissingRealSection() {
        NmcWeatherResponse response = response();
        response.data.real = null;
        assertRejected(response);
    }

    private void assertRejected(NmcWeatherResponse response) {
        try {
            Weather.fromNmc(response);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessageThat().contains("Invalid NMC weather response");
        }
    }
}
