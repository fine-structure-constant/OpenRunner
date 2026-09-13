package cn.edu.pku.pkurunner;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.Utils.PerfectExitUtil;
import com.instacart.library.truetime.TrueTime;
import com.tencent.bugly.crashreport.CrashReport;
import com.umeng.commonsdk.UMConfigure;
import java.io.IOException;
import org.xutils.x;

public class MainApplication extends Application {

    /* renamed from: a, reason: collision with root package name */
    private static MainApplication f6888a;

    public static MainApplication getContext() {
        return f6888a;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void b() {
        try {
            TrueTime.build().initialize();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        f6888a = this;
        SharedPreferences appearance = getSharedPreferences("appearance", MODE_PRIVATE);
        String theme = appearance.getString("theme", "system");
        AppCompatDelegate.setDefaultNightMode("dark".equals(theme)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : "light".equals(theme) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        Network.init(this);
        x.Ext.init(this);
        x.Ext.setDebug(false);
        CrashReport.initCrashReport(this, BuildConfig.BUGLY_APP_ID, false);
        UMConfigure.preInit(this, BuildConfig.UMENG_APP_KEY, "Umeng");
        UMConfigure.init(this, BuildConfig.UMENG_APP_KEY, "Umeng", 1, "");
        PerfectExitUtil.init(this);
        new Thread(new Runnable() { // from class: cn.edu.pku.pkurunner.q1
            @Override // java.lang.Runnable
            public final void run() {
                MainApplication.b();
            }
        }).start();
    }
}
