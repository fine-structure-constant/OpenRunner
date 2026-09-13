package cn.edu.pku.pkurunner.Model;

import android.content.Context;
import cn.edu.pku.pkurunner.R;
import java.util.ArrayList;
import java.io.Serializable;
import java.util.List;

public class Weather implements Serializable {
    private AqiBean aqi;
    private BasicBean basic;
    private List<DailyForecastBean> daily_forecast;
    private List<HourlyForecastBean> hourly_forecast;
    private NowBean now;
    private String status;
    private SuggestionBean suggestion;

    public static Weather fromNmc(NmcWeatherResponse response) {
        if (response == null || response.code != 0 || response.data == null || response.data.real == null) {
            throw new IllegalArgumentException("Invalid NMC weather response");
        }
        Weather result = new Weather();
        NmcWeatherResponse.Real real = response.data.real;
        NmcWeatherResponse.CurrentWeather current = real.weather;
        NmcWeatherResponse.CurrentWind wind = real.wind;

        result.status = "ok";
        result.basic = new BasicBean();
        result.basic.city = real.station == null ? "北京" : real.station.city;
        result.basic.cnty = real.station == null ? "中国" : real.station.province;
        result.basic.id = real.station == null ? null : real.station.code;
        result.basic.update = new BasicBean.UpdateBean();
        result.basic.update.loc = real.publish_time;

        result.now = new NowBean();
        result.now.cond = new NowBean.CondBean();
        result.now.cond.code = nmcIconCode(current == null ? null : current.img);
        result.now.cond.txt = current == null ? "未知" : current.info;
        result.now.tmp = current == null ? null : formatNumber(current.temperature);
        result.now.fl = current == null ? null : formatNumber(current.feelst);
        result.now.hum = current == null ? null : formatNumber(current.humidity);
        result.now.pcpn = current == null ? null : formatNumber(current.rain);
        result.now.pres = current == null ? null : formatNumber(current.airpressure);
        result.now.wind = new NowBean.WindBean();
        result.now.wind.dir = wind == null ? "" : wind.direct;
        result.now.wind.deg = wind == null ? null : formatNumber(wind.degree);
        result.now.wind.sc = wind == null ? null : wind.power;
        result.now.wind.spd = wind == null ? null : formatNumber(wind.speed);

        result.aqi = new AqiBean();
        result.aqi.city = new AqiBean.CityBean();
        result.aqi.city.aqi = response.data.air == null ? "0" : String.valueOf(response.data.air.aqi);
        result.aqi.city.qlty = response.data.air == null ? "未知" : response.data.air.text;

        result.suggestion = new SuggestionBean();
        result.suggestion.sport = new SuggestionBean.SportBean();
        result.suggestion.sport.brf = "参考";
        result.suggestion.sport.txt = "天气数据来自中国气象局，可根据实时天气安排运动。";

        if (response.data.predict != null && response.data.predict.detail != null) {
            result.daily_forecast = new ArrayList<>();
            for (NmcWeatherResponse.Forecast item : response.data.predict.detail) {
                DailyForecastBean forecast = new DailyForecastBean();
                forecast.date = item.date;
                forecast.cond = new DailyForecastBean.CondBean();
                if (item.day != null && item.day.weather != null) {
                    forecast.cond.code_d = nmcIconCode(item.day.weather.img);
                    forecast.cond.txt_d = item.day.weather.info;
                    forecast.tmp = new DailyForecastBean.TmpBean();
                    forecast.tmp.max = item.day.weather.temperature;
                }
                if (item.night != null && item.night.weather != null) {
                    if (forecast.cond == null) forecast.cond = new DailyForecastBean.CondBean();
                    forecast.cond.code_n = nmcIconCode(item.night.weather.img);
                    forecast.cond.txt_n = item.night.weather.info;
                    if (forecast.tmp == null) forecast.tmp = new DailyForecastBean.TmpBean();
                    forecast.tmp.min = item.night.weather.temperature;
                }
                if (item.day != null && item.day.wind != null) {
                    forecast.wind = new DailyForecastBean.WindBean();
                    forecast.wind.dir = item.day.wind.direct;
                    forecast.wind.sc = item.day.wind.power;
                }
                if (item.night != null && item.night.wind != null) {
                    forecast.nightWind = new DailyForecastBean.WindBean();
                    forecast.nightWind.dir = item.night.wind.direct;
                    forecast.nightWind.sc = item.night.wind.power;
                }
                result.daily_forecast.add(forecast);
            }
        }
        return result;
    }

    private static String formatNumber(double value) {
        return value == 9999.0 ? null : String.valueOf(value);
    }

    private static String nmcIconCode(String code) {
        if (code == null) return "999";
        return code;
    }

    public static class AqiBean implements Serializable {
        private CityBean city;

        public static class CityBean implements Serializable {
            private String aqi;
            private String co;
            private String no2;
            private String o3;
            private String pm10;
            private String pm25;
            private String qlty;
            private String so2;
        }
    }

    public static class BasicBean implements Serializable {
        private String city;
        private String cnty;

        private String id;
        private String lat;
        private String lon;
        private UpdateBean update;

        public static class UpdateBean implements Serializable {
            private String loc;
            private String utc;
        }
    }

    public static class DailyForecastBean implements Serializable {
        private AstroBean astro;
        private CondBean cond;
        private String date;
        private String hum;
        private String pcpn;
        private String pop;
        private String pres;
        private TmpBean tmp;
        private String vis;
        private WindBean wind;
        private WindBean nightWind;

        public static class AstroBean implements Serializable {
            private String sr;
            private String ss;
        }

