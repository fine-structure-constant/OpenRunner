package cn.edu.pku.pkurunner;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import cn.edu.pku.pkurunner.Exception.ServerException;
import cn.edu.pku.pkurunner.Model.User;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.Offline.QuickOfflineSignin;
import cn.edu.pku.pkurunner.Utils.ClientUpdateNotice;
import cn.edu.pku.pkurunner.Utils.IaaaWrapper;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import java.util.ArrayList;
import java.util.List;
import org.xutils.common.Callback;

public class LoginActivity extends AppCompatActivity {

    /* renamed from: b, reason: collision with root package name */
    private ProgressDialog f6865b;

    private final ActivityResultLauncher<Intent> f6866c = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override // androidx.activity.result.ActivityResultCallback
        public void onActivityResult(ActivityResult activityResult) {
            IaaaWrapper.HandleIaaaResult(LoginActivity.this, activityResult).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.n0
                @Override // io.reactivex.functions.Function
                public final Object apply(Object obj) {
                    ObservableSource z2;
                    z2 = LoginActivity.this.z((Pair) obj);
                    return z2;
                }
            }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.o0
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    LoginActivity.this.A((Boolean) obj);
                }
            }, new Consumer() { // from class: cn.edu.pku.pkurunner.p0
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    LoginActivity.this.B((Throwable) obj);
                }
            });
        }
    });

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void A(Boolean bool) {
        ProgressDialog progressDialog = this.f6865b;
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void B(Throwable th) {
        ProgressDialog progressDialog = this.f6865b;
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        x(th);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource D(Pair pair) {
        String str = (String) pair.first;
        int intValue = ((Integer) pair.second).intValue();
        if (!w(str, intValue)) {
            return Observable.error(new Throwable(getString(R.string.a_login_offline_invalid)));
        }
        Data.setUser(User.createOfflineUser(intValue == 0 ? 1 : 0, str));
        return Data.saveUserToDatabase();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void E(Throwable th) {
        if (th instanceof Exception) {
            Toast.makeText(this, th.getLocalizedMessage(), 0).show();
        } else {
            Toast.makeText(this, getString(R.string.a_login_offline_existing_error, th.getLocalizedMessage()), 0).show();
        }
        th.printStackTrace();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void F(View view) {
        Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.r0
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                LoginActivity.this.L(observableEmitter);
            }
        }).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.s0
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource M;
                M = LoginActivity.M((String) obj);
                return M;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.t0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                LoginActivity.this.N((Boolean) obj);
            }
        }, new Consumer() { // from class: cn.edu.pku.pkurunner.e0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                LoginActivity.this.E((Throwable) obj);
            }
        });
    }

    private void Q() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        this.f6865b = progressDialog;
        progressDialog.setProgressStyle(0);
        this.f6865b.setMessage(getString(R.string.a_login_logining_to_server));
        this.f6865b.setIndeterminate(false);
        this.f6865b.setCancelable(false);
        this.f6865b.show();
    }

    private boolean w(String str, int i2) {
        if ("".equals(str) || str == null) {
            return false;
        }
        return i2 == 0 || i2 == 1;
    }

    private void x(final Throwable th) {
        if ((th instanceof ServerException) && 15 == ((ServerException) th).getErrorCode()) {
            P();
        } else {
            Network.interceptIfSocketTimeout(th, new Callback.Callable() { // from class: cn.edu.pku.pkurunner.q0
                @Override // org.xutils.common.Callback.Callable
                public final void call(Object obj) {
                    LoginActivity.this.y(th, (Void) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void y(Throwable th, Void r4) {
        Toast.makeText(this, th instanceof IaaaWrapper.IAAAException ? th.getLocalizedMessage() : getString(R.string.a_login_login_to_server_error, th.getLocalizedMessage()), 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource z(Pair pair) {
        Data.setUser(new User((String) pair.first, (String) pair.second));
        Q();
        return Data.login();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void C(View view) {
        this.f6866c.launch(IaaaWrapper.createIaaaIntent(this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void G(Boolean bool) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void H(Throwable th) {
        Toast.makeText(this, th.getLocalizedMessage(), 0).show();
        th.printStackTrace();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void I(View view) {
        QuickOfflineSignin.createDialog(this).observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.f0
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource D;
                D = LoginActivity.this.D((Pair) obj);
                return D;
            }
        }).subscribeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.g0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                LoginActivity.this.G((Boolean) obj);
            }
        }, new Consumer() { // from class: cn.edu.pku.pkurunner.h0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                LoginActivity.this.H((Throwable) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void K(List list, ObservableEmitter observableEmitter, DialogInterface dialogInterface, int i2) {
        observableEmitter.onNext(((User) list.get(i2)).getId());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void L(final ObservableEmitter observableEmitter) {
        List<User> databaseUsers = Data.getDatabaseUsers();
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice);
        final ArrayList arrayList = new ArrayList();
        if (databaseUsers == null) {
            observableEmitter.onError(new Exception("There is no saved offline user."));
            return;
        }
        for (User user : databaseUsers) {
            if (user.isOffline().booleanValue()) {
                arrayList.add(user);
                arrayAdapter.add(user.getName());
            }
        }
        new AlertDialog.Builder(this).setTitle(getString(R.string.a_login_offline_existing_choose_one)).setNegativeButton(getString(R.string.a_login_offline_existing_cancel), new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.j0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                dialogInterface.dismiss();
            }
        }).setAdapter(arrayAdapter, new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.k0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                LoginActivity.K(arrayList, observableEmitter, dialogInterface, i2);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource M(String str) {
        Data.loadSpecificUser(str);
        return Data.loadByUser();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void N(Boolean bool) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void O(Boolean bool) {
        if (!bool.booleanValue()) {
            moveTaskToBack(true);
        }
    }

    private void P() {
        ClientUpdateNotice.showVersionLowDialog(this).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.i0
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                LoginActivity.this.O((Boolean) obj);
            }
        });
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        setContentView(R.layout.activity_login_pkurunner);
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("logout", false)) {
            Data.setUser(null);
            Data.setUserStatus(null);
            Data.setValid(false);
            Data.saveCurrentUserIdToFile();
        }
        ((Button) findViewById(R.id.a_login_btn_iaaa_login)).setOnClickListener(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.d0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LoginActivity.this.C(view);
            }
        });
        ((Button) findViewById(R.id.a_login_btn_offline_login)).setOnClickListener(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.l0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LoginActivity.this.I(view);
            }
        });
        ((Button) findViewById(R.id.a_login_btn_old_user)).setOnClickListener(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.m0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LoginActivity.this.F(view);
            }
        });
    }
}
