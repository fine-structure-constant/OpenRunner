package cn.edu.pku.pkurunner.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Utils.TokenInvalidNotice;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class TokenInvalidNotice {
    public static /* synthetic */ void c(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int index) {
        observableEmitter.onNext(Boolean.TRUE);
    }

    public static /* synthetic */ void d(Context context, final ObservableEmitter observableEmitter) {
        new AlertDialog.Builder(context).setTitle(R.string.d_token_invalid_dialog_title).setMessage(R.string.d_token_invalid_dialog_content).setPositiveButton(R.string.d_token_invalid_dialog_positive_button, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index) {
                TokenInvalidNotice.c(observableEmitter, dialogInterface, index);
            }
        }).setCancelable(false).create().show();
    }

    public static Observable<Boolean> showTokenInvalidDialog(final Context context) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                TokenInvalidNotice.d(context, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }
}
