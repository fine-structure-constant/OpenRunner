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

    /* renamed from: a, reason: collision with root package name */
    private View f7162a;

    /* renamed from: b, reason: collision with root package name */
    private LinearLayout f7163b;

    /* renamed from: c, reason: collision with root package name */
    private ImageView f7164c;

    /* renamed from: d, reason: collision with root package name */
    private ImageView f7165d;

    /* renamed from: e, reason: collision with root package name */
    private ProgressBar f7166e;

    /* renamed from: f, reason: collision with root package name */
    private ProgressBar f7167f;

    /* renamed from: g, reason: collision with root package name */
    private TextView f7168g;

    /* renamed from: h, reason: collision with root package name */
    private TextView f7169h;

    /* renamed from: i, reason: collision with root package name */
    private TextView f7170i;

    /* renamed from: j, reason: collision with root package name */
    private boolean f7171j;

    /* renamed from: k, reason: collision with root package name */
    private boolean f7172k;

    public SimpleProgressView(@NonNull Context context) {
        super(context);
        b(context, null);
    }

    private static int a(boolean z2) {
        return z2 ? 0 : 8;
    }

    private void c() {
        this.f7164c.setVisibility(a(this.f7172k));
        this.f7169h.setVisibility(a(this.f7172k));
        boolean z2 = false;
        this.f7167f.setVisibility(a(this.f7172k && !this.f7171j));
        this.f7168g.setVisibility(a(this.f7172k && !this.f7171j));
        ProgressBar progressBar = this.f7166e;
        if (this.f7172k && !this.f7171j) {
            z2 = true;
        }
        progressBar.setVisibility(a(z2));
        this.f7165d.setVisibility(a(!this.f7172k));
        this.f7170i.setVisibility(a(!this.f7172k));
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void reset() {
        this.f7168g.setText(R.string.status_loading_text);
        this.f7169h.setText((CharSequence) null);
        this.f7166e.setProgress(0);
        this.f7166e.setSecondaryProgress(0);
        this.f7167f.setProgress(0);
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setActiveMode(boolean z2) {
        this.f7172k = z2;
        c();
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setCollapseMode(boolean z2) {
        this.f7171j = z2;
        c();
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setDayMode(boolean z2) {
        this.f7163b.setBackgroundColor(getResources().getColor(z2 ? R.color.red_500 : R.color.grey_800));
        this.f7164c.setImageResource(z2 ? R.drawable.clip_weather_sun : R.drawable.clip_weather_moon);
        this.f7165d.setColorFilter(getResources().getColor(z2 ? R.color.grey_900 : R.color.grey_200));
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setMainBonusProgress(float f2) {
        this.f7166e.setSecondaryProgress((int) (f2 * 100.0f));
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setMainProgress(float f2) {
        this.f7166e.setProgress((int) (f2 * 100.0f));
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setMainText(String str) {
        this.f7168g.setText(str);
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setSecondaryProgress(float f2) {
        this.f7167f.setProgress((int) (f2 * 100.0f));
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setSecondaryText(String str) {
        this.f7169h.setText(str);
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public void setSleepingIndicatorText(String str) {
        this.f7170i.setText(str);
        invalidate();
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
    public <T> ViewTarget<ImageView, T> setWeatherDrawable(RequestBuilder<T> glideRequest) {
        return glideRequest.into(this.f7164c);
    }

    public SimpleProgressView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        b(context, attributeSet);
    }

    private void b(Context context, AttributeSet attributeSet) {
        View inflate = View.inflate(context, R.layout.view_simple_progress, this);
        this.f7162a = inflate;
        this.f7163b = (LinearLayout) inflate.findViewById(R.id.v_progress_root);
        this.f7166e = (ProgressBar) this.f7162a.findViewById(R.id.v_progress_progress_distance);
        this.f7167f = (ProgressBar) this.f7162a.findViewById(R.id.v_progress_progress_day);
        this.f7168g = (TextView) this.f7162a.findViewById(R.id.v_progress_txt_distance);
        this.f7169h = (TextView) this.f7162a.findViewById(R.id.v_progress_txt_day);
        this.f7170i = (TextView) this.f7162a.findViewById(R.id.v_progress_txt_sleeping_notice);
        this.f7164c = (ImageView) this.f7162a.findViewById(R.id.v_progress_img_weather);
        this.f7165d = (ImageView) this.f7162a.findViewById(R.id.v_progress_img_sleeping);
        this.f7166e.setMax(100);
        this.f7167f.setMax(100);
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SimpleProgressView);
            int indexCount = obtainStyledAttributes.getIndexCount();
            for (int i2 = 0; i2 < indexCount; i2++) {
                switch (obtainStyledAttributes.getIndex(i2)) {
                    case 0:
                        setActiveMode(obtainStyledAttributes.getBoolean(i2, false));
                        break;
                    case 1:
                        setMainBonusProgress(obtainStyledAttributes.getFloat(i2, BitmapDescriptorFactory.HUE_RED));
                        break;
                    case 2:
                        setCollapseMode(obtainStyledAttributes.getBoolean(i2, false));
                        break;
                    case 3:
                        setSecondaryProgress(obtainStyledAttributes.getFloat(i2, BitmapDescriptorFactory.HUE_RED));
                        break;
                    case 4:
                        setSecondaryText(obtainStyledAttributes.getString(i2));
                        break;
                    case 5:
                        setDayMode(obtainStyledAttributes.getBoolean(i2, true));
                        break;
                    case 6:
                        setMainProgress(obtainStyledAttributes.getFloat(i2, BitmapDescriptorFactory.HUE_RED));
                        break;
                    case 7:
                        setMainText(obtainStyledAttributes.getString(i2));
                        break;
                    case 8:
                        setSleepingIndicatorText(obtainStyledAttributes.getString(i2));
                        break;
                }
            }
            obtainStyledAttributes.recycle();
        }
    }

    @Override // cn.edu.pku.pkurunner.View.ProgressableView
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
