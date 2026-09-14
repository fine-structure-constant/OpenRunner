package cn.edu.pku.pkurunner.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.RequestBuilder;
import cn.edu.pku.pkurunner.R;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.bumptech.glide.request.target.ViewTarget;
import java.util.Date;
import org.xutils.common.util.LogUtil;

public class RunningProgressView extends FrameLayout implements ProgressableView {

    private View rootView;

    private FrameLayout progressRootLayout;

    private ImageView groupImageView;

    private ImageView runnerImageView;

    private ImageView weatherImageView;

    private TextView groupText;

    private TextView runnerText;

    private float mainProgress;

    private float secondaryProgress;

    private boolean dayMode;

    public RunningProgressView(@NonNull Context context) {
        super(context);
        this.mainProgress = BitmapDescriptorFactory.HUE_RED;
        this.secondaryProgress = BitmapDescriptorFactory.HUE_RED;
        this.dayMode = true;
        b(context, null);
    }

    private float a(float value) {
        return Math.min(1.0f, Math.max(BitmapDescriptorFactory.HUE_RED, value));
    }

    private float c(float value, float value2, float f4) {
        return value2 + (Math.min(Math.max(value, BitmapDescriptorFactory.HUE_RED), 1.0f) * (f4 - value2));
    }

    private void d(View view, float value, int index2, int index3) {
        view.setX(c(value, BitmapDescriptorFactory.HUE_RED, (index3 - index2) - view.getWidth()));
    }

    @Override
    public void setActiveMode(boolean z2) {
    }

    @Override
    public void setCollapseMode(boolean z2) {
    }

    @Override
    public void setMainBonusProgress(float value) {
    }

    @Override
    public void setSleepingIndicatorText(String str) {
    }

    @Override
    public <T> ViewTarget<ImageView, T> setWeatherDrawable(RequestBuilder<T> glideRequest) {
        return null;
    }

    @Override
    public void reset() {
        this.runnerText.setText(R.string.status_loading_text);
        this.groupText.setText((CharSequence) null);
        invalidate();
    }

    @Override
    public void setDayMode(boolean z2) {
        this.dayMode = z2;
        this.progressRootLayout.setBackgroundColor(getResources().getColor(z2 ? R.color.white : R.color.grey_800));
        this.runnerText.setTextColor(getResources().getColor(z2 ? R.color.black : R.color.white));
        this.runnerText.setAlpha(z2 ? 0.87f : 1.0f);
        this.groupText.setTextColor(getResources().getColor(z2 ? R.color.black : R.color.white));
        this.groupText.setAlpha(z2 ? 0.87f : 1.0f);
        this.weatherImageView.setImageResource(z2 ? R.drawable.clip_weather_sun : R.drawable.clip_weather_moon);
        invalidate();
    }

    @Override
    public void setMainProgress(float value) {
        this.mainProgress = value;
        invalidate();
    }

    @Override
    public void setMainText(String str) {
        this.runnerText.setText(str);
        invalidate();
    }

    @Override
    public void setSecondaryProgress(float value) {
        this.secondaryProgress = value;
        invalidate();
    }

    @Override
    public void setSecondaryText(String str) {
        this.groupText.setText(str);
        invalidate();
    }

    public RunningProgressView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mainProgress = BitmapDescriptorFactory.HUE_RED;
        this.secondaryProgress = BitmapDescriptorFactory.HUE_RED;
        this.dayMode = true;
        b(context, attributeSet);
    }

    private void b(Context context, AttributeSet attributeSet) {
        View inflate = View.inflate(context, R.layout.view_running_progress, this);
        this.rootView = inflate;
        this.progressRootLayout = (FrameLayout) inflate.findViewById(R.id.v_progress_root);
        this.groupImageView = (ImageView) this.rootView.findViewById(R.id.v_progress_img_group);
        this.groupText = (TextView) this.rootView.findViewById(R.id.v_progress_txt_group);
        this.runnerImageView = (ImageView) this.rootView.findViewById(R.id.v_progress_img_runner);
        this.runnerText = (TextView) this.rootView.findViewById(R.id.v_progress_txt_runner);
        this.weatherImageView = (ImageView) this.rootView.findViewById(R.id.v_progress_img_weather);
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RunningProgressView);
            int indexCount = obtainStyledAttributes.getIndexCount();
            for (int index2 = 0; index2 < indexCount; index2++) {
                int index = obtainStyledAttributes.getIndex(index2);
                if (index != 0) {
                    if (index != 1) {
                        if (index != 2) {
                            if (index != 3) {
                                if (index == 4) {
                                    setSecondaryText(obtainStyledAttributes.getString(index2));
                                }
                            } else {
                                setSecondaryProgress(obtainStyledAttributes.getFloat(index2, this.secondaryProgress));
                            }
                        } else {
                            setMainText(obtainStyledAttributes.getString(index2));
                        }
                    } else {
                        setMainProgress(obtainStyledAttributes.getFloat(index2, this.mainProgress));
                    }
                } else {
                    setDayMode(obtainStyledAttributes.getBoolean(index2, this.dayMode));
                }
            }
            obtainStyledAttributes.recycle();
        }
    }

    @Override
    protected void onLayout(boolean z2, int index2, int index3, int index4, int i5) {
        super.onLayout(z2, index2, index3, index4, i5);
        d(this.runnerImageView, a(this.mainProgress), index2, index4);
        d(this.groupImageView, a(this.secondaryProgress), index2, index4);
        if (this.mainProgress < 0.5f) {
            this.runnerText.setX(this.runnerImageView.getX() + this.runnerImageView.getWidth());
        } else {
            this.runnerText.setX(this.runnerImageView.getX() - this.runnerText.getWidth());
        }
        if (this.secondaryProgress < 0.5f) {
            this.groupText.setX(this.groupImageView.getX() + this.groupImageView.getWidth());
            d(this.weatherImageView, 0.75f, index2, index4);
        } else {
            this.groupText.setX(this.groupImageView.getX() - this.groupText.getWidth());
            d(this.weatherImageView, 0.25f, index2, index4);
        }
    }

    @Override
    public void setReferenceTime(Date date) {
        boolean z2;
        int hours = date.getHours();
        LogUtil.d("Hours is " + hours);
        if (6 <= hours && hours <= 18) {
            z2 = true;
        } else {
            z2 = false;
        }
        setDayMode(z2);
    }
}
