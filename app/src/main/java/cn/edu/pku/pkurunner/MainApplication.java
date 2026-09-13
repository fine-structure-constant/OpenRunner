package cn.edu.pku.pkurunner;

import android.app.Application;
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
        Network.init(this);
        x.Ext.init(this);
        x.Ext.setDebug(false);
        CrashReport.initCrashReport(this, "900033802", false);
        UMConfigure.preInit(this, "632197a105844627b548e03b", "Umeng");
        UMConfigure.init(this, "632197a105844627b548e03b", "Umeng", 1, "");
        PerfectExitUtil.init(this);
        new Thread(new Runnable() { // from class: cn.edu.pku.pkurunner.q1
            @Override // java.lang.Runnable
            public final void run() {
                MainApplication.b();
            }
        }).start();
    }
}
