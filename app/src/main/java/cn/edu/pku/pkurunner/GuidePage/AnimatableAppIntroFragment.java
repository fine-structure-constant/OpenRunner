package cn.edu.pku.pkurunner.GuidePage;

import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import com.github.paolorotolo.appintro.AppIntroBaseFragment;
import cn.edu.pku.pkurunner.R;

public class AnimatableAppIntroFragment extends AppIntroBaseFragment {

    private ImageView imageView;

    public static AnimatableAppIntroFragment newInstance(CharSequence charSequence, CharSequence charSequence2, @DrawableRes int index, @ColorInt int index2) {
        return newInstance(charSequence, charSequence2, index, index2, 0, 0);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_intro_content;
    }

    public static AnimatableAppIntroFragment newInstance(CharSequence charSequence, CharSequence charSequence2, @DrawableRes int index, @ColorInt int index2, @ColorInt int index3, @ColorInt int i5) {
        AnimatableAppIntroFragment animatableAppIntroFragment = new AnimatableAppIntroFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", charSequence.toString());
        bundle.putString("title_typeface", null);
        bundle.putString("desc", charSequence2.toString());
        bundle.putString("desc_typeface", null);
        bundle.putInt("drawable", index);
        bundle.putInt("bg_color", index2);
        bundle.putInt("title_color", index3);
        bundle.putInt("desc_color", i5);
        animatableAppIntroFragment.setArguments(bundle);
        return animatableAppIntroFragment;
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        View onCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        this.imageView = (ImageView) onCreateView.findViewById(R.id.image);
        return onCreateView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.imageView == null) {
            return;
        }
        Object drawable = this.imageView.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }
    }

    public static AnimatableAppIntroFragment newInstance(CharSequence charSequence, String str, CharSequence charSequence2, String str2, @DrawableRes int index, @ColorInt int index2) {
        return newInstance(charSequence, str, charSequence2, str2, index, index2, 0, 0);
    }

    public static AnimatableAppIntroFragment newInstance(CharSequence charSequence, String str, CharSequence charSequence2, String str2, @DrawableRes int index, @ColorInt int index2, @ColorInt int index3, @ColorInt int i5) {
        AnimatableAppIntroFragment animatableAppIntroFragment = new AnimatableAppIntroFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", charSequence.toString());
        bundle.putString("title_typeface", str);
        bundle.putString("desc", charSequence2.toString());
        bundle.putString("desc_typeface", str2);
        bundle.putInt("drawable", index);
        bundle.putInt("bg_color", index2);
        bundle.putInt("title_color", index3);
        bundle.putInt("desc_color", i5);
        animatableAppIntroFragment.setArguments(bundle);
        return animatableAppIntroFragment;
    }
}
