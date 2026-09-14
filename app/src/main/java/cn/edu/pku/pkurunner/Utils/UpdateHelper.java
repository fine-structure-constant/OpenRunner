package cn.edu.pku.pkurunner.Utils;

import android.app.Activity;
import android.text.format.Formatter;
import cn.edu.pku.pkurunner.Utils.UpdateHelper;
import com.azhon.appupdate.config.Constant;
import com.azhon.appupdate.manager.DownloadManager;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class UpdateHelper {
    public static /* synthetic */ void b(Activity activity, String str, boolean z2, int index, String str2, String str3, String str4, Long l2, ObservableEmitter observableEmitter) {
        DownloadManager.Builder jumpInstallPage = new DownloadManager.Builder(activity).apkUrl(str).forcedUpgrade(z2).apkName("pkurunner_" + index + Constant.APK_SUFFIX).smallIcon(activity.getApplicationInfo().icon).apkVersionCode(Integer.MAX_VALUE).apkVersionName(str2).apkDescription(str3).showNotification(true).jumpInstallPage(true);
        if (str4 != null) {
            jumpInstallPage.apkMD5(str4);
        }
        if (l2 != null) {
            jumpInstallPage.apkSize(Formatter.formatFileSize(activity, l2.longValue()));
        }
        jumpInstallPage.build().download();
    }

    public static Observable<Boolean> showVersionLowDialog(final Activity activity, final int index, final String str, final String str2, final String str3, final String str4, final Long l2, final boolean z2) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                UpdateHelper.b(activity, str3, z2, index, str, str2, str4, l2, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }
}
