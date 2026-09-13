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

    /* renamed from: a, reason: collision with root package name */
    private View f7152a;

    /* renamed from: b, reason: collision with root package name */
    private FrameLayout f7153b;

    /* renamed from: c, reason: collision with root package name */
    private ImageView f7154c;

    /* renamed from: d, reason: collision with root package name */
    private ImageView f7155d;

    /* renamed from: e, reason: collision with root package name */
    private ImageView f7156e;

    /* renamed from: f, reason: collision with root package name */
    private TextView f7157f;

    /* renamed from: g, reason: collision with root package name */
    private TextView f7158g;

    /* renamed from: h, reason: collision with root package name */
    private float f7159h;

    /* renamed from: i, reason: collision with root package name */
    private float f7160i;

    /* renamed from: j, reason: collision with root package name */
    private boolean f7161j;

    public RunningProgressView(@NonNull Context context) {
        super(context);
        this.f7159h = BitmapDescriptorFactory.HUE_RED;
        this.f7160i = BitmapDescriptorFactory.HUE_RED;
        this.f7161j = true;
        b(context, null);
    }

    private float a(float f2) {
        return Math.min(1.0f, Math.max(BitmapDescriptorFactory.HUE_RED, f2));
    }

    private float c(float f2, float f3, float f4) {
        return f3 + (Math.min(Math.max(f2, BitmapDescriptorFactory.HUE_RED), 1.0f) * (f4 - f3));
    }

    private void d(View view, float f2, int i2, int i3) {
        view.setX(c(f2, BitmapDescriptorFactory.HUE_RED, (i3 - i2) - view.getWidth()));
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setActiveMode(boolean z2) {
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setCollapseMode(boolean z2) {
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setMainBonusProgress(float f2) {
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setSleepingIndicatorText(String str) {
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public <T> ViewTarget<ImageView, T> setWeatherDrawable(RequestBuilder<T> glideRequest) {
        return null;
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void reset() {
        this.f7158g.setText(R.string.status_loading_text);
        this.f7157f.setText((CharSequence) null);
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setDayMode(boolean z2) {
        this.f7161j = z2;
        this.f7153b.setBackgroundColor(getResources().getColor(z2 ? R.color.white : R.color.grey_800));
        this.f7158g.setTextColor(getResources().getColor(z2 ? R.color.black : R.color.white));
        this.f7158g.setAlpha(z2 ? 0.87f : 1.0f);
        this.f7157f.setTextColor(getResources().getColor(z2 ? R.color.black : R.color.white));
        this.f7157f.setAlpha(z2 ? 0.87f : 1.0f);
        this.f7156e.setImageResource(z2 ? R.drawable.clip_weather_sun : R.drawable.clip_weather_moon);
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setMainProgress(float f2) {
        this.f7159h = f2;
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setMainText(String str) {
        this.f7158g.setText(str);
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setSecondaryProgress(float f2) {
        this.f7160i = f2;
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setSecondaryText(String str) {
        this.f7157f.setText(str);
        invalidate();
    }

    public RunningProgressView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.f7159h = BitmapDescriptorFactory.HUE_RED;
        this.f7160i = BitmapDescriptorFactory.HUE_RED;
        this.f7161j = true;
        b(context, attributeSet);
    }

    private void b(Context context, AttributeSet attributeSet) {
        View inflate = View.inflate(context, R.layout.view_running_progress, this);
        this.f7152a = inflate;
        this.f7153b = (FrameLayout) inflate.findViewById(R.id.v_progress_root);
        this.f7154c = (ImageView) this.f7152a.findViewById(R.id.v_progress_img_group);
        this.f7157f = (TextView) this.f7152a.findViewById(R.id.v_progress_txt_group);
        this.f7155d = (ImageView) this.f7152a.findViewById(R.id.v_progress_img_runner);
        this.f7158g = (TextView) this.f7152a.findViewById(R.id.v_progress_txt_runner);
        this.f7156e = (ImageView) this.f7152a.findViewById(R.id.v_progress_img_weather);
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RunningProgressView);
            int indexCount = obtainStyledAttributes.getIndexCount();
            for (int i2 = 0; i2 < indexCount; i2++) {
                int index = obtainStyledAttributes.getIndex(i2);
                if (index != 0) {
                    if (index != 1) {
                        if (index != 2) {
                            if (index != 3) {
                                if (index == 4) {
                                    setSecondaryText(obtainStyledAttributes.getString(i2));
                                }
                            } else {
                                setSecondaryProgress(obtainStyledAttributes.getFloat(i2, this.f7160i));
                            }
                        } else {
                            setMainText(obtainStyledAttributes.getString(i2));
                        }
                    } else {
                        setMainProgress(obtainStyledAttributes.getFloat(i2, this.f7159h));
                    }
                } else {
                    setDayMode(obtainStyledAttributes.getBoolean(i2, this.f7161j));
                }
            }
            obtainStyledAttributes.recycle();
        }
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z2, int i2, int i3, int i4, int i5) {
        super.onLayout(z2, i2, i3, i4, i5);
        d(this.f7155d, a(this.f7159h), i2, i4);
        d(this.f7154c, a(this.f7160i), i2, i4);
        if (this.f7159h < 0.5f) {
            this.f7158g.setX(this.f7155d.getX() + this.f7155d.getWidth());
        } else {
            this.f7158g.setX(this.f7155d.getX() - this.f7158g.getWidth());
        }
        if (this.f7160i < 0.5f) {
            this.f7157f.setX(this.f7154c.getX() + this.f7154c.getWidth());
            d(this.f7156e, 0.75f, i2, i4);
        } else {
            this.f7157f.setX(this.f7154c.getX() - this.f7157f.getWidth());
            d(this.f7156e, 0.25f, i2, i4);
        }
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
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
