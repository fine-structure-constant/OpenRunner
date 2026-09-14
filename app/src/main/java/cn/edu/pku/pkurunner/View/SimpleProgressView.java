package cn.edu.pku.pkurunner.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cn.edu.pku.pkurunner.R;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.ViewTarget;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.Date;

public class SimpleProgressView extends MaterialCardView implements ProgressableView {

    private View rootView;

    private ImageView weatherImageView;

    private ImageView sleepingImageView;

    private LinearProgressIndicator distanceProgressBar;

    private View progressGroup;

    private View dayGroup;

    private View sleepingGroup;

    private TextView distanceText;

    private TextView unitText;

    private TextView captionText;

    private TextView percentText;

    private TextView dayText;

    private TextView sleepingNoticeText;

    private boolean collapsed;

    private boolean activeMode;

    public SimpleProgressView(@NonNull Context context) {
        super(context);
        b(context, null);
    }

    public SimpleProgressView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        b(context, attributeSet);
    }

    public SimpleProgressView(Context context, @Nullable AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        b(context, attributeSet);
    }

    private void c() {
        this.weatherImageView.setVisibility(this.activeMode ? 0 : 8);
        this.dayGroup.setVisibility(this.activeMode && !this.collapsed ? 0 : 8);
        this.progressGroup.setVisibility(this.activeMode && !this.collapsed ? 0 : 8);
        this.sleepingGroup.setVisibility(this.activeMode ? 8 : 0);
    }

    @Override
    public void reset() {
        this.distanceText.setText(R.string.status_loading_text);
        this.unitText.setText((CharSequence) null);
        this.captionText.setText((CharSequence) null);
        this.percentText.setText((CharSequence) null);
        this.dayText.setText((CharSequence) null);
        this.distanceProgressBar.setProgress(0);
        invalidate();
    }

    @Override
    public void setActiveMode(boolean z2) {
        this.activeMode = z2;
        c();
        invalidate();
    }

    @Override
    public void setCollapseMode(boolean z2) {
        this.collapsed = z2;
        c();
        invalidate();
    }

    @Override
    public void setDayMode(boolean z2) {
        this.weatherImageView.setImageResource(z2 ? R.drawable.clip_weather_sun : R.drawable.clip_weather_moon);
        invalidate();
    }

    @Override
    public void setMainBonusProgress(float value) {
        invalidate();
    }

    @Override
    public void setMainProgress(float value) {
        int progress = (int) (value * 100.0f);
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }
        this.distanceProgressBar.setProgress(progress);
        this.percentText.setText(getResources().getString(R.string.or_percent_format, progress));
        invalidate();
    }

    @Override
    public void setMainText(String str) {
        setMainValue(str);
    }

    @Override
    public void setMainValue(String str) {
        this.distanceText.setText(str);
        invalidate();
    }

    @Override
    public void setMainUnit(String str) {
        this.unitText.setText(str);
        invalidate();
    }

    @Override
    public void setMainCaption(String str) {
        this.captionText.setText(str);
        invalidate();
    }

    @Override
    public void setSecondaryProgress(float value) {
        invalidate();
    }

    @Override
    public void setSecondaryText(String str) {
        this.dayText.setText(str);
        invalidate();
    }

    @Override
    public void setSleepingIndicatorText(String str) {
        this.sleepingNoticeText.setText(str);
        invalidate();
    }

    @Override
    public <T> ViewTarget<ImageView, T> setWeatherDrawable(RequestBuilder<T> glideRequest) {
        return glideRequest.into(this.weatherImageView);
    }

    private void b(Context context, AttributeSet attributeSet) {
        this.activeMode = true;
        View inflate = LayoutInflater.from(context).inflate(R.layout.view_simple_progress, this, true);
        this.rootView = inflate;
        this.distanceProgressBar = (LinearProgressIndicator) inflate.findViewById(R.id.v_progress_progress_distance);
        this.progressGroup = inflate.findViewById(R.id.v_progress_progress_group);
        this.dayGroup = inflate.findViewById(R.id.v_progress_day_group);
        this.sleepingGroup = inflate.findViewById(R.id.v_progress_sleeping_group);
        this.distanceText = (TextView) inflate.findViewById(R.id.v_progress_txt_distance);
        this.unitText = (TextView) inflate.findViewById(R.id.v_progress_txt_unit);
        this.captionText = (TextView) inflate.findViewById(R.id.v_progress_txt_caption);
        this.percentText = (TextView) inflate.findViewById(R.id.v_progress_txt_percent);
        this.dayText = (TextView) inflate.findViewById(R.id.v_progress_txt_day);
        this.sleepingNoticeText = (TextView) inflate.findViewById(R.id.v_progress_txt_sleeping_notice);
        this.weatherImageView = (ImageView) inflate.findViewById(R.id.v_progress_img_weather);
        this.sleepingImageView = (ImageView) inflate.findViewById(R.id.v_progress_img_sleeping);
        this.distanceProgressBar.setMax(100);
        c();
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SimpleProgressView);
            setActiveMode(obtainStyledAttributes.getBoolean(R.styleable.SimpleProgressView_activeMode, true));
            setMainBonusProgress(obtainStyledAttributes.getFloat(R.styleable.SimpleProgressView_bonusProgress, BitmapDescriptorFactory.HUE_RED));
            setCollapseMode(obtainStyledAttributes.getBoolean(R.styleable.SimpleProgressView_collapse, false));
            setSecondaryProgress(obtainStyledAttributes.getFloat(R.styleable.SimpleProgressView_dayProgress, BitmapDescriptorFactory.HUE_RED));
            setSecondaryText(obtainStyledAttributes.getString(R.styleable.SimpleProgressView_dayText));
            setDayMode(obtainStyledAttributes.getBoolean(R.styleable.SimpleProgressView_dayTime, true));
            setMainProgress(obtainStyledAttributes.getFloat(R.styleable.SimpleProgressView_distanceProgress, BitmapDescriptorFactory.HUE_RED));
            setMainText(obtainStyledAttributes.getString(R.styleable.SimpleProgressView_distanceText));
            setSleepingIndicatorText(obtainStyledAttributes.getString(R.styleable.SimpleProgressView_sleepingText));
            obtainStyledAttributes.recycle();
        }
    }

    @Override
    public void setReferenceTime(Date date) {
        boolean z2;
        int hours = date.getHours();
        if (6 <= hours && hours <= 18) {
            z2 = true;
        } else {
            z2 = false;
        }
        setDayMode(z2);
    }
}
