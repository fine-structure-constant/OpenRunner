package cn.edu.pku.pkurunner.TaskList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.TaskList.BadgeResourceResolver;

public class BadgeView extends FrameLayout {

    /* renamed from: f, reason: collision with root package name */
    private static float f7089f = 0.5f;

    /* renamed from: g, reason: collision with root package name */
    private static float f7090g = 0.77f;

    /* renamed from: a, reason: collision with root package name */
    private View f7091a;

    /* renamed from: b, reason: collision with root package name */
    private ImageView f7092b;

    /* renamed from: c, reason: collision with root package name */
    private TextView f7093c;

    /* renamed from: d, reason: collision with root package name */
    private String f7094d;

    /* renamed from: e, reason: collision with root package name */
    private boolean f7095e;

    public BadgeView(@NonNull Context context) {
        super(context);
        this.f7095e = false;
        a(context, null);
    }

    private void b() {
        if (this.f7095e) {
            try {
                this.f7092b.setImageResource(BadgeResourceResolver.resolve(this.f7094d));
                this.f7093c.setVisibility(0);
            } catch (BadgeResourceResolver.a unused) {
                this.f7092b.setImageResource(BadgeResourceResolver.NULL_RESOURCE);
                this.f7093c.setVisibility(8);
            }
        } else {
            this.f7092b.setImageResource(BadgeResourceResolver.UNACHIEVED_RESOURCE);
            this.f7093c.setVisibility(8);
        }
        invalidate();
    }

    public void setBadgeSeries(String str) {
        this.f7094d = str;
        b();
    }

    public void setDistance(int i2) {
        this.f7093c.setText(String.valueOf(i2));
        invalidate();
    }

    public void setStatus(boolean z2) {
        this.f7095e = z2;
        b();
    }

    public BadgeView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.f7095e = false;
        a(context, attributeSet);
    }

    private void a(Context context, AttributeSet attributeSet) {
        View inflate = View.inflate(context, R.layout.view_badge, this);
        this.f7091a = inflate;
        this.f7092b = (ImageView) inflate.findViewById(R.id.v_badge_img);
        this.f7093c = (TextView) this.f7091a.findViewById(R.id.v_badge_txt);
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.BadgeView);
            int indexCount = obtainStyledAttributes.getIndexCount();
            for (int i2 = 0; i2 < indexCount; i2++) {
                int index = obtainStyledAttributes.getIndex(i2);
                if (index != 0) {
                    if (index == 1) {
                        f7090g = obtainStyledAttributes.getFloat(i2, f7090g);
                    }
                } else {
                    setDistance(obtainStyledAttributes.getInteger(i2, 0));
                }
            }
            obtainStyledAttributes.recycle();
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.f7092b.draw(canvas);
        this.f7093c.draw(canvas);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z2, int i2, int i3, int i4, int i5) {
        super.onLayout(z2, i2, i3, i4, i5);
        float x2 = (this.f7092b.getX() + (this.f7092b.getWidth() * f7089f)) - (this.f7093c.getWidth() / 2);
        float y2 = (this.f7092b.getY() + (this.f7092b.getHeight() * f7090g)) - (this.f7093c.getHeight() / 2);
        this.f7093c.setX(x2);
        this.f7093c.setY(y2);
    }
}
