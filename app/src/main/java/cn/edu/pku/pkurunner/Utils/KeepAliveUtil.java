package cn.edu.pku.pkurunner.Utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Utils.KeepAliveUtil;
import com.bumptech.glide.Glide;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;
import com.permissionx.guolindev.request.PermissionBuilder;
import com.permissionx.guolindev.request.RequestBackgroundLocationPermission;
import com.umeng.analytics.pro.am;
import java.util.List;

public class KeepAliveUtil {

    public static class WakeAndWifiLocker {
        public static void lock(Context context) {
        }

        public static void release() {
        }
    }

    static class VendorAutostartGuide {

        interface GoToSettingsCallback {
            void onConfirmed();
        }

        public static boolean j() {
            return Build.MANUFACTURER.matches("^(Xiaomi|vivo|HUAWEI)$");
        }

        public static boolean k(Context context) {
            return context.getSharedPreferences("ContinuousPositioning", 0).getBoolean("BatterySaverDialogWontShow2022", false);
        }

        public static /* synthetic */ void l(Intent intent, Context context, Activity activity) {
            ComponentName componentName = new ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity");
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, context.getPackageName());
            intent.putExtra("package_label", context.getResources().getString(R.string.app_name));
            intent.setComponent(componentName);
            activity.startActivity(intent);
        }

        public static /* synthetic */ void n(Intent intent, Activity activity) {
            intent.setComponent(new ComponentName("com.iqoo.powersaving", "com.iqoo.powersaving.PowerSavingManagerActivity"));
            activity.startActivity(intent);
        }

        public static void q(Context context) {
            SharedPreferences.Editor edit = context.getSharedPreferences("ContinuousPositioning", 0).edit();
            edit.putBoolean("BatterySaverDialogWontShow2022", true);
            edit.apply();
        }

