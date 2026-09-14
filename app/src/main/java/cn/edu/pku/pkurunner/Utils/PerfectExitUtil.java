package cn.edu.pku.pkurunner.Utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.HashSet;
import java.util.Set;

public class PerfectExitUtil {

    private static final Set<Activity> activitySet = new HashSet<>();

    private static boolean exiting = false;

    static class ActivityLifecycleTracker implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        ActivityLifecycleTracker() {
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            PerfectExitUtil.activitySet.add(activity);
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            PerfectExitUtil.activitySet.remove(activity);
        }
    }

    public static void exit() {
        for (Activity activity : activitySet) {
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public final void run() {
                System.exit(0);
            }
        }, 100L);
    }

    public static void init(Application application) {
        if (exiting) {
            return;
        }
        exiting = true;
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleTracker());
    }
}
