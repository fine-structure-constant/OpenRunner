package cn.edu.pku.pkurunner.Model;

import android.content.Context;
import cn.edu.pku.pkurunner.R;
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

    public NowBean getNow() {
        return this.now;
    }

    public String getDescription(Context context) {
        return Integer.valueOf(this.aqi.city.aqi).intValue() > 100 ? context.getString(R.string.a_main_weather_abnormal_format, this.basic.city, this.now.cond.txt, this.now.wind.dir, this.aqi.city.qlty) : context.getString(R.string.a_main_weather_format, this.basic.city, this.now.cond.txt, this.now.wind.dir, this.aqi.city.qlty, this.suggestion.sport.brf, this.suggestion.sport.txt);
    }
}
