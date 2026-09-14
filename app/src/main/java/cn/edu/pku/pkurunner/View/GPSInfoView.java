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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import cn.edu.pku.pkurunner.R;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.google.android.material.card.MaterialCardView;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.util.concurrent.TimeUnit;

public class GPSInfoView extends MaterialCardView {

    private static final int[] SIGNAL_LEVEL_DRAWABLES;

    private static final int SIGNAL_LEVEL_COUNT;

    private ImageView signalImageView;

    private TextView infoText;

    private ObservableEmitter clickEmitter;

    private boolean persistent;

    static {
        int[] iArr = {R.drawable.ic_signal_cellular_0_bar, R.drawable.ic_signal_cellular_1_bar, R.drawable.ic_signal_cellular_2_bar, R.drawable.ic_signal_cellular_3_bar, R.drawable.ic_signal_cellular_4_bar};
        SIGNAL_LEVEL_DRAWABLES = iArr;
        SIGNAL_LEVEL_COUNT = iArr.length;
    }

    public GPSInfoView(@NonNull Context context) {
        super(context);
        this.persistent = true;
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

    public /* synthetic */ void j(ObservableEmitter observableEmitter) {
        this.clickEmitter = observableEmitter;
    }

    public void setSignalStrength(int index2) {
        this.signalImageView.setImageResource(SIGNAL_LEVEL_DRAWABLES[m(index2)]);
        invalidate();
    }

    private int m(int index2) {
        return Math.max(0, Math.min(SIGNAL_LEVEL_COUNT - 1, index2));
    }

    public void notifyVisible() {
        this.clickEmitter.onNext(Boolean.TRUE);
    }

    public void setAppear(boolean z2) {
        setTranslationY(z2 ? BitmapDescriptorFactory.HUE_RED : -getOuterHeight());
        invalidate();
    }

    public void setInfoText(String str) {
        this.infoText.setText(str);
        invalidate();
    }

    public void setPersistent(boolean z2) {
        this.persistent = z2;
        setAppear(z2);
    }

    public GPSInfoView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.persistent = true;
        g(context, attributeSet);
    }

    private void g(Context context, AttributeSet attributeSet) {
        View.inflate(context, R.layout.view_gps_indicator, this);
        this.signalImageView = (ImageView) findViewById(R.id.v_gps_img_signal);
        this.infoText = (TextView) findViewById(R.id.v_gps_txt_info);
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.GPSInfoView);
            for (int index2 = 0; index2 < obtainStyledAttributes.getIndexCount(); index2++) {
                int index = obtainStyledAttributes.getIndex(index2);
                if (index != 0) {
                    if (index != 1) {
                        if (index == 2) {
                            setInfoText(obtainStyledAttributes.getString(index2));
                        }
                    } else {
                        setSignalStrength(obtainStyledAttributes.getInteger(index2, 0));
                    }
                } else {
                    setAppear(obtainStyledAttributes.getBoolean(index2, true));
                }
            }
            obtainStyledAttributes.recycle();
        }
        if (!isInEditMode()) {
            Observable create = Observable.create(new ObservableOnSubscribe() {
                @Override
                public final void subscribe(ObservableEmitter observableEmitter) {
                    GPSInfoView.this.j(observableEmitter);
                }
            });
            TimeUnit timeUnit = TimeUnit.SECONDS;
            create.throttleFirst(3L, timeUnit).observeOn(AndroidSchedulers.mainThread()).doOnNext(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    GPSInfoView.this.k((Boolean) obj);
                }
            }).debounce(5L, timeUnit).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
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

    public /* synthetic */ void k(Boolean bool) {
        if (h()) {
            e();
        }
    }

    public /* synthetic */ void l(Boolean bool) {
        if (i() && !this.persistent) {
            f();
        }
    }

    public void setSignalStrength(double value) {
        setSignalStrength((int) (SIGNAL_LEVEL_COUNT * value));
    }
}
