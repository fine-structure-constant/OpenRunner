package com.github.paolorotolo.appintro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import cn.edu.pku.pkurunner.R;

/** Local fragment base retaining the methods used by the recovered guide page. */
public class AppIntroBaseFragment extends Fragment {
    protected int getLayoutId() { return 0; }
    @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle state) {
        int layout = getLayoutId();
        if (layout == 0) return new View(requireContext());
        LinearLayout page = new LinearLayout(requireContext());
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(android.view.Gravity.CENTER);
        page.setBackgroundColor(Color.DKGRAY);
        inflater.inflate(layout, page, true);
        Bundle args = getArguments();
        if (args != null) {
            TextView title = page.findViewById(R.id.title);
            TextView description = page.findViewById(R.id.description);
            ImageView image = page.findViewById(R.id.image);
            if (title != null) title.setText(args.getString("title", ""));
            if (description != null) description.setText(args.getString("desc", ""));
            if (image != null && args.containsKey("drawable")) image.setImageResource(args.getInt("drawable"));
            if (args.getInt("bg_color", 0) != 0) page.setBackgroundColor(args.getInt("bg_color"));
        }
        return page;
    }
}
