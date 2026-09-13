package cn.edu.pku.pkurunner.Utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import java.util.HashSet;
import java.util.Set;

public class PerfectExitUtil {

    /* renamed from: a, reason: collision with root package name */
    private static final Set<Activity> f7133a = new HashSet<>();

    /* renamed from: b, reason: collision with root package name */
    private static boolean f7134b = false;

    static class a implements Application.ActivityLifecycleCallbacks {
        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityPaused(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityResumed(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStarted(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStopped(Activity activity) {
        }

        a() {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityCreated(Activity activity, Bundle bundle) {
            PerfectExitUtil.f7133a.add(activity);
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityDestroyed(Activity activity) {
            PerfectExitUtil.f7133a.remove(activity);
        }
    }

    public static void exit() {
        for (Activity activity : f7133a) {
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
        }
        new Handler().postDelayed(new Runnable() { // from class: y.h
            @Override // java.lang.Runnable
            public final void run() {
                System.exit(0);
            }
        }, 100L);
    }

    public static void init(Application application) {
        if (f7134b) {
            return;
        }
        f7134b = true;
        application.registerActivityLifecycleCallbacks(new a());
    }
}
