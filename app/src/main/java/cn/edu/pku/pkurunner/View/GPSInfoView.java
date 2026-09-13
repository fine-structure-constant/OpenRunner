package cn.edu.pku.pkurunner.View;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import cn.edu.pku.pkurunner.R;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.util.concurrent.TimeUnit;

public class GPSInfoView extends CardView {

    /* renamed from: n, reason: collision with root package name */
    private static final int[] f7146n;

    /* renamed from: o, reason: collision with root package name */
    private static final int f7147o;

    /* renamed from: j, reason: collision with root package name */
    private ImageView f7148j;

    /* renamed from: k, reason: collision with root package name */
    private TextView f7149k;

    /* renamed from: l, reason: collision with root package name */
    private ObservableEmitter f7150l;

    /* renamed from: m, reason: collision with root package name */
    private boolean f7151m;

    static {
        int[] iArr = {R.drawable.ic_signal_cellular_0_bar, R.drawable.ic_signal_cellular_1_bar, R.drawable.ic_signal_cellular_2_bar, R.drawable.ic_signal_cellular_3_bar, R.drawable.ic_signal_cellular_4_bar};
        f7146n = iArr;
        f7147o = iArr.length;
    }

    public GPSInfoView(@NonNull Context context) {
        super(context);
        this.f7151m = true;
        g(context, null);
    }

    private void e() {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, "translationY", -getOuterHeight(), BitmapDescriptorFactory.HUE_RED);
        ofFloat.setInterpolator(new LinearOutSlowInInterpolator());
        ofFloat.setDuration(250L);
        ofFloat.start();
    }

    private void f() {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, "translationY", BitmapDescriptorFactory.HUE_RED, -getOuterHeight());
        ofFloat.setInterpolator(new FastOutLinearInInterpolator());
        ofFloat.setDuration(250L);
        ofFloat.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void j(ObservableEmitter observableEmitter) {
        this.f7150l = observableEmitter;
    }

    public void setSignalStrength(int i2) {
        this.f7148j.setImageResource(f7146n[m(i2)]);
        invalidate();
    }

    private int m(int i2) {
        return Math.max(0, Math.min(f7147o - 1, i2));
    }

    public void notifyVisible() {
        this.f7150l.onNext(Boolean.TRUE);
    }

    public void setAppear(boolean z2) {
        setTranslationY(z2 ? BitmapDescriptorFactory.HUE_RED : -getOuterHeight());
        invalidate();
    }

    public void setInfoText(String str) {
        this.f7149k.setText(str);
        invalidate();
    }

    public void setPersistent(boolean z2) {
        this.f7151m = z2;
        setAppear(z2);
    }

    public GPSInfoView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.f7151m = true;
        g(context, attributeSet);
    }

    private void g(Context context, AttributeSet attributeSet) {
        View.inflate(context, R.layout.view_gps_indicator, this);
        this.f7148j = (ImageView) findViewById(R.id.v_gps_img_signal);
        this.f7149k = (TextView) findViewById(R.id.v_gps_txt_info);
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.GPSInfoView);
            for (int i2 = 0; i2 < obtainStyledAttributes.getIndexCount(); i2++) {
                int index = obtainStyledAttributes.getIndex(i2);
                if (index != 0) {
                    if (index != 1) {
                        if (index == 2) {
                            setInfoText(obtainStyledAttributes.getString(i2));
                        }
                    } else {
                        setSignalStrength(obtainStyledAttributes.getInteger(i2, 0));
                    }
                } else {
                    setAppear(obtainStyledAttributes.getBoolean(i2, true));
                }
            }
            obtainStyledAttributes.recycle();
        }
        if (!isInEditMode()) {
            Observable create = Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.View.a
                @Override // io.reactivex.ObservableOnSubscribe
                public final void subscribe(ObservableEmitter observableEmitter) {
                    GPSInfoView.this.j(observableEmitter);
                }
            });
            TimeUnit timeUnit = TimeUnit.SECONDS;
            create.throttleFirst(3L, timeUnit).observeOn(AndroidSchedulers.mainThread()).doOnNext(new Consumer() { // from class: cn.edu.pku.pkurunner.View.b
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    GPSInfoView.this.k((Boolean) obj);
                }
            }).debounce(5L, timeUnit).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.View.c
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    GPSInfoView.this.l((Boolean) obj);
                }
            });
        }
    }

    private float getOuterHeight() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams == null) {
            return getHeight();
        }
        ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) layoutParams;
        return getHeight() + margins.topMargin + margins.bottomMargin;
    }

    private boolean h() {
        if (Math.abs(getTranslationY() + getOuterHeight()) < 1.0E-6d) {
            return true;
        }
        return false;
    }

    private boolean i() {
        if (Math.abs(getTranslationY()) < 1.0E-6d) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void k(Boolean bool) {
        if (h()) {
            e();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void l(Boolean bool) {
        if (i() && !this.f7151m) {
            f();
        }
    }

    public void setSignalStrength(double d2) {
        setSignalStrength((int) (f7147o * d2));
    }
}
