package cn.edu.pku.pkurunner;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import cn.edu.pku.pkurunner.Model.Weather;
import cn.edu.pku.pkurunner.Network.Network;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class WeatherActivity extends AppCompatActivity {
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("天气");
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.addView(content);
        setContentView(scroll);
        render(Network.weather);
        if (Network.weather == null) {
            addText("正在加载天气…", 18, Color.DKGRAY);
            Network.getWeather().observeOn(AndroidSchedulers.mainThread()).subscribe(
                    this::render,
                    error -> showError(error.getMessage()));
        }
    }

    private void render(Weather weather) {
        if (weather == null) return;
        content.removeAllViews();

        TextView title = addText(weather.getCity(), 26, Color.BLACK);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView update = addText("国家气象中心 · 实时天气", 13, Color.GRAY);
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
        temperature.setTextColor(Color.BLACK);
        summary.addView(temperature);
        TextView description = new TextView(this);
        description.setText(weather.getCurrentDescription());
        description.setTextSize(18);
        summary.addView(description);
        current.addView(summary);
        content.addView(current);
        String iconUrl = "https://image.nmc.cn/assets/img/w/40x40/4/" + weather.getCurrentIconCode() + ".png";
        Glide.with(this).load(iconUrl).error(R.drawable.ic_cloud_off_black_24dp).into(icon);

        addText("体感温度：" + safe(weather.getFeelsLike(), "--") + " ℃    湿度：" + safe(weather.getHumidity(), "--") + "%", 16, Color.DKGRAY);
        addText("风向：" + safe(weather.getWindDirection(), "--") + "    风力：" + safe(weather.getWindPower(), "--") + "    风速：" + safe(weather.getWindSpeed(), "--"), 16, Color.DKGRAY);
        addText("空气质量：" + safe(weather.getAqiQuality(), "未知") + "（AQI " + safe(weather.getAqiValue(), "--") + "）", 16, Color.DKGRAY);

        addText("7 天预报", 22, Color.BLACK).setPadding(0, dp(24), 0, dp(8));
        for (int i = 0; i < weather.getForecastCount(); i++) {
            TextView date = addText(weather.getForecastDate(i), 17, Color.BLACK);
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
        TextView title = textView(label, 15, Color.GRAY);
        part.addView(title);
        ImageView icon = new ImageView(this);
        part.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        String iconUrl = "https://image.nmc.cn/assets/img/w/40x40/4/" + safe(iconCode, "9999") + ".png";
        Glide.with(this).load(iconUrl).error(R.drawable.ic_cloud_off_black_24dp).into(icon);
        part.addView(textView(safe(description, "未知"), 15, Color.DKGRAY));
        part.addView(textView(safe(temperature, "--") + " ℃", 16, Color.BLACK));
        part.addView(textView("风：" + safe(wind, "--"), 14, Color.GRAY));
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
        addText("天气加载失败：" + safe(message, "未知错误"), 17, Color.DKGRAY);
    }

    private String safe(String value, String fallback) {
        return value == null || value.length() == 0 || "9999".equals(value) || "9999.0".equals(value) ? fallback : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
