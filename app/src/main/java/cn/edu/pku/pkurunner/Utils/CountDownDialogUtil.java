package cn.edu.pku.pkurunner.Utils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

    class ButtonCountdownTimer extends CountDownTimer {

        final /* synthetic */ Button button;

        final /* synthetic */ CharSequence originalText;

        final /* synthetic */ String finishText;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        ButtonCountdownTimer(long j2, long j3, Button button, CharSequence charSequence, String str) {
            super(j2, j3);
            this.button = button;
            this.originalText = charSequence;
            this.finishText = str;
        }

        @Override
        public void onFinish() {
            this.button.setEnabled(true);
            this.button.setText(this.finishText);
        }

        @Override
        public void onTick(long j2) {
            this.button.setEnabled(false);
            this.button.setText(String.format(Locale.getDefault(), "%s (%d)", this.originalText, Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(j2) + 1)));
        }
    }

    public static /* synthetic */ void b(AlertDialog alertDialog, int index, String str, DialogInterface dialogInterface) {
        Button button = alertDialog.getButton(-1);
        new CountDownDialogUtil().new ButtonCountdownTimer(index * 1000, 1000L, button, button.getText(), str).start();
    }

    public static void showDialog(Activity activity, String str, @Nullable String str2, @Nullable View view, final String str3, DialogInterface.OnClickListener onClickListener, @Nullable String str4, @Nullable DialogInterface.OnClickListener onClickListener2, final int index) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(activity);
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
        create.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public final void onShow(DialogInterface dialogInterface) {
                CountDownDialogUtil.b(create, index, str3, dialogInterface);
            }
        });
        create.show();
    }
}
