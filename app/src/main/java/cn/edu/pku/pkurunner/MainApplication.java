package cn.edu.pku.pkurunner;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.Utils.PerfectExitUtil;
import com.instacart.library.truetime.TrueTime;
import java.io.IOException;
import org.xutils.x;

public class MainApplication extends Application {

    private static MainApplication instance;

    public static MainApplication getContext() {
        return instance;
    }

    public static /* synthetic */ void b() {
        try {
            TrueTime.build().initialize();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SharedPreferences appearance = getSharedPreferences("appearance", MODE_PRIVATE);
        String theme = appearance.getString("theme", "system");
        AppCompatDelegate.setDefaultNightMode("dark".equals(theme)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : "light".equals(theme) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        Network.init(this);
        x.Ext.init(this);
        x.Ext.setDebug(false);
        PerfectExitUtil.init(this);
        new Thread(new Runnable() {
            @Override
            public final void run() {
                MainApplication.b();
            }
        }).start();
    }
}
