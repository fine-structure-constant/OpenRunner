package cn.edu.pku.pkurunner.Utils;

import android.os.Vibrator;
import cn.edu.pku.pkurunner.MainApplication;

public class VibratorUtil {
    public static void vibrate() {
        ((Vibrator) MainApplication.getContext().getSystemService("vibrator")).vibrate(new long[]{0, 500, 200, 500}, -1);
    }
}
