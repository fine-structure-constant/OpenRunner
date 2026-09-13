package cn.edu.pku.pkurunner.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import cn.edu.pku.pkurunner.MainActivity;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Utils.KeepAliveUtil;
import org.xutils.common.util.LogUtil;

public class NotificationDisplayService extends Service {
    @Override // android.app.Service
    public void onDestroy() {
        stopForeground(true);
        KeepAliveUtil.WakeAndWifiLocker.release();
        super.onDestroy();
        LogUtil.d("onDestroy");
    }

    private String a() {
        int color;
        NotificationChannel a2 = new NotificationChannel("PKURunner", "PKURunner Foreground Service", NotificationManager.IMPORTANCE_LOW);
        color = getResources().getColor(R.color.red_500, getTheme());
        a2.setLightColor(color);
        a2.setLockscreenVisibility(0);
        NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(a2);
        }
        return "PKURunner";
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        LogUtil.d("onBind()");
        return null;
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i2, int i3) {
        Notification.Builder builder;
        LogUtil.d("onStartCommand()");
        if (Build.VERSION.SDK_INT >= 26) {
            String a2 = a();
            builder = new Notification.Builder(getApplicationContext(), a2);
        } else {
            builder = new Notification.Builder(getApplicationContext());
        }
        builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, (Class<?>) MainActivity.class), 0)).setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)).setContentTitle(getString(R.string.s_running)).setSmallIcon(R.drawable.launcher_notification).setContentText(getString(R.string.s_return_to_main)).setWhen(System.currentTimeMillis());
        Notification build = builder.build();
        build.defaults = 1;
        KeepAliveUtil.WakeAndWifiLocker.lock(this);
        startForeground(110, build);
        return 1;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        LogUtil.d("onCreate()");
    }
}
