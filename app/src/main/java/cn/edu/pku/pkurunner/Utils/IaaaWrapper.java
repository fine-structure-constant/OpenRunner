package cn.edu.pku.pkurunner.Utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import androidx.activity.result.ActivityResult;
import cn.edu.pku.pkuiaaa_android.IAAA_Authen;
import cn.edu.pku.pkurunner.R;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class IaaaWrapper {
    public static final String EXTRA_iAAA_APPID = "iAAA_APPID";
    public static final String EXTRA_iAAA_RESULT = "iAAA_RESULT";
    public static final String EXTRA_iAAA_TOKEN = "iAAA_TOKEN";
    public static final String EXTRA_iAAA_UID = "iAAA_UID";
    public static final String RESULT_CANCEL = "cancel";
    public static final String RESULT_SUCCESS = "success";

    public static Intent createIaaaIntent(Context context) {
        Intent intent = new Intent(context, (Class<?>) IAAA_Authen.class);
        intent.putExtra(EXTRA_iAAA_APPID, "PKU_Runner");
        return intent;
    }

    public static class IAAAException extends Exception {
        public IAAAException(String str) {
            super(str);
        }
    }

    public static Observable<Pair<String, String>> HandleIaaaResult(final Context context, final ActivityResult activityResult) {
        return Observable.create(new ObservableOnSubscribe() { // from class: y.e
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                IaaaWrapper.b(context, activityResult, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void b(Context context, ActivityResult activityResult, ObservableEmitter observableEmitter) {
        if (activityResult.getResultCode() != -1) {
            observableEmitter.onError(new IAAAException(context.getString(R.string.a_login_fail)));
            return;
        }
        Intent data = activityResult.getData();
        if (data == null) {
            observableEmitter.onError(new IAAAException(context.getString(R.string.a_login_fail)));
            return;
        }
        Bundle extras = data.getExtras();
        if (RESULT_CANCEL.equals(extras.getString(EXTRA_iAAA_RESULT))) {
            observableEmitter.onError(new IAAAException(context.getString(R.string.a_login_canceled)));
        } else {
            observableEmitter.onNext(new Pair(extras.getString(EXTRA_iAAA_UID), extras.getString(EXTRA_iAAA_TOKEN)));
        }
    }
}
