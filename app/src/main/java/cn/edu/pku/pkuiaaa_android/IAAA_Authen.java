package cn.edu.pku.pkuiaaa_android;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.app.Activity;
import cn.edu.pku.pkurunner.View.OrLoadingDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Utils.IaaaWrapper;
import java.util.Timer;
import java.util.TimerTask;

public class IAAA_Authen extends AppCompatActivity {

    static String appId;

    static Timer countdownTimer = new Timer();

    int countdownSeconds = 0;

    boolean loginSucceeded = false;

    boolean secondFactorRequired = false;

    final int RETRY_INTERVAL_MILLIS = 2000;

    final int COUNTDOWN_SECONDS = 60;

    boolean smsMode = false;

    boolean otpMode = false;

    TextView loginButton;

    TextView cancelButton;

    TextView sendCodeButton;

    EditText usernameEdit;

    EditText passwordEdit;

    EditText verificationCodeEdit;

    LinearLayout verificationCodeContainer;

    String username;

    String password;

    String verificationCode;

    final class ShowSendCodeWaitRunnable implements Runnable {
        ShowSendCodeWaitRunnable() {
        }

        @Override
        public final void run() {
            IAAA_Authen.this.sendCodeButton.setEnabled(false);
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            iAAA_Authen.sendCodeButton.setTextColor(ContextCompat.getColor(iAAA_Authen.getApplicationContext(), R.color.colorIPGWGray));
            IAAA_Authen.this.sendCodeButton.setText(IAAA_Authen.this.getResources().getString(R.string.sendcode_wait) + IAAA_Authen.this.countdownSeconds);
        }
    }

    final class SendCodeCountdownTask extends TimerTask {
        SendCodeCountdownTask() {
        }

        @Override
        public final void run() {
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            iAAA_Authen.g(iAAA_Authen.sendCodeButton);
        }
    }

    final class LoginClickListener implements View.OnClickListener {
        LoginClickListener() {
        }

