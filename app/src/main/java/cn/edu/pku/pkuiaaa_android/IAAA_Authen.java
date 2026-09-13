package cn.edu.pku.pkuiaaa_android;

import android.app.Activity;
import android.app.ProgressDialog;
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

    /* renamed from: s, reason: collision with root package name */
    static String f6819s;

    /* renamed from: t, reason: collision with root package name */
    static Timer f6820t = new Timer();

    /* renamed from: b, reason: collision with root package name */
    int f6821b = 0;

    /* renamed from: c, reason: collision with root package name */
    boolean f6822c = false;

    /* renamed from: d, reason: collision with root package name */
    boolean f6823d = false;

    /* renamed from: e, reason: collision with root package name */
    final int f6824e = 2000;

    /* renamed from: f, reason: collision with root package name */
    final int f6825f = 60;

    /* renamed from: g, reason: collision with root package name */
    boolean f6826g = false;

    /* renamed from: h, reason: collision with root package name */
    boolean f6827h = false;

    /* renamed from: i, reason: collision with root package name */
    TextView f6828i;

    /* renamed from: j, reason: collision with root package name */
    TextView f6829j;

    /* renamed from: k, reason: collision with root package name */
    TextView f6830k;

    /* renamed from: l, reason: collision with root package name */
    EditText f6831l;

    /* renamed from: m, reason: collision with root package name */
    EditText f6832m;

    /* renamed from: n, reason: collision with root package name */
    EditText f6833n;

    /* renamed from: o, reason: collision with root package name */
    LinearLayout f6834o;

    /* renamed from: p, reason: collision with root package name */
    String f6835p;

    /* renamed from: q, reason: collision with root package name */
    String f6836q;

    /* renamed from: r, reason: collision with root package name */
    String f6837r;

    final class a implements Runnable {
        a() {
        }

        @Override // java.lang.Runnable
        public final void run() {
            IAAA_Authen.this.f6830k.setEnabled(false);
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            iAAA_Authen.f6830k.setTextColor(ContextCompat.getColor(iAAA_Authen.getApplicationContext(), R.color.colorIPGWGray));
            IAAA_Authen.this.f6830k.setText(IAAA_Authen.this.getResources().getString(R.string.sendcode_wait) + IAAA_Authen.this.f6821b);
        }
    }

    final class b extends TimerTask {
        b() {
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public final void run() {
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            iAAA_Authen.g(iAAA_Authen.f6830k);
        }
    }

    final class c implements View.OnClickListener {
        c() {
        }

        @Override // android.view.View.OnClickListener
        public final void onClick(View view) {
            IAAA_Authen.a(IAAA_Authen.this);
            String username = IAAA_Authen.this.f6831l.getText().toString().trim();
            String password = IAAA_Authen.this.f6832m.getText().toString().trim();
            View root = IAAA_Authen.this.getWindow().getDecorView().getRootView();
            if (username.length() == 0 || password.length() == 0) {
                IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(R.string.enter_username_passwd)).show();
                return;
            }
            if (username.length() < 2 || password.length() < 8) {
                IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(R.string.wrong_username_passwd)).show();
                return;
            }
            if (!username.equals(IAAA_Authen.this.f6835p)) {
                IAAA_Authen.this.f6823d = false;
                IAAA_Authen.this.f6826g = false;
                IAAA_Authen.this.f6827h = false;
            }
            String code = "";
            if (IAAA_Authen.this.f6826g || IAAA_Authen.this.f6827h) {
                code = IAAA_Authen.this.f6833n.getText().toString().trim();
                if (code.length() == 0) {
                    int message = IAAA_Authen.this.f6826g ? R.string.enter_msgcode : R.string.enter_otpcode;
                    IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(message)).show();
                    return;
                }
                if (code.length() < 4 || code.length() > 6) {
                    int message = IAAA_Authen.this.f6826g ? R.string.wrong_msgcode : R.string.wrong_otpcode;
                    IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(message)).show();
                    return;
                }
                try {
                    Integer.parseInt(code);
                } catch (Exception ignored) {
                    int message = IAAA_Authen.this.f6826g ? R.string.wrong_msgcode : R.string.wrong_otpcode;
                    IAAA_Authen.f(root, IAAA_Authen.this.getResources().getString(message)).show();
                    return;
                }
            }
            IAAA_Authen.this.f6835p = username;
            IAAA_Authen.this.f6836q = password;
            IAAA_Authen.this.f6837r = code;
            IAAA_Authen.this.h(false);
            ProgressDialog progress = IAAA_Authen.i(IAAA_Authen.this);
            progress.show();
            try {
                String mode = IAAA_Authen.this.f6823d
                        ? (IAAA_Authen.this.f6826g ? "SMS" : "OTP")
                        : cn.edu.pku.pkuiaaa_android.a.e(username, IAAA_Authen.f6819s);
                if (mode == null) mode = "";
                if (!IAAA_Authen.this.f6823d && mode.length() != 0) {
                    IAAA_Authen.this.f6826g = "SMS".equals(mode);
                    IAAA_Authen.this.f6827h = !IAAA_Authen.this.f6826g;
                    IAAA_Authen.this.f6823d = true;
                    IAAA_Authen.this.h(true);
                    progress.dismiss();
                    IAAA_Authen.j(root, "请输入验证码后再次登录").show();
                    return;
                }
                String token = cn.edu.pku.pkuiaaa_android.a.d(username, password, code, IAAA_Authen.f6819s, mode);
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

    final class d implements View.OnClickListener {
        d() {
        }

        @Override // android.view.View.OnClickListener
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

    final class e implements View.OnClickListener {

        final class a implements Runnable {
            a() {
            }

            @Override // java.lang.Runnable
            public final void run() {
                IAAA_Authen.this.f6830k.setEnabled(false);
                IAAA_Authen iAAA_Authen = IAAA_Authen.this;
                iAAA_Authen.f6830k.setTextColor(ContextCompat.getColor(iAAA_Authen.getApplicationContext(), R.color.colorIPGWGray));
            }
        }

        final class b extends TimerTask {
            b() {
            }

            @Override // java.util.TimerTask, java.lang.Runnable
            public final void run() {
                IAAA_Authen iAAA_Authen = IAAA_Authen.this;
                iAAA_Authen.g(iAAA_Authen.f6830k);
            }
        }

        e() {
        }

        @Override // android.view.View.OnClickListener
        public final void onClick(View view) {
            View rootView;
            Resources resources = IAAA_Authen.this.getResources();
            int i2 = R.string.enter_username_passwd;
            String string;
            String str;
            IAAA_Authen.a(IAAA_Authen.this);
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            if (iAAA_Authen.f6826g) {
                String trim = iAAA_Authen.f6831l.getText().toString().trim();
                String trim2 = IAAA_Authen.this.f6832m.getText().toString().trim();
                if (trim.length() == 0 || trim2.length() == 0) {
                    rootView = IAAA_Authen.this.getWindow().getDecorView().getRootView();
                    resources = IAAA_Authen.this.getResources();
                    i2 = R.string.enter_username_passwd;
                } else if (trim.length() < 2 || trim2.length() < 8) {
                    rootView = IAAA_Authen.this.getWindow().getDecorView().getRootView();
                    resources = IAAA_Authen.this.getResources();
                    i2 = R.string.wrong_username_passwd;
                } else {
                    IAAA_Authen iAAA_Authen2 = IAAA_Authen.this;
                    iAAA_Authen2.f6835p = trim;
                    iAAA_Authen2.f6836q = trim2;
                    iAAA_Authen2.f6822c = true;
                    new Handler(Looper.getMainLooper()).post(new a());
                    IAAA_Authen.this.f6821b = 60;
                    if (IAAA_Authen.f6820t == null) {
                        IAAA_Authen.f6820t = new Timer();
                    }
                    IAAA_Authen.f6820t.schedule(new b(), 0L, 2000L);
                    IAAA_Authen.this.h(true);
                    ProgressDialog i3 = IAAA_Authen.i(IAAA_Authen.this);
                    i3.show();
                    try {
                        String c2 = cn.edu.pku.pkuiaaa_android.a.c(IAAA_Authen.this.f6835p, IAAA_Authen.f6819s);
                        if (c2.equals("")) {
                            str = "验证码已经发送到您的手机！";
                        } else {
                            str = "验证码已经发送到您的手机: " + c2;
                        }
                        IAAA_Authen.this.h(true);
                        i3.dismiss();
                        IAAA_Authen.j(IAAA_Authen.this.getWindow().getDecorView().getRootView(), str).show();
                        return;
                    } catch (Exception e2) {
                        IAAA_Authen.this.h(true);
                        i3.dismiss();
                        rootView = IAAA_Authen.this.getWindow().getDecorView().getRootView();
                        string = e2.getMessage();
                    }
                }
                string = resources.getString(i2);
                IAAA_Authen.f(rootView, string).show();
            }
        }
    }

    final class f implements View.OnClickListener {
        f() {
        }

        @Override // android.view.View.OnClickListener
        public final void onClick(View view) {
            IAAA_Authen.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://cc.pku.edu.cn")));
        }
    }

    final class g implements Runnable {

        /* renamed from: a, reason: collision with root package name */
        final /* synthetic */ boolean f6846a;

        g(boolean z2) {
            this.f6846a = z2;
        }

        @Override // java.lang.Runnable
        public final void run() {
            IAAA_Authen.this.f6831l.setEnabled(this.f6846a);
            IAAA_Authen.this.f6832m.setEnabled(this.f6846a);
            IAAA_Authen.this.f6833n.setEnabled(this.f6846a);
            IAAA_Authen.this.f6830k.setEnabled(this.f6846a);
            IAAA_Authen.this.f6828i.setEnabled(this.f6846a);
            IAAA_Authen.this.f6829j.setEnabled(this.f6846a);
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            if (!iAAA_Authen.f6826g && !iAAA_Authen.f6827h) {
                iAAA_Authen.f6834o.setVisibility(8);
                return;
            }
            iAAA_Authen.f6834o.setVisibility(0);
            IAAA_Authen iAAA_Authen2 = IAAA_Authen.this;
            boolean z2 = iAAA_Authen2.f6826g;
            TextView textView = iAAA_Authen2.f6830k;
            if (z2) {
                textView.setVisibility(0);
                IAAA_Authen iAAA_Authen3 = IAAA_Authen.this;
                iAAA_Authen3.f6833n.setHint(iAAA_Authen3.getResources().getString(R.string.hint_msgcode));
            } else {
                textView.setVisibility(4);
                IAAA_Authen iAAA_Authen4 = IAAA_Authen.this;
                iAAA_Authen4.f6833n.setHint(iAAA_Authen4.getResources().getString(R.string.hint_otpcode));
            }
        }
    }

    final class h implements Runnable {

        /* renamed from: a, reason: collision with root package name */
        final /* synthetic */ TextView f6848a;

        h(TextView textView) {
            this.f6848a = textView;
        }

        @Override // java.lang.Runnable
        public final void run() {
            IAAA_Authen iAAA_Authen = IAAA_Authen.this;
            int i2 = iAAA_Authen.f6821b;
            if (i2 <= 0) {
                this.f6848a.setText(iAAA_Authen.getResources().getString(R.string.sendcode));
                this.f6848a.setTextColor(ContextCompat.getColor(IAAA_Authen.this.getApplicationContext(), R.color.black1));
                this.f6848a.setEnabled(true);
                IAAA_Authen.f6820t.cancel();
                IAAA_Authen.f6820t = null;
                IAAA_Authen.this.f6822c = false;
                return;
            }
            iAAA_Authen.f6821b = i2 - 2;
            this.f6848a.setText(IAAA_Authen.this.getResources().getString(R.string.sendcode_wait) + IAAA_Authen.this.f6821b);
        }
    }

    public static void a(Activity activity) {
        View currentFocus = activity.getWindow().getCurrentFocus();
        if (currentFocus != null) {
            ((InputMethodManager) activity.getSystemService("input_method")).hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    static AlertDialog f(View view, String str) {
        return new AlertDialog.Builder(view.getContext()).setTitle(R.string.error).setMessage(str).setNegativeButton(R.string.ok, (DialogInterface.OnClickListener) null).create();
    }

    static ProgressDialog i(Activity activity) {
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setProgressStyle(0);
        progressDialog.setMessage("请稍候...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    static AlertDialog j(View view, String str) {
        return new AlertDialog.Builder(view.getContext()).setTitle(R.string.notice).setMessage(str).setNegativeButton(R.string.ok, (DialogInterface.OnClickListener) null).create();
    }

    final void g(TextView textView) {
        new Handler(Looper.getMainLooper()).post(new h(textView));
    }

    final void h(boolean z2) {
        new Handler(Looper.getMainLooper()).post(new g(z2));
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        EditText editText;
        Resources resources;
        int i2;
        super.onCreate(bundle);
        setContentView(R.layout.activity_login);
        Intent intent = getIntent();
        f6819s = intent.getStringExtra(IaaaWrapper.EXTRA_iAAA_APPID);
        this.f6835p = intent.getStringExtra(IaaaWrapper.EXTRA_iAAA_UID);
        String str = f6819s;
        if (str == null || str.equals("")) {
            f6819s = "NA";
        }
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        this.f6826g = false;
        this.f6827h = false;
        this.f6823d = false;
        if (this.f6835p == null) {
            this.f6835p = "";
        }
        this.f6836q = "";
        this.f6837r = "";
        this.f6828i = (TextView) findViewById(R.id.login);
        this.f6829j = (TextView) findViewById(R.id.cancel);
        this.f6830k = (TextView) findViewById(R.id.sendcodes);
        this.f6831l = (EditText) findViewById(R.id.userName);
        if (!this.f6835p.equals("")) {
            this.f6831l.setText(this.f6835p);
        }
        this.f6832m = (EditText) findViewById(R.id.passwd);
        this.f6833n = (EditText) findViewById(R.id.msgcodes);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.msgcodecontainer);
        this.f6834o = linearLayout;
        if (this.f6826g || this.f6827h) {
            linearLayout.setVisibility(0);
            if (this.f6826g) {
                this.f6830k.setVisibility(0);
                editText = this.f6833n;
                resources = getResources();
                i2 = R.string.hint_msgcode;
            } else {
                this.f6830k.setVisibility(4);
                editText = this.f6833n;
                resources = getResources();
                i2 = R.string.hint_otpcode;
            }
            editText.setHint(resources.getString(i2));
        } else {
            linearLayout.setVisibility(8);
        }
        if (this.f6822c) {
            new Handler(Looper.getMainLooper()).post(new a());
            Timer timer = f6820t;
            if (timer != null) {
                timer.cancel();
            }
            f6820t = new Timer();
            f6820t.schedule(new b(), 0L, 2000L);
        }
        this.f6828i.setOnClickListener(new c());
        this.f6829j.setOnClickListener(new d());
        this.f6830k.setOnClickListener(new e());
        ((TextView) findViewById(R.id.cc_link)).setOnClickListener(new f());
    }
}
