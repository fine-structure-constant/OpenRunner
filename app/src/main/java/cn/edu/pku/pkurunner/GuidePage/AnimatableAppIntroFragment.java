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

    /* renamed from: d, reason: collision with root package name */
    private ImageView f6864d;

    public static AnimatableAppIntroFragment newInstance(CharSequence charSequence, CharSequence charSequence2, @DrawableRes int i2, @ColorInt int i3) {
        return newInstance(charSequence, charSequence2, i2, i3, 0, 0);
    }

    @Override // com.github.paolorotolo.appintro.AppIntroBaseFragment
    protected int getLayoutId() {
        return R.layout.fragment_intro_content;
    }

    public static AnimatableAppIntroFragment newInstance(CharSequence charSequence, CharSequence charSequence2, @DrawableRes int i2, @ColorInt int i3, @ColorInt int i4, @ColorInt int i5) {
        AnimatableAppIntroFragment animatableAppIntroFragment = new AnimatableAppIntroFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", charSequence.toString());
        bundle.putString("title_typeface", null);
        bundle.putString("desc", charSequence2.toString());
        bundle.putString("desc_typeface", null);
        bundle.putInt("drawable", i2);
        bundle.putInt("bg_color", i3);
        bundle.putInt("title_color", i4);
        bundle.putInt("desc_color", i5);
        animatableAppIntroFragment.setArguments(bundle);
        return animatableAppIntroFragment;
    }

    @Override // com.github.paolorotolo.appintro.AppIntroBaseFragment, androidx.fragment.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        View onCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        this.f6864d = (ImageView) onCreateView.findViewById(R.id.image);
        return onCreateView;
    }

    @Override // androidx.fragment.app.Fragment
    public void onStart() {
        super.onStart();
        if (this.f6864d == null) {
            return;
        }
        Object drawable = this.f6864d.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }
    }

    public static AnimatableAppIntroFragment newInstance(CharSequence charSequence, String str, CharSequence charSequence2, String str2, @DrawableRes int i2, @ColorInt int i3) {
        return newInstance(charSequence, str, charSequence2, str2, i2, i3, 0, 0);
    }

    public static AnimatableAppIntroFragment newInstance(CharSequence charSequence, String str, CharSequence charSequence2, String str2, @DrawableRes int i2, @ColorInt int i3, @ColorInt int i4, @ColorInt int i5) {
        AnimatableAppIntroFragment animatableAppIntroFragment = new AnimatableAppIntroFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", charSequence.toString());
        bundle.putString("title_typeface", str);
        bundle.putString("desc", charSequence2.toString());
        bundle.putString("desc_typeface", str2);
        bundle.putInt("drawable", i2);
        bundle.putInt("bg_color", i3);
        bundle.putInt("title_color", i4);
        bundle.putInt("desc_color", i5);
        animatableAppIntroFragment.setArguments(bundle);
        return animatableAppIntroFragment;
    }
}
