package cn.edu.pku.pkurunner.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import cn.edu.pku.pkurunner.Config;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Utils.ClientUpdateNotice;
import cn.edu.pku.pkurunner.Utils.PerfectExitUtil;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class ClientUpdateNotice {
    public static void downloadLatestVersion(Context context, boolean z2) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(Uri.parse(z2 ? Config.OFFLINE_APK_URL : Config.APK_URL));
        context.startActivity(intent);
    }

    public static /* synthetic */ void f(final Context context, ObservableEmitter observableEmitter) {
        new AlertDialog.Builder(context).setTitle(R.string.a_login_version_dialog_title).setMessage(R.string.a_login_version_dialog_content).setPositiveButton(R.string.a_login_version_dialog_positive_button, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index) {
                ClientUpdateNotice.downloadLatestVersion(context, false);
            }
        }).setNegativeButton(R.string.a_login_version_dialog_negative_button, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index) {
                PerfectExitUtil.exit();
            }
        }).setCancelable(false).create().show();
    }

    public static Observable<Boolean> showVersionLowDialog(final Context context) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                ClientUpdateNotice.f(context, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }
}