        private static void r(final Activity activity, String str, final GoToSettingsCallback goToSettingsCallback) {
            CountDownDialogUtil.showDialog(activity, "跑步轨迹优化-步骤2", str, null, "去设置", new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int index) {
                    KeepAliveUtil.VendorAutostartGuide.o(goToSettingsCallback, activity, dialogInterface, index);
                }
            }, "永久忽略", new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int index) {
                    KeepAliveUtil.VendorAutostartGuide.q(activity);
                }
            }, 4);
        }

        public static void i(final Activity activity) {
            final Context applicationContext;
            final Intent intent;
            applicationContext = activity.getApplicationContext();
            intent = new Intent();
            intent.addFlags(268435456);
            String str = Build.MANUFACTURER;
            str.hashCode();
            switch (str) {
                case "Xiaomi":
                    r(activity, "为减少跑步轨迹的问题，请继续在下一界面中点击【无限制】", new GoToSettingsCallback() {
                        @Override
                        public final void onConfirmed() {
                            KeepAliveUtil.VendorAutostartGuide.l(intent, applicationContext, activity);
                        }
                    });
                    break;
                case "vivo":
            r(activity, "请在下一界面中点击【后台高耗电】或【后台耗电管理】，然后找到" + applicationContext.getResources().getString(R.string.app_name) + "，开启开关或选择“允许后台高耗电”即可", new GoToSettingsCallback() {
                        @Override
                        public final void onConfirmed() {
                            KeepAliveUtil.VendorAutostartGuide.n(intent, activity);
                        }
                    });
                    break;
                case "HUAWEI":
                    r(activity, "点击 应用启动管理 → PKURunner → 关闭 → 在弹出的三个选项中，开启“允许后台活动”、“允许自启动“", new GoToSettingsCallback() {
                        @Override
                        public final void onConfirmed() {
                            KeepAliveUtil.VendorAutostartGuide.m(activity);
                        }
                    });
                    break;
            }
        }

        public static /* synthetic */ void m(Activity activity) {
            activity.startActivity(activity.getPackageManager().getLaunchIntentForPackage("com.huawei.systemmanager"));
        }

        public static /* synthetic */ void o(GoToSettingsCallback goToSettingsCallback, Activity activity, DialogInterface dialogInterface, int index) {
            try {
                goToSettingsCallback.onConfirmed();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            q(activity);
        }
    }

    static class ContinuousPositioningPrefs {
        private static boolean d(Context context) {
            return context.getSharedPreferences("ContinuousPositioning", 0).getBoolean("ContinuousPositioningDenied", false);
        }

        public static /* synthetic */ void f(ForwardScope forwardScope, List list) {
            String str = Build.MANUFACTURER;
            str.hashCode();
            switch (str) {
                case "Xiaomi":
                    forwardScope.showForwardToSettingsDialog(list, "为减少熄屏后轨迹问题，需点击【权限管理】→【定位】→【始终允许】", "去设置");
                    break;
                case "OPPO":
                    forwardScope.showForwardToSettingsDialog(list, "为减少熄屏后轨迹问题，需点击【应用权限】→【位置信息】→【始终允许】", "去设置");
                    break;
                case "HUAWEI":
                    forwardScope.showForwardToSettingsDialog(list, "为减少熄屏后轨迹问题，需点击【权限】→【位置信息】→【始终允许】", "去设置");
                    break;
                default:
                    forwardScope.showForwardToSettingsDialog(list, "为减少熄屏后轨迹问题，需点击【权限管理】→【定位】→【始终允许】", "去设置");
                    break;
            }
        }

        public static /* synthetic */ void g(FragmentActivity fragmentActivity, boolean z2, List list, List list2) {
            if (z2) {
                return;
            }
            i(fragmentActivity);
        }

        public static boolean h(final FragmentActivity fragmentActivity) {
            if (Build.VERSION.SDK_INT < 29 || PermissionX.isGranted(fragmentActivity, RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)) {
                return true;
            }
            PermissionBuilder permissions = PermissionX.init(fragmentActivity).permissions("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION);
            if (!d(fragmentActivity)) {
                permissions = permissions.onExplainRequestReason(new ExplainReasonCallback() {
                    @Override
                    public final void onExplainReason(ExplainScope explainScope, List list) {
                        explainScope.showRequestReasonDialog(list, "为减少轨迹问题，接下来请点击【始终允许】", "去设置");
                    }
                });
            }
            permissions.onForwardToSettings(new ForwardToSettingsCallback() {
                @Override
                public final void onForwardToSettings(ForwardScope forwardScope, List list) {
                    KeepAliveUtil.ContinuousPositioningPrefs.f(forwardScope, list);
                }
            }).request(new RequestCallback() {
                @Override
                public final void onResult(boolean z2, List list, List list2) {
                    KeepAliveUtil.ContinuousPositioningPrefs.g(fragmentActivity, z2, list, list2);
                }
            });
            return false;
        }

        private static void i(Context context) {
            SharedPreferences.Editor edit = context.getSharedPreferences("ContinuousPositioning", 0).edit();
            edit.putBoolean("ContinuousPositioningDenied", true);
            edit.apply();
        }
    }

    static class TaskLockDialogPrefs {
        public static boolean d(Context context) {
            return context.getSharedPreferences("ContinuousPositioning", 0).getBoolean("TaskLockDialogWontShow2022", false);
        }

        public static void f(Context context) {
            SharedPreferences.Editor edit = context.getSharedPreferences("ContinuousPositioning", 0).edit();
            edit.putBoolean("TaskLockDialogWontShow2022", true);
            edit.apply();
        }

        /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
        public static void g(final Activity activity) {
            View inflate = LayoutInflater.from(activity).inflate(R.layout.dialog_gif, (ViewGroup) null);
            ImageView imageView = (ImageView) inflate.findViewById(R.id.dialoggif_webp);
            TextView textView = (TextView) inflate.findViewById(R.id.dialoggif_text);
            String str = Build.MANUFACTURER;
            str.hashCode();
            char character = 65535;
            switch (str.hashCode()) {
                case -1675632421:
                    if (str.equals("Xiaomi")) {
                        character = 0;
                        break;
                    }
                    break;
                case 2432928:
                    if (str.equals("OPPO")) {
                        character = 1;
                        break;
                    }
                    break;
                case 3620012:
                    if (str.equals("vivo")) {
                        character = 2;
                        break;
                    }
                    break;
                case 2141820391:
                    if (str.equals("HUAWEI")) {
                        character = 3;
                        break;
                    }
                    break;
            }
            int index = R.drawable.lockapp_xiaomi;
            switch (character) {
                case 0:
                    textView.setText("进入任务切换界面 → 长按app视图 → 再点击锁图标");
                    break;
                case 1:
                    textView.setText("进入任务切换界面 → 点击app右上角图标 → 点击“锁定”");
                    index = R.drawable.lockapp_oppo;
                    break;
                case 2:
                    textView.setText("进入任务切换界面 → 点击app左上角图标 → 点击“锁定”");
                    index = R.drawable.lockapp_vivo;
                    break;
                case 3:
                    textView.setText("进入任务切换界面 → 将当前app向下拖动即可锁定");
                    index = R.drawable.lockapp_huawei;
                    break;
            }
            Glide.with(activity).asGif().load(Integer.valueOf(index)).into(imageView);
            CountDownDialogUtil.showDialog(activity, "跑步轨迹优化-给任务加锁※", null, inflate, "我已按动画加锁完毕", new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int index2) {
                    KeepAliveUtil.TaskLockDialogPrefs.f(activity);
                }
            }, null, null, 12);
        }
    }

    private static boolean b(Context context) {
        PowerManager powerManager;
        boolean isIgnoringBatteryOptimizations;
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        Intent intent = new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        if (intent.resolveActivity(context.getPackageManager()) == null || (powerManager = (PowerManager) context.getSystemService("power")) == null) {
            return true;
        }
        isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        return isIgnoringBatteryOptimizations;
    }

    public static void d(Context context) {
        try {
            Intent intent = new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public static boolean check(final FragmentActivity fragmentActivity) {
        Context applicationContext = fragmentActivity.getApplicationContext();
        if (!b(applicationContext)) {
            new AlertDialog.Builder(fragmentActivity).setTitle("跑步轨迹优化-电池白名单").setMessage("为减少跑步轨迹的问题，请在弹出的界面中点击【允许】或【不限制】，将" + applicationContext.getString(R.string.app_name) + "加入电池白名单").setPositiveButton("好，我知道了", new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int index) {
                    KeepAliveUtil.d(fragmentActivity);
                }
            }).setCancelable(false).show();
            return false;
        }
        if (!VendorAutostartGuide.k(applicationContext) && VendorAutostartGuide.j()) {
            VendorAutostartGuide.i(fragmentActivity);
            return false;
        }
        if (!TaskLockDialogPrefs.d(applicationContext)) {
            TaskLockDialogPrefs.g(fragmentActivity);
            return false;
        }
        if (!ContinuousPositioningPrefs.h(fragmentActivity)) {
            return false;
        }
        return true;
    }
}
