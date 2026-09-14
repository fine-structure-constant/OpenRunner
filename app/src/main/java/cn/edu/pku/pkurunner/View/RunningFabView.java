package cn.edu.pku.pkurunner.View;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cn.edu.pku.pkurunner.R;
import com.google.android.material.card.MaterialCardView;

public class RunningFabView extends MaterialCardView {

    private boolean hidden;

    public RunningFabView(@NonNull Context context) {
        super(context);
        b(context);
    }

    public RunningFabView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        b(context);
    }

    public RunningFabView(Context context, @Nullable AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        b(context);
    }

    private void b(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_running_fab, this, true);
        setClickable(true);
        setFocusable(true);
    }

    public void show() {
        if (!this.hidden && getVisibility() == 0) {
            return;
        }
        this.hidden = false;
        setVisibility(0);
        animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(180L).start();
    }

    public void hide() {
        if (this.hidden) {
            return;
        }
        this.hidden = true;
        animate().scaleX(0.0f).scaleY(0.0f).alpha(0.0f).setDuration(180L).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (RunningFabView.this.hidden) {
                    RunningFabView.this.setVisibility(8);
                }
            }
        }).start();
    }

    public boolean isHidden() {
        return this.hidden;
    }
}
