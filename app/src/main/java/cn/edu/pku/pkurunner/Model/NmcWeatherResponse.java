package cn.edu.pku.pkurunner.Model;

import java.util.List;

/** JSON response from the National Meteorological Center station endpoint. */
public class NmcWeatherResponse {
    public int code;
    public String msg;
    public Data data;

    public static class Data {
        public Real real;
        public Predict predict;
        public Air air;
    }

    public static class Real {
        public Station station;
        public String publish_time;
        public CurrentWeather weather;
        public CurrentWind wind;
    }

    public static class Predict {
        public Station station;
        public String publish_time;
        public List<Forecast> detail;
    }

    public static class Station {
        public String code;
        public String province;
        public String city;
        public String url;
    }

    public static class CurrentWeather {
        public double temperature;
        public double temperatureDiff;
        public double airpressure;
        public double humidity;
        public double rain;
        public String info;
        public String img;
        public double feelst;
    }

    public static class CurrentWind {
        public String direct;
        public double degree;
        public String power;
        public double speed;
    }

    public static class Air {
        public int aqi;
        public int aq;
        public String text;
    }

    public static class Forecast {
        public String date;
        public String pt;
        public Part day;
        public Part night;
        public double precipitation;
    }

    public static class Part {
        public ForecastWeather weather;
        public CurrentWind wind;
    }

    public static class ForecastWeather {
        public String info;
        public String img;
        public String temperature;
    }
}
