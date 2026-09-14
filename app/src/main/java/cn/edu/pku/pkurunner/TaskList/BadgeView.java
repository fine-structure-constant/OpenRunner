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

    private static float BADGE_ANCHOR_RATIO_X = 0.5f;

    private static float BADGE_ANCHOR_RATIO_Y = 0.77f;

    private View rootView;

    private ImageView badgeImageView;

    private TextView badgeText;

    private String badgeName;

    private boolean badgeLoaded;

    public BadgeView(@NonNull Context context) {
        super(context);
        this.badgeLoaded = false;
        a(context, null);
    }

    private void b() {
        if (this.badgeLoaded) {
            try {
                this.badgeImageView.setImageResource(BadgeResourceResolver.resolve(this.badgeName));
                this.badgeText.setVisibility(0);
            } catch (BadgeResourceResolver.UnknownBadgeException unused) {
                this.badgeImageView.setImageResource(BadgeResourceResolver.NULL_RESOURCE);
                this.badgeText.setVisibility(8);
            }
        } else {
            this.badgeImageView.setImageResource(BadgeResourceResolver.UNACHIEVED_RESOURCE);
            this.badgeText.setVisibility(8);
        }
        invalidate();
    }

    public void setBadgeSeries(String str) {
        this.badgeName = str;
        b();
    }

    public void setDistance(int index2) {
        this.badgeText.setText(String.valueOf(index2));
        invalidate();
    }

    public void setStatus(boolean z2) {
        this.badgeLoaded = z2;
        b();
    }

    public BadgeView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.badgeLoaded = false;
        a(context, attributeSet);
    }

    private void a(Context context, AttributeSet attributeSet) {
        View inflate = View.inflate(context, R.layout.view_badge, this);
        this.rootView = inflate;
        this.badgeImageView = (ImageView) inflate.findViewById(R.id.v_badge_img);
        this.badgeText = (TextView) this.rootView.findViewById(R.id.v_badge_txt);
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.BadgeView);
            int indexCount = obtainStyledAttributes.getIndexCount();
            for (int index2 = 0; index2 < indexCount; index2++) {
                int index = obtainStyledAttributes.getIndex(index2);
                if (index != 0) {
                    if (index == 1) {
                        BADGE_ANCHOR_RATIO_Y = obtainStyledAttributes.getFloat(index2, BADGE_ANCHOR_RATIO_Y);
                    }
                } else {
                    setDistance(obtainStyledAttributes.getInteger(index2, 0));
                }
            }
            obtainStyledAttributes.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.badgeImageView.draw(canvas);
        this.badgeText.draw(canvas);
    }

    @Override
    protected void onLayout(boolean z2, int index2, int index3, int index4, int i5) {
        super.onLayout(z2, index2, index3, index4, i5);
        float x2 = (this.badgeImageView.getX() + (this.badgeImageView.getWidth() * BADGE_ANCHOR_RATIO_X)) - (this.badgeText.getWidth() / 2);
        float y2 = (this.badgeImageView.getY() + (this.badgeImageView.getHeight() * BADGE_ANCHOR_RATIO_Y)) - (this.badgeText.getHeight() / 2);
        this.badgeText.setX(x2);
        this.badgeText.setY(y2);
    }
}
