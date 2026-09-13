package cn.edu.pku.pkurunner.GuidePage;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import cn.edu.pku.pkurunner.LoginActivity;
import cn.edu.pku.pkurunner.R;
import com.github.paolorotolo.appintro.AppIntro;

public class IntroActivity extends AppIntro {
    public static final String GuidePreferencesKey = "guided";
    public static final String GuidePreferencesName = "GuideActivity";

    public void enterApp() {
        getSharedPreferences(GuidePreferencesName, 0).edit().putBoolean(GuidePreferencesKey, true).apply();
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("jumpToLogin")) {
            Intent intent = new Intent(this, (Class<?>) LoginActivity.class);
            intent.setFlags(67108864);
            startActivity(intent);
        }
        finish();
    }

    @Override // com.github.paolorotolo.appintro.AppIntroBase, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        int color = getResources().getColor(R.color.grey_700);
        String[] stringArray = getResources().getStringArray(R.array.a_intro_title);
        String[] stringArray2 = getResources().getStringArray(R.array.a_intro_content);
        int[] iArr = {R.drawable.guide_running, R.drawable.guide_calendar, R.drawable.guide_permission, R.drawable.anim_icon};
        int length = stringArray.length;
        for (int i2 = 0; i2 < length; i2++) {
            addSlide(AnimatableAppIntroFragment.newInstance(stringArray[i2], stringArray2[i2], iArr[i2], color));
        }
        showStatusBar(false);
        setNavBarColor(R.color.grey_700);
        setBarColor(getResources().getColor(R.color.grey_800));
        showSkipButton(false);
        setProgressButtonEnabled(true);
        askForPermissions(new String[]{"android.permission.READ_PHONE_STATE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.ACCESS_FINE_LOCATION"}, 3);
    }

    @Override // com.github.paolorotolo.appintro.AppIntroBase
    public void onDonePressed(Fragment fragment) {
        super.onDonePressed(fragment);
        enterApp();
    }
}