        public static class CondBean implements Serializable {
            private String code_d;
            private String code_n;
            private String txt_d;
            private String txt_n;
        }

        public static class TmpBean implements Serializable {
            private String max;
            private String min;
        }

        public static class WindBean implements Serializable {
            private String deg;
            private String dir;
            private String sc;
            private String spd;
        }

        public String getDate() {
            return this.date;
        }

        public void setDate(String str) {
            this.date = str;
        }
    }

    public static class HourlyForecastBean implements Serializable {
        private String date;
        private String hum;
        private String pop;
        private String pres;
        private String tmp;
        private WindBean wind;

        public static class WindBean implements Serializable {
            private String deg;
            private String dir;
            private String sc;
            private String spd;
        }

        public String getDate() {
            return this.date;
        }

        public void setDate(String str) {
            this.date = str;
        }
    }

    public static class NowBean implements Serializable {
        private CondBean cond;
        private String fl;
        private String hum;
        private String pcpn;
        private String pres;
        private String tmp;
        private String vis;
        private WindBean wind;

        public static class CondBean implements Serializable {
            private String code;
            private String txt;

            public String getCode() {
                return this.code;
            }
        }

        public static class WindBean implements Serializable {
            private String deg;
            private String dir;
            private String sc;
            private String spd;
        }

        public CondBean getCond() {
            return this.cond;
        }
    }

    public static class SuggestionBean implements Serializable {
        private ComfBean comf;
        private CwBean cw;
        private DrsgBean drsg;
        private FluBean flu;
        private SportBean sport;
        private TravBean trav;
        private UvBean uv;

        public static class ComfBean implements Serializable {
            private String brf;
            private String txt;
        }

        public static class CwBean implements Serializable {
            private String brf;
            private String txt;
        }

        public static class DrsgBean implements Serializable {
            private String brf;
            private String txt;
        }

        public static class FluBean implements Serializable {
            private String brf;
            private String txt;
        }

        public static class SportBean implements Serializable {
            private String brf;
            private String txt;
        }

        public static class TravBean implements Serializable {
            private String brf;
            private String txt;
        }

        public static class UvBean implements Serializable {
            private String brf;
            private String txt;
        }
    }

    public AqiBean getAqi() {
        return this.aqi;
    }

    public String getCity() {
        return this.basic == null ? "" : this.basic.city;
    }

    public String getCurrentIconCode() {
        return this.now == null || this.now.cond == null ? "999" : this.now.cond.code;
    }

    public String getCurrentDescription() {
        return this.now == null || this.now.cond == null ? "未知" : this.now.cond.txt;
    }

    public String getCurrentTemperature() {
        return this.now == null ? null : this.now.tmp;
    }

    public String getFeelsLike() {
        return this.now == null ? null : this.now.fl;
    }

    public String getHumidity() {
        return this.now == null ? null : this.now.hum;
    }

    public String getWindDirection() {
        return this.now == null || this.now.wind == null ? "" : this.now.wind.dir;
    }

    public String getWindSpeed() {
        return this.now == null || this.now.wind == null ? "" : this.now.wind.spd;
    }

    public String getWindPower() {
        return this.now == null || this.now.wind == null ? "" : this.now.wind.sc;
    }

    public String getAqiValue() {
        return this.aqi == null || this.aqi.city == null ? "" : this.aqi.city.aqi;
    }

    public String getAqiQuality() {
        return this.aqi == null || this.aqi.city == null ? "" : this.aqi.city.qlty;
    }

    public int getForecastCount() {
        return this.daily_forecast == null ? 0 : this.daily_forecast.size();
    }

    public String getForecastDate(int index) {
        return this.daily_forecast.get(index).date;
    }

    public String getForecastWeather(int index) {
        DailyForecastBean.CondBean cond = this.daily_forecast.get(index).cond;
        return cond == null ? "未知" : cond.txt_d;
    }

    public String getForecastDayIconCode(int index) {
        DailyForecastBean.CondBean cond = this.daily_forecast.get(index).cond;
        return cond == null ? "9999" : cond.code_d;
    }

    public String getForecastNightIconCode(int index) {
        DailyForecastBean.CondBean cond = this.daily_forecast.get(index).cond;
        return cond == null ? "9999" : cond.code_n;
    }

    public String getForecastNightWeather(int index) {
        DailyForecastBean.CondBean cond = this.daily_forecast.get(index).cond;
        return cond == null ? "未知" : cond.txt_n;
    }

    public String getForecastMinTemperature(int index) {
        return this.daily_forecast.get(index).tmp == null ? "" : this.daily_forecast.get(index).tmp.min;
    }

    public String getForecastMaxTemperature(int index) {
        return this.daily_forecast.get(index).tmp == null ? "" : this.daily_forecast.get(index).tmp.max;
    }

    public String getForecastWind(int index) {
        return this.daily_forecast.get(index).wind == null ? "" : this.daily_forecast.get(index).wind.dir;
    }

    public String getForecastNightWind(int index) {
        return this.daily_forecast.get(index).nightWind == null ? "" : this.daily_forecast.get(index).nightWind.dir;
    }

    public NowBean getNow() {
        return this.now;
    }

    public String getDescription(Context context) {
        return Integer.valueOf(this.aqi.city.aqi).intValue() > 100 ? context.getString(R.string.a_main_weather_abnormal_format, this.basic.city, this.now.cond.txt, this.now.wind.dir, this.aqi.city.qlty) : context.getString(R.string.a_main_weather_format, this.basic.city, this.now.cond.txt, this.now.wind.dir, this.aqi.city.qlty, this.suggestion.sport.brf, this.suggestion.sport.txt);
    }
}
