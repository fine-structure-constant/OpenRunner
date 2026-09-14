package cn.edu.pku.pkurunner;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

import cn.edu.pku.pkurunner.Model.Weather;
import cn.edu.pku.pkurunner.Network.Network;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Weather page. A plain pushed screen with a back arrow, same shape as
 * {@link cn.edu.pku.pkurunner.Settings.SettingsActivity} -- it no longer piggybacks on
 * {@code activity_main} / the navigation drawer.
 */
public class WeatherActivity extends AppCompatActivity {

    private static final String ICON_BASE = "https://image.nmc.cn/assets/img/w/40x40/4/";

    private LinearLayout content;

    @Nullable
    private TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        MaterialToolbar toolbar = findViewById(R.id.p_weather_toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        View root = getLayoutInflater().inflate(R.layout.view_weather, null, false);
        this.content = root.findViewById(R.id.v_weather_content);
        this.loadingText = root.findViewById(R.id.v_weather_txt_loading);
        ((FrameLayout) findViewById(R.id.f_weather_container)).addView(root, new FrameLayout.LayoutParams(-1, -1));

        render(Network.weather);
        if (Network.weather == null) {
            Network.getWeather().observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::render, error -> showError(error.getMessage()));
        }
    }

    private void render(Weather weather) {
        if (weather == null) {
            return;
        }
        content.removeAllViews();

        // ---- current conditions -------------------------------------------------------------
        View hero = getLayoutInflater().inflate(R.layout.view_weather_current, content, false);
        setText(hero, R.id.v_weather_txt_city, safe(weather.getCity(), "—"));
        setText(hero, R.id.v_weather_txt_temperature, safe(weather.getCurrentTemperature(), "--"));
        setText(hero, R.id.v_weather_txt_description, safe(weather.getCurrentDescription(), ""));
        loadIcon(hero.findViewById(R.id.v_weather_img_current), weather.getCurrentIconCode());
        content.addView(hero);

        // ---- live details -------------------------------------------------------------------
        View details = getLayoutInflater().inflate(R.layout.view_weather_details, content, false);
        setText(details, R.id.v_weather_txt_feels, safe(weather.getFeelsLike(), "--") + " ℃");
        setText(details, R.id.v_weather_txt_humidity, safe(weather.getHumidity(), "--") + "%");
        setText(details, R.id.v_weather_txt_wind,
                safe(weather.getWindDirection(), "--") + " · " + safe(weather.getWindPower(), "--"));
        setText(details, R.id.v_weather_txt_wind_speed, safe(weather.getWindSpeed(), "--"));
        setText(details, R.id.v_weather_txt_aqi, safe(weather.getAqiQuality(), "未知")
                + "（AQI " + safe(weather.getAqiValue(), "--") + "）");
        content.addView(details);

        // ---- 7 day forecast -----------------------------------------------------------------
        TextView section = new TextView(this);
        section.setText(R.string.or_weather_section_forecast);
        TextViewCompat.setTextAppearance(section, R.style.TextAppearance_OpenRunner_SectionTitle);
        section.setPadding(0, dp(22), 0, dp(2));
        content.addView(section);

        for (int i = 0; i < weather.getForecastCount(); i++) {
            View day = getLayoutInflater().inflate(R.layout.view_weather_day, content, false);
            setText(day, R.id.v_weather_txt_date, weather.getForecastDate(i));

            setText(day, R.id.v_weather_txt_day_desc, safe(weather.getForecastWeather(i), "未知"));
            setText(day, R.id.v_weather_txt_day_temp,
                    safe(weather.getForecastMaxTemperature(i), "--") + " ℃");
            setText(day, R.id.v_weather_txt_day_wind,
                    getString(R.string.or_weather_wind_format, safe(weather.getForecastWind(i), "--")));
            loadIcon(day.findViewById(R.id.v_weather_img_day), weather.getForecastDayIconCode(i));

            setText(day, R.id.v_weather_txt_night_desc, safe(weather.getForecastNightWeather(i), "未知"));
            setText(day, R.id.v_weather_txt_night_temp,
                    safe(weather.getForecastMinTemperature(i), "--") + " ℃");
            setText(day, R.id.v_weather_txt_night_wind,
                    getString(R.string.or_weather_wind_format, safe(weather.getForecastNightWind(i), "--")));
            loadIcon(day.findViewById(R.id.v_weather_img_night), weather.getForecastNightIconCode(i));

            content.addView(day);
        }
    }

    private void showError(String message) {
        content.removeAllViews();
        TextView error = new TextView(this);
        error.setText(getString(R.string.or_weather_error, safe(message, "未知错误")));
        TextViewCompat.setTextAppearance(error, R.style.TextAppearance_OpenRunner_ItemSubtitle);
        error.setPadding(0, dp(48), 0, dp(48));
        error.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        content.addView(error);
    }

    private void setText(View root, int id, CharSequence value) {
        TextView text = root.findViewById(id);
        if (text != null) {
            text.setText(value);
        }
    }

    private void loadIcon(View target, String iconCode) {
        if (!(target instanceof ImageView)) {
            return;
        }
        Glide.with(this)
                .load(ICON_BASE + safe(iconCode, "9999") + ".png")
                .error(R.drawable.ic_cloud_off_black_24dp)
                .into((ImageView) target);
    }

    private String safe(String value, String fallback) {
        return value == null || value.length() == 0 || "9999".equals(value) || "9999.0".equals(value)
                ? fallback : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