        @Override
        public final void onClick(View view) {
            IAAA_Authen.a(IAAA_Authen.this);
            String username = IAAA_Authen.this.usernameEdit.getText().toString().trim();
            String password = IAAA_Authen.this.passwordEdit.getText().toString().trim();
            View root = IAAA_Authen.this.getWindow().getDecorView().getRootView();
            if (username.length() == 0 || password.length() == 0) {
                IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(R.string.enter_username_passwd)).show();
                return;
            }
            if (username.length() < 2 || password.length() < 8) {
                IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(R.string.wrong_username_passwd)).show();
                return;
            }
            if (!username.equals(IAAA_Authen.this.username)) {
                IAAA_Authen.this.secondFactorRequired = false;
                IAAA_Authen.this.smsMode = false;
                IAAA_Authen.this.otpMode = false;
            }
            String code = "";
            if (IAAA_Authen.this.smsMode || IAAA_Authen.this.otpMode) {
                code = IAAA_Authen.this.verificationCodeEdit.getText().toString().trim();
                if (code.length() == 0) {
                    int message = IAAA_Authen.this.smsMode ? R.string.enter_msgcode : R.string.enter_otpcode;
                    IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(message)).show();
                    return;
                }
                if (code.length() < 4 || code.length() > 6) {
                    int message = IAAA_Authen.this.smsMode ? R.string.wrong_msgcode : R.string.wrong_otpcode;
                    IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(message)).show();
                    return;
                }
                try {
                    Integer.parseInt(code);
                } catch (Exception ignored) {
                    int message = IAAA_Authen.this.smsMode ? R.string.wrong_msgcode : R.string.wrong_otpcode;
                    IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(message)).show();
                    return;
                }
            }
            IAAA_Authen.this.username = username;
            IAAA_Authen.this.password = password;
            IAAA_Authen.this.verificationCode = code;
            IAAA_Authen.this.h(false);
            OrLoadingDialog progress = IAAA_Authen.i(IAAA_Authen.this);
            progress.show();
            try {
                String mode = IAAA_Authen.this.secondFactorRequired
                        ? (IAAA_Authen.this.smsMode ? "SMS" : "OTP")
                        : IaaaApi.getAuthMode(username, IAAA_Authen.appId);
                if (mode == null) mode = "";
                if (!IAAA_Authen.this.secondFactorRequired && mode.length() != 0) {
                    IAAA_Authen.this.smsMode = "SMS".equals(mode);
                    IAAA_Authen.this.otpMode = !IAAA_Authen.this.smsMode;
                    IAAA_Authen.this.secondFactorRequired = true;
                    IAAA_Authen.this.h(true);
                    progress.dismiss();
                    IAAA_Authen.j(root, "请输入验证码后再次登录").show();
                    return;
                }
                String token = IaaaApi.login(username, password, code, IAAA_Authen.appId, mode);
                Intent result = new Intent();
                result.putExtra(IaaaWrapper.EXTRA_iAAA_RESULT, IaaaWrapper.RESULT_SUCCESS);
                result.putExtra(IaaaWrapper.EXTRA_iAAA_UID, username);
                result.putExtra(IaaaWrapper.EXTRA_iAAA_TOKEN, token);
                IAAA_Authen.this.setResult(Activity.RESULT_OK, result);
                progress.dismiss();
                IAAA_Authen.this.h(true);
                IAAA_Authen.this.finish();
            } catch (Exception error) {
                progress.dismiss();
                IAAA_Authen.this.h(true);
                String message = error.getMessage();
                if (message == null || message.length() == 0) message = "登录请求失败";
                IAAA_Authen.f(root, message).show();
            }
        }
    }

    final class CancelClickListener implements View.OnClickListener {
        CancelClickListener() {
        }

        @Override
        public final void onClick(View view) {
            IAAA_Authen.a(IAAA_Authen.this);
            Intent intent = new Intent();
            intent.putExtra(IaaaWrapper.EXTRA_iAAA_RESULT, IaaaWrapper.RESULT_CANCEL);
            intent.putExtra(IaaaWrapper.EXTRA_iAAA_UID, "");
            intent.putExtra(IaaaWrapper.EXTRA_iAAA_TOKEN, "");
            IAAA_Authen.this.setResult(-1, intent);
            IAAA_Authen.this.finish();
        }
    }

    final class SendCodeClickListener implements View.OnClickListener {

        final class DisableSendCodeRunnable implements Runnable {
            DisableSendCodeRunnable() {
            }

            @Override
            public final void run() {
                IAAA_Authen.this.sendCodeButton.setEnabled(false);
                IAAA_Authen iAAA_Authen = IAAA_Authen.this;
                iAAA_Authen.sendCodeButton.setTextColor(ContextCompat.getColor(iAAA_Authen.getApplicationContext(), R.color.colorIPGWGray));
            }
        }

        final class SendCodeCountdownTask extends TimerTask {
            SendCodeCountdownTask() {
            }

            @Override
            public final void run() {
                IAAA_Authen iAAA_Authen = IAAA_Authen.this;
                iAAA_Authen.g(iAAA_Authen.sendCodeButton);
            }
        }

        SendCodeClickListener() {
        }

        @Override
        public final void onClick(View view) {
            View rootView;
            Resources resources = IAAA_Authen.this.getResources();
            int index = R.string.enter_username_passwd;
            String string;
            String str;
            IAAA_Authen.a(IAAA_Authen.this);
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            if (iAAA_Authen.smsMode) {
                String trim = iAAA_Authen.usernameEdit.getText().toString().trim();
                String trim2 = IAAA_Authen.this.passwordEdit.getText().toString().trim();
                if (trim.length() == 0 || trim2.length() == 0) {
                    rootView = IAAA_Authen.this.getWindow().getDecorView().getRootView();
                    resources = IAAA_Authen.this.getResources();
                    index = R.string.enter_username_passwd;
                } else if (trim.length() < 2 || trim2.length() < 8) {
                    rootView = IAAA_Authen.this.getWindow().getDecorView().getRootView();
                    resources = IAAA_Authen.this.getResources();
                    index = R.string.wrong_username_passwd;
                } else {
                    IAAA_Authen iAAA_Authen2 = IAAA_Authen.this;
                    iAAA_Authen2.username = trim;
                    iAAA_Authen2.password = trim2;
                    iAAA_Authen2.loginSucceeded = true;
                    new Handler(Looper.getMainLooper()).post(new DisableSendCodeRunnable());
                    IAAA_Authen.this.countdownSeconds = 60;
                    if (IAAA_Authen.countdownTimer == null) {
                        IAAA_Authen.countdownTimer = new Timer();
                    }
                    IAAA_Authen.countdownTimer.schedule(new SendCodeCountdownTask(), 0L, 2000L);
                    IAAA_Authen.this.h(true);
                    OrLoadingDialog progressDialog = IAAA_Authen.i(IAAA_Authen.this);
                    progressDialog.show();
                    try {
                        String mobileMask = IaaaApi.requestSmsCode(IAAA_Authen.this.username, IAAA_Authen.appId);
                        if (mobileMask.equals("")) {
                            str = "验证码已经发送到您的手机！";
                        } else {
                            str = "验证码已经发送到您的手机: " + mobileMask;
                        }
                        IAAA_Authen.this.h(true);
                        progressDialog.dismiss();
                        IAAA_Authen.j(IAAA_Authen.this.getWindow().getDecorView().getRootView(), str).show();
                        return;
                    } catch (Exception e2) {
                        IAAA_Authen.this.h(true);
                        progressDialog.dismiss();
                        rootView = IAAA_Authen.this.getWindow().getDecorView().getRootView();
                        string = e2.getMessage();
                    }
                }
                string = resources.getString(index);
                IAAA_Authen.f(rootView, string).show();
            }
        }
    }

    final class CopyrightLinkClickListener implements View.OnClickListener {
        CopyrightLinkClickListener() {
        }

        @Override
        public final void onClick(View view) {
            IAAA_Authen.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://cc.pku.edu.cn")));
        }
    }

    final class SetInputsEnabledRunnable implements Runnable {

        final /* synthetic */ boolean enabled;

        SetInputsEnabledRunnable(boolean z2) {
            this.enabled = z2;
        }

        @Override
        public final void run() {
            IAAA_Authen.this.usernameEdit.setEnabled(this.enabled);
            IAAA_Authen.this.passwordEdit.setEnabled(this.enabled);
            IAAA_Authen.this.verificationCodeEdit.setEnabled(this.enabled);
            IAAA_Authen.this.sendCodeButton.setEnabled(this.enabled);
            IAAA_Authen.this.loginButton.setEnabled(this.enabled);
            IAAA_Authen.this.cancelButton.setEnabled(this.enabled);
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            if (!iAAA_Authen.smsMode && !iAAA_Authen.otpMode) {
                iAAA_Authen.verificationCodeContainer.setVisibility(8);
                return;
            }
            iAAA_Authen.verificationCodeContainer.setVisibility(0);
            IAAA_Authen iAAA_Authen2 = IAAA_Authen.this;
            boolean z2 = iAAA_Authen2.smsMode;
            TextView textView = iAAA_Authen2.sendCodeButton;
            if (z2) {
                textView.setVisibility(0);
                IAAA_Authen iAAA_Authen3 = IAAA_Authen.this;
                iAAA_Authen3.verificationCodeEdit.setHint(iAAA_Authen3.getResources().getString(R.string.hint_msgcode));
            } else {
                textView.setVisibility(4);
                IAAA_Authen iAAA_Authen4 = IAAA_Authen.this;
                iAAA_Authen4.verificationCodeEdit.setHint(iAAA_Authen4.getResources().getString(R.string.hint_otpcode));
            }
        }
    }

    final class UpdateSendCodeTextRunnable implements Runnable {

        final /* synthetic */ TextView sendCodeText;

        UpdateSendCodeTextRunnable(TextView textView) {
            this.sendCodeText = textView;
        }

        @Override
        public final void run() {
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            int index = iAAA_Authen.countdownSeconds;
            if (index <= 0) {
                this.sendCodeText.setText(iAAA_Authen.getResources().getString(R.string.sendcode));
                this.sendCodeText.setTextColor(ContextCompat.getColor(IAAA_Authen.this.getApplicationContext(), R.color.black1));
                this.sendCodeText.setEnabled(true);
                IAAA_Authen.countdownTimer.cancel();
                IAAA_Authen.countdownTimer = null;
                IAAA_Authen.this.loginSucceeded = false;
                return;
            }
            iAAA_Authen.countdownSeconds = index - 2;
            this.sendCodeText.setText(IAAA_Authen.this.getResources().getString(R.string.sendcode_wait) + IAAA_Authen.this.countdownSeconds);
        }
    }

    public static void a(Activity activity) {
        View currentFocus = activity.getWindow().getCurrentFocus();
        if (currentFocus != null) {
            ((InputMethodManager) activity.getSystemService("input_method")).hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    static AlertDialog f(View view, String str) {
        return new MaterialAlertDialogBuilder(view.getContext()).setTitle(R.string.error).setMessage(str).setNegativeButton(R.string.ok, (DialogInterface.OnClickListener) null).create();
    }

    static OrLoadingDialog i(Activity activity) {
        OrLoadingDialog progressDialog = new OrLoadingDialog(activity);
        progressDialog.setProgressStyle(0);
        progressDialog.setMessage("请稍候...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    static AlertDialog j(View view, String str) {
        return new MaterialAlertDialogBuilder(view.getContext()).setTitle(R.string.notice).setMessage(str).setNegativeButton(R.string.ok, (DialogInterface.OnClickListener) null).create();
    }

    final void g(TextView textView) {
        new Handler(Looper.getMainLooper()).post(new UpdateSendCodeTextRunnable(textView));
    }

    final void h(boolean z2) {
        new Handler(Looper.getMainLooper()).post(new SetInputsEnabledRunnable(z2));
    }

    @Override
    protected void onCreate(Bundle bundle) {
        EditText editText;
        Resources resources;
        int index;
        super.onCreate(bundle);
        setContentView(R.layout.activity_login);
        Intent intent = getIntent();
        appId = intent.getStringExtra(IaaaWrapper.EXTRA_iAAA_APPID);
        this.username = intent.getStringExtra(IaaaWrapper.EXTRA_iAAA_UID);
        String str = appId;
        if (str == null || str.equals("")) {
            appId = "NA";
        }
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        this.smsMode = false;
        this.otpMode = false;
        this.secondFactorRequired = false;
        if (this.username == null) {
            this.username = "";
        }
        this.password = "";
        this.verificationCode = "";
        this.loginButton = (TextView) findViewById(R.id.login);
        this.cancelButton = (TextView) findViewById(R.id.cancel);
        this.sendCodeButton = (TextView) findViewById(R.id.sendcodes);
        this.usernameEdit = (EditText) findViewById(R.id.userName);
        if (!this.username.equals("")) {
            this.usernameEdit.setText(this.username);
        }
        this.passwordEdit = (EditText) findViewById(R.id.passwd);
        this.verificationCodeEdit = (EditText) findViewById(R.id.msgcodes);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.msgcodecontainer);
        this.verificationCodeContainer = linearLayout;
        if (this.smsMode || this.otpMode) {
            linearLayout.setVisibility(0);
            if (this.smsMode) {
                this.sendCodeButton.setVisibility(0);
                editText = this.verificationCodeEdit;
                resources = getResources();
                index = R.string.hint_msgcode;
            } else {
                this.sendCodeButton.setVisibility(4);
                editText = this.verificationCodeEdit;
                resources = getResources();
                index = R.string.hint_otpcode;
            }
            editText.setHint(resources.getString(index));
        } else {
            linearLayout.setVisibility(8);
        }
        if (this.loginSucceeded) {
            new Handler(Looper.getMainLooper()).post(new ShowSendCodeWaitRunnable());
            Timer timer = countdownTimer;
            if (timer != null) {
                timer.cancel();
            }
            countdownTimer = new Timer();
            countdownTimer.schedule(new SendCodeCountdownTask(), 0L, 2000L);
        }
        this.loginButton.setOnClickListener(new LoginClickListener());
        this.cancelButton.setOnClickListener(new CancelClickListener());
        this.sendCodeButton.setOnClickListener(new SendCodeClickListener());
        ((TextView) findViewById(R.id.cc_link)).setOnClickListener(new CopyrightLinkClickListener());
    }
}
