package cn.edu.pku.pkurunner.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.RequestBuilder;
import cn.edu.pku.pkurunner.R;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.bumptech.glide.request.target.ViewTarget;
import java.util.Date;

public class SimpleProgressView extends LinearLayout implements ProgressableView {

    private View rootView;

    private LinearLayout progressRootLayout;

    private ImageView weatherImageView;

    private ImageView sleepingImageView;

    private ProgressBar distanceProgressBar;

    private ProgressBar dayProgressBar;

    private TextView distanceText;

    private TextView dayText;

    private TextView sleepingNoticeText;

    private boolean dayMode;

    private boolean activeMode;

    public SimpleProgressView(@NonNull Context context) {
        super(context);
        b(context, null);
    }

    private static int a(boolean z2) {
        return z2 ? 0 : 8;
    }

    private void c() {
        this.weatherImageView.setVisibility(a(this.activeMode));
        this.dayText.setVisibility(a(this.activeMode));
        boolean z2 = false;
        this.dayProgressBar.setVisibility(a(this.activeMode && !this.dayMode));
        this.distanceText.setVisibility(a(this.activeMode && !this.dayMode));
        ProgressBar progressBar = this.distanceProgressBar;
        if (this.activeMode && !this.dayMode) {
            z2 = true;
        }
        progressBar.setVisibility(a(z2));
        this.sleepingImageView.setVisibility(a(!this.activeMode));
        this.sleepingNoticeText.setVisibility(a(!this.activeMode));
    }

    @Override
    public void reset() {
        this.distanceText.setText(R.string.status_loading_text);
        this.dayText.setText((CharSequence) null);
        this.distanceProgressBar.setProgress(0);
        this.distanceProgressBar.setSecondaryProgress(0);
        this.dayProgressBar.setProgress(0);
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
        this.dayMode = z2;
        c();
        invalidate();
    }

    @Override
    public void setDayMode(boolean z2) {
        this.progressRootLayout.setBackgroundColor(getResources().getColor(z2 ? R.color.red_500 : R.color.grey_800));
        this.weatherImageView.setImageResource(z2 ? R.drawable.clip_weather_sun : R.drawable.clip_weather_moon);
        this.sleepingImageView.setColorFilter(getResources().getColor(z2 ? R.color.grey_900 : R.color.grey_200));
        invalidate();
    }

    @Override
    public void setMainBonusProgress(float value) {
        this.distanceProgressBar.setSecondaryProgress((int) (value * 100.0f));
        invalidate();
    }

    @Override
    public void setMainProgress(float value) {
        this.distanceProgressBar.setProgress((int) (value * 100.0f));
        invalidate();
    }

    @Override
    public void setMainText(String str) {
        this.distanceText.setText(str);
        invalidate();
    }

    @Override
    public void setSecondaryProgress(float value) {
        this.dayProgressBar.setProgress((int) (value * 100.0f));
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

    public SimpleProgressView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        b(context, attributeSet);
    }

    private void b(Context context, AttributeSet attributeSet) {
        View inflate = View.inflate(context, R.layout.view_simple_progress, this);
        this.rootView = inflate;
        this.progressRootLayout = (LinearLayout) inflate.findViewById(R.id.v_progress_root);
        this.distanceProgressBar = (ProgressBar) this.rootView.findViewById(R.id.v_progress_progress_distance);
        this.dayProgressBar = (ProgressBar) this.rootView.findViewById(R.id.v_progress_progress_day);
        this.distanceText = (TextView) this.rootView.findViewById(R.id.v_progress_txt_distance);
        this.dayText = (TextView) this.rootView.findViewById(R.id.v_progress_txt_day);
        this.sleepingNoticeText = (TextView) this.rootView.findViewById(R.id.v_progress_txt_sleeping_notice);
        this.weatherImageView = (ImageView) this.rootView.findViewById(R.id.v_progress_img_weather);
        this.sleepingImageView = (ImageView) this.rootView.findViewById(R.id.v_progress_img_sleeping);
        this.distanceProgressBar.setMax(100);
        this.dayProgressBar.setMax(100);
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SimpleProgressView);
            int indexCount = obtainStyledAttributes.getIndexCount();
            for (int index = 0; index < indexCount; index++) {
                switch (obtainStyledAttributes.getIndex(index)) {
                    case 0:
                        setActiveMode(obtainStyledAttributes.getBoolean(index, false));
                        break;
                    case 1:
                        setMainBonusProgress(obtainStyledAttributes.getFloat(index, BitmapDescriptorFactory.HUE_RED));
                        break;
                    case 2:
                        setCollapseMode(obtainStyledAttributes.getBoolean(index, false));
                        break;
                    case 3:
                        setSecondaryProgress(obtainStyledAttributes.getFloat(index, BitmapDescriptorFactory.HUE_RED));
                        break;
                    case 4:
                        setSecondaryText(obtainStyledAttributes.getString(index));
                        break;
                    case 5:
                        setDayMode(obtainStyledAttributes.getBoolean(index, true));
                        break;
                    case 6:
                        setMainProgress(obtainStyledAttributes.getFloat(index, BitmapDescriptorFactory.HUE_RED));
                        break;
                    case 7:
                        setMainText(obtainStyledAttributes.getString(index));
                        break;
                    case 8:
                        setSleepingIndicatorText(obtainStyledAttributes.getString(index));
                        break;
                }
            }
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
