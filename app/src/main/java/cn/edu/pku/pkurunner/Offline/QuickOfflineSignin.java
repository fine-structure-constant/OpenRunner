package cn.edu.pku.pkurunner.Offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import cn.edu.pku.pkurunner.Offline.QuickOfflineSignin;
import cn.edu.pku.pkurunner.R;
import com.google.android.material.textfield.TextInputEditText;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class QuickOfflineSignin {
    @NonNull
    public static Observable<Pair<String, Integer>> createDialog(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe() { // from class: t.a
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                QuickOfflineSignin.f(activity, observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void e(ObservableEmitter observableEmitter, Activity activity, DialogInterface dialogInterface) {
        observableEmitter.onError(new Throwable(activity.getString(R.string.f_record_error_login_cancelled)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void f(final Activity activity, final ObservableEmitter observableEmitter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View inflate = activity.getLayoutInflater().inflate(R.layout.fragment_offline_info, (ViewGroup) null);
        final TextInputEditText textInputEditText = (TextInputEditText) inflate.findViewById(R.id.f_offline_info_tiet);
        final RadioGroup radioGroup = (RadioGroup) inflate.findViewById(R.id.f_offline_info_radiogroup);
        builder.setView(inflate).setPositiveButton(R.string.a_login_offline_button_text, new DialogInterface.OnClickListener() { // from class: t.b
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                QuickOfflineSignin.d(textInputEditText, radioGroup, observableEmitter, dialogInterface, i2);
            }
        }).setCancelable(true).setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: t.c
            @Override // android.content.DialogInterface.OnCancelListener
            public final void onCancel(DialogInterface dialogInterface) {
                QuickOfflineSignin.e(observableEmitter, activity, dialogInterface);
            }
        }).create().show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void d(TextInputEditText textInputEditText, RadioGroup radioGroup, ObservableEmitter observableEmitter, DialogInterface dialogInterface, int i2) {
        String obj = textInputEditText.getText().toString();
        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
        int i3 = -1;
        if (checkedRadioButtonId != -1) {
            if (checkedRadioButtonId == R.id.f_offline_info_radio_male) {
                i3 = 0;
            } else {
                i3 = 1;
            }
        }
        observableEmitter.onNext(new Pair(obj, Integer.valueOf(i3)));
    }
}
