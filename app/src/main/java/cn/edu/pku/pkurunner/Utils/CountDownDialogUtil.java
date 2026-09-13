package cn.edu.pku.pkurunner.Utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import cn.edu.pku.pkurunner.Utils.CountDownDialogUtil;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CountDownDialogUtil {

    class a extends CountDownTimer {

        /* renamed from: a, reason: collision with root package name */
        final /* synthetic */ Button f7130a;

        /* renamed from: b, reason: collision with root package name */
        final /* synthetic */ CharSequence f7131b;

        /* renamed from: c, reason: collision with root package name */
        final /* synthetic */ String f7132c;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        a(long j2, long j3, Button button, CharSequence charSequence, String str) {
            super(j2, j3);
            this.f7130a = button;
            this.f7131b = charSequence;
            this.f7132c = str;
        }

        @Override // android.os.CountDownTimer
        public void onFinish() {
            this.f7130a.setEnabled(true);
            this.f7130a.setText(this.f7132c);
        }

        @Override // android.os.CountDownTimer
        public void onTick(long j2) {
            this.f7130a.setEnabled(false);
            this.f7130a.setText(String.format(Locale.getDefault(), "%s (%d)", this.f7131b, Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(j2) + 1)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void b(AlertDialog alertDialog, int i2, String str, DialogInterface dialogInterface) {
        Button button = alertDialog.getButton(-1);
        new CountDownDialogUtil().new a(i2 * 1000, 1000L, button, button.getText(), str).start();
    }

    public static void showDialog(Activity activity, String str, @Nullable String str2, @Nullable View view, final String str3, DialogInterface.OnClickListener onClickListener, @Nullable String str4, @Nullable DialogInterface.OnClickListener onClickListener2, final int i2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (view != null) {
            builder.setView(view);
        }
        if (str4 != null && onClickListener2 != null) {
            builder.setNeutralButton(str4, onClickListener2);
        }
        if (str2 != null) {
            builder.setMessage(str2);
        }
        final AlertDialog create = builder.setTitle(str).setPositiveButton(str3, onClickListener).setCancelable(false).create();
        create.setOnShowListener(new DialogInterface.OnShowListener() { // from class: y.d
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                CountDownDialogUtil.b(create, i2, str3, dialogInterface);
            }
        });
        create.show();
    }
}
