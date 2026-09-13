package cn.edu.pku.pkurunner.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import androidx.fragment.app.Fragment;
import cn.edu.pku.pkuiaaa_android.IAAA_Authen;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Utils.IaaaWrapper;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class IaaaWrapper {
    public static final int ACTIVITY_iAAA = 1651;
    public static final String EXTRA_iAAA_APPID = "iAAA_APPID";
    public static final String EXTRA_iAAA_RESULT = "iAAA_RESULT";
    public static final String EXTRA_iAAA_TOKEN = "iAAA_TOKEN";
    public static final String EXTRA_iAAA_UID = "iAAA_UID";
    public static final String RESULT_CANCEL = "cancel";
    public static final String RESULT_SUCCESS = "success";

    public static void LaunchIaaaLogin(Activity activity) {
        Intent intent = new Intent(activity, (Class<?>) IAAA_Authen.class);
        intent.putExtra(EXTRA_iAAA_APPID, "PKU_Runner");
        activity.startActivityForResult(intent, ACTIVITY_iAAA);
    }

    public static class IAAAException extends Exception {
        public IAAAException(String str) {
            super(str);
        }
    }

    public static Observable<Pair<String, String>> HandleIaaaResult(final Context context, final int i2, final int i3, final Intent intent) {
        return Observable.create(new ObservableOnSubscribe() { // from class: y.e
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                IaaaWrapper.b(i2, i3, intent, context, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void b(int i2, int i3, Intent intent, Context context, ObservableEmitter observableEmitter) {
        if (i2 == 1651) {
            if (i3 != -1) {
                observableEmitter.onError(new IAAAException(context.getString(R.string.a_login_fail)));
                return;
            }
            Bundle extras = intent.getExtras();
            if (RESULT_CANCEL.equals(extras.getString(EXTRA_iAAA_RESULT))) {
                observableEmitter.onError(new IAAAException(context.getString(R.string.a_login_canceled)));
            } else {
                observableEmitter.onNext(new Pair(extras.getString(EXTRA_iAAA_UID), extras.getString(EXTRA_iAAA_TOKEN)));
            }
        }
    }

    public static void LaunchIaaaLogin(Fragment fragment) {
        Intent intent = new Intent(fragment.getActivity(), (Class<?>) IAAA_Authen.class);
        intent.putExtra(EXTRA_iAAA_APPID, "PKU_Runner");
        fragment.startActivityForResult(intent, ACTIVITY_iAAA);
    }
}
