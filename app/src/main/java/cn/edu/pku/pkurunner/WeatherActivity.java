package cn.edu.pku.pkurunner;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.bumptech.glide.Glide;
import cn.edu.pku.pkurunner.Model.Weather;
import cn.edu.pku.pkurunner.Network.Network;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class WeatherActivity extends AppCompatActivity {
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DrawerLayout drawer = findViewById(R.id.a_main_drawer_layout);
        Toolbar toolbar = findViewById(R.id.v_main_toolbar);
        toolbar.setTitle("天气");
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigation = findViewById(R.id.a_main_nav);
        navigation.getMenu().findItem(R.id.nav_weather).setChecked(true);
        navigation.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_weather) {
                drawer.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_settings) {
                startActivity(new android.content.Intent(this, cn.edu.pku.pkurunner.Settings.SettingsActivity.class));
                drawer.closeDrawer(GravityCompat.START);
            } else {
                finish();
            }
            return true;
        });
        findViewById(R.id.v_status_progress).setVisibility(View.GONE);
        findViewById(R.id.v_main_fab_switch).setVisibility(View.GONE);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.addView(content);
        ((FrameLayout) findViewById(R.id.v_main_frame)).addView(scroll,
                new FrameLayout.LayoutParams(-1, -1));
        render(Network.weather);
        if (Network.weather == null) {
            addText("正在加载天气…", 18, themedColor(android.R.attr.textColorSecondary, 0xff666666));
            Network.getWeather().observeOn(AndroidSchedulers.mainThread()).subscribe(
                    this::render,
                    error -> showError(error.getMessage()));
        }
    }

    private void render(Weather weather) {
        if (weather == null) return;
        content.removeAllViews();

        TextView title = addText(weather.getCity(), 26, themedColor(android.R.attr.textColorPrimary, 0xff000000));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView update = addText("国家气象中心 · 实时天气", 13, themedColor(android.R.attr.textColorSecondary, 0xff666666));
        update.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout current = new LinearLayout(this);
        current.setGravity(Gravity.CENTER_VERTICAL);
        current.setPadding(0, dp(16), 0, dp(12));
        ImageView icon = new ImageView(this);
        current.addView(icon, new LinearLayout.LayoutParams(dp(72), dp(72)));
        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        TextView temperature = new TextView(this);
        temperature.setText(safe(weather.getCurrentTemperature(), "--") + " ℃");
        temperature.setTextSize(30);
        temperature.setTextColor(themedColor(android.R.attr.textColorPrimary, 0xff000000));
        summary.addView(temperature);
        TextView description = new TextView(this);
        description.setText(weather.getCurrentDescription());
        description.setTextSize(18);
        summary.addView(description);
        current.addView(summary);
        content.addView(current);
        String iconUrl = "https://image.nmc.cn/assets/img/w/40x40/4/" + weather.getCurrentIconCode() + ".png";
        Glide.with(this).load(iconUrl).error(R.drawable.ic_cloud_off_black_24dp).into(icon);

        addText("体感温度：" + safe(weather.getFeelsLike(), "--") + " ℃    湿度：" + safe(weather.getHumidity(), "--") + "%", 16, themedColor(android.R.attr.textColorSecondary, 0xff666666));
        addText("风向：" + safe(weather.getWindDirection(), "--") + "    风力：" + safe(weather.getWindPower(), "--") + "    风速：" + safe(weather.getWindSpeed(), "--"), 16, themedColor(android.R.attr.textColorSecondary, 0xff666666));
        addText("空气质量：" + safe(weather.getAqiQuality(), "未知") + "（AQI " + safe(weather.getAqiValue(), "--") + "）", 16, themedColor(android.R.attr.textColorSecondary, 0xff666666));

        addText("7 天预报", 22, themedColor(android.R.attr.textColorPrimary, 0xff000000)).setPadding(0, dp(24), 0, dp(8));
        for (int i = 0; i < weather.getForecastCount(); i++) {
            TextView date = addText(weather.getForecastDate(i), 17, themedColor(android.R.attr.textColorPrimary, 0xff000000));
            date.setPadding(0, dp(10), 0, dp(4));
            LinearLayout parts = new LinearLayout(this);
            parts.setWeightSum(2);
            parts.addView(forecastPart("白天", weather.getForecastDayIconCode(i),
                    weather.getForecastWeather(i), weather.getForecastMaxTemperature(i), weather.getForecastWind(i)),
                    new LinearLayout.LayoutParams(0, -2, 1));
            parts.addView(forecastPart("夜间", weather.getForecastNightIconCode(i),
                    weather.getForecastNightWeather(i), weather.getForecastMinTemperature(i), weather.getForecastNightWind(i)),
                    new LinearLayout.LayoutParams(0, -2, 1));
            content.addView(parts);
        }
    }

    private LinearLayout forecastPart(String label, String iconCode, String description, String temperature, String wind) {
        LinearLayout part = new LinearLayout(this);
        part.setOrientation(LinearLayout.VERTICAL);
        part.setPadding(0, dp(2), dp(8), dp(8));
        TextView title = textView(label, 15, themedColor(android.R.attr.textColorSecondary, 0xff666666));
        part.addView(title);
        ImageView icon = new ImageView(this);
        part.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        String iconUrl = "https://image.nmc.cn/assets/img/w/40x40/4/" + safe(iconCode, "9999") + ".png";
        Glide.with(this).load(iconUrl).error(R.drawable.ic_cloud_off_black_24dp).into(icon);
        part.addView(textView(safe(description, "未知"), 15, themedColor(android.R.attr.textColorSecondary, 0xff666666)));
        part.addView(textView(safe(temperature, "--") + " ℃", 16, themedColor(android.R.attr.textColorPrimary, 0xff000000)));
        part.addView(textView("风：" + safe(wind, "--"), 14, themedColor(android.R.attr.textColorSecondary, 0xff666666)));
        return part;
    }

    private TextView addText(String value, float size, int color) {
        TextView text = textView(value, size, color);
        content.addView(text, new LinearLayout.LayoutParams(-1, -2));
        return text;
    }

    private TextView textView(String value, float size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        return text;
    }

    private void showError(String message) {
        content.removeAllViews();
        addText("天气加载失败：" + safe(message, "未知错误"), 17, themedColor(android.R.attr.textColorSecondary, 0xff666666));
    }

    private String safe(String value, String fallback) {
        return value == null || value.length() == 0 || "9999".equals(value) || "9999.0".equals(value) ? fallback : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @ColorInt
    private int themedColor(int attribute, int fallback) {
        TypedValue value = new TypedValue();
        if (getTheme().resolveAttribute(attribute, value, true)) {
            if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) {
                return value.data;
            }
            if (value.resourceId != 0) {
                ColorStateList colors = ContextCompat.getColorStateList(this, value.resourceId);
                if (colors != null) return colors.getDefaultColor();
            }
        }
        return fallback;
    }

}
