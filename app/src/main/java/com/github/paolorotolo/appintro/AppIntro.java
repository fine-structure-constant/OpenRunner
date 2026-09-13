package com.github.paolorotolo.appintro;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

/** Small local compatibility shell for the original AppIntro 4.x API. */
public class AppIntro extends AppCompatActivity {
    private final List<Fragment> slides = new ArrayList<>();
    private FrameLayout container;
    private Button next;
    private Button done;
    private int current;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(34, 34, 34));
        container = new FrameLayout(this);
        container.setId(View.generateViewId());
        root.addView(container, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        next = new Button(this);
        next.setText("下一页");
        done = new Button(this);
        done.setText("完成");
        done.setVisibility(View.GONE);
        bar.addView(next, new LinearLayout.LayoutParams(-2, -2));
        bar.addView(done, new LinearLayout.LayoutParams(-2, -2));
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        next.setOnClickListener(v -> showSlide(current + 1));
        done.setOnClickListener(v -> onDonePressed(slides.isEmpty() ? null : slides.get(current)));
    }

    protected void addSlide(Fragment fragment) {
        slides.add(fragment);
        if (slides.size() == 1) showSlide(0);
    }

    private void showSlide(int index) {
        if (slides.isEmpty() || index < 0 || index >= slides.size()) return;
        current = index;
        getSupportFragmentManager().beginTransaction().replace(container.getId(), slides.get(index)).commitAllowingStateLoss();
        boolean last = index == slides.size() - 1;
        next.setVisibility(last ? View.GONE : View.VISIBLE);
        done.setVisibility(last ? View.VISIBLE : View.GONE);
    }
    protected void showStatusBar(boolean show) { }
    protected void setNavBarColor(int color) { getWindow().setNavigationBarColor(getColor(color)); }
    protected void setBarColor(int color) { }
    protected void showSkipButton(boolean show) { }
    protected void setProgressButtonEnabled(boolean enabled) { }
    protected void askForPermissions(String[] permissions, int requestCode) { }
    public void onDonePressed(Fragment fragment) { }
}
