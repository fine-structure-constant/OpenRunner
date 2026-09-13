package cn.edu.pku.pkurunner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import cn.edu.pku.pkurunner.Broadcasts.WeatherUpdateReceiver;
import cn.edu.pku.pkurunner.Exception.ServerException;
import cn.edu.pku.pkurunner.GuidePage.IntroActivity;
import cn.edu.pku.pkurunner.Map.MapFragment;
import cn.edu.pku.pkurunner.Model.User;
import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.Model.Weather;
import cn.edu.pku.pkurunner.Network.Model.UserStatus;
import cn.edu.pku.pkurunner.Network.Model.Version;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.Permission.PermissionDialog;
import cn.edu.pku.pkurunner.RecordList.RecordListFragment;
import cn.edu.pku.pkurunner.RecordList.RecordListPresenter;
import cn.edu.pku.pkurunner.Settings.SettingsActivity;
import cn.edu.pku.pkurunner.TaskList.TaskListFragment;
import cn.edu.pku.pkurunner.TaskList.TaskListPresenter;
import cn.edu.pku.pkurunner.Utils.ClientUpdateNotice;
import cn.edu.pku.pkurunner.Utils.PerfectExitUtil;
import cn.edu.pku.pkurunner.Utils.SerializeHelper;
import cn.edu.pku.pkurunner.Utils.ThreatDetectUtil;
import cn.edu.pku.pkurunner.Utils.TokenInvalidNotice;
import cn.edu.pku.pkurunner.Utils.UpdateHelper;
import cn.edu.pku.pkurunner.View.ProgressableView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.xutils.common.Callback;
import org.xutils.common.util.LogUtil;
import retrofit2.HttpException;

public class MainActivity extends AppCompatActivity {

    /* renamed from: b, reason: collision with root package name */
    private MapFragment f6866b;

    /* renamed from: c, reason: collision with root package name */
    private RecordListFragment f6867c;

    /* renamed from: d, reason: collision with root package name */
    private TaskListFragment f6868d;

    /* renamed from: e, reason: collision with root package name */
    private Fragment f6869e;

    /* renamed from: h, reason: collision with root package name */
    private NavigationView f6872h;

    /* renamed from: i, reason: collision with root package name */
    private DrawerLayout f6873i;

    /* renamed from: j, reason: collision with root package name */
    private CoordinatorLayout f6874j;

    /* renamed from: k, reason: collision with root package name */
    private SharedPreferences f6875k;

    /* renamed from: l, reason: collision with root package name */
    private SharedPreferences f6876l;

    /* renamed from: n, reason: collision with root package name */
    private WeatherUpdateReceiver f6878n;

    /* renamed from: o, reason: collision with root package name */
    private TextView f6879o;

    /* renamed from: p, reason: collision with root package name */
    private TextView f6880p;

    /* renamed from: q, reason: collision with root package name */
    private ProgressableView f6881q;

    /* renamed from: f, reason: collision with root package name */
    private int[] f6870f = new int[3];

    /* renamed from: g, reason: collision with root package name */
    private int f6871g = -1;

    /* renamed from: m, reason: collision with root package name */
    private long f6877m = 0;

    class a implements DialogInterface.OnClickListener {
        a() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i2) {
            PerfectExitUtil.exit();
        }
    }

    class b implements Observer<Weather> {
        @Override // io.reactivex.Observer
        public void onComplete() {
        }

        @Override // io.reactivex.Observer
        public void onSubscribe(Disposable disposable) {
        }

        b() {
        }

        @Override // io.reactivex.Observer
        /* renamed from: a, reason: merged with bridge method [inline-methods] */
        public void onNext(Weather weather) {
            MainActivity.this.F(weather);
        }

        @Override // io.reactivex.Observer
        public void onError(Throwable th) {
            MainActivity mainActivity = MainActivity.this;
            Toast.makeText(mainActivity, mainActivity.getString(R.string.a_main_error_loading_weather, th.getMessage()), 0).show();
        }
    }

    class c extends SimpleTarget<Bitmap> {
        c(int i2, int i3) {
            super(i2, i3);
        }

        @Override // com.bumptech.glide.request.target.Target
        /* renamed from: a, reason: merged with bridge method [inline-methods] */
        public void onResourceReady(Bitmap bitmap, Transition transition) {
            MainActivity.this.f6872h.getMenu().findItem(R.id.nav_weather).setIcon(new BitmapDrawable(MainActivity.this.getResources(), bitmap));
        }
    }

    class d extends ActionBarDrawerToggle {
        d(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar, int i2, int i3) {
            super(activity, drawerLayout, toolbar, i2, i3);
        }

        @Override // androidx.appcompat.app.ActionBarDrawerToggle, androidx.drawerlayout.widget.DrawerLayout.DrawerListener
        public void onDrawerOpened(View view) {
            super.onDrawerOpened(view);
            MainActivity.this.refreshUserStatusNotice();
        }
    }

    class e extends TapTargetView.Listener {
        e() {
        }

        @Override // com.getkeepsafe.taptargetview.TapTargetView.Listener
        public void onTargetClick(TapTargetView tapTargetView) {
            super.onTargetClick(tapTargetView);
            MainActivity.this.switchFromRecordListToRunning();
        }
    }

    class f extends TapTargetView.Listener {
        f() {
        }

        @Override // com.getkeepsafe.taptargetview.TapTargetView.Listener
        public void onOuterCircleClick(TapTargetView tapTargetView) {
            super.onOuterCircleClick(tapTargetView);
            tapTargetView.dismiss(false);
        }

        @Override // com.getkeepsafe.taptargetview.TapTargetView.Listener
        public void onTargetClick(TapTargetView tapTargetView) {
            super.onTargetClick(tapTargetView);
            MainActivity.this.e0();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void O(Throwable th) {
        Toast.makeText(this, getString(R.string.a_main_error_loading_data, th.getMessage()), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void Q(DialogInterface dialogInterface, int i2) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void U(Boolean bool) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void V(Throwable th) {
        Toast.makeText(this, getString(R.string.a_main_error_loading_data, th.getMessage()), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void Y(Boolean bool) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void Z(Throwable th, Void r3) {
        Toast.makeText(this, getString(R.string.a_main_error_loading_status, th.getMessage()), 0).show();
    }

    private void i0(Bundle bundle) {
        if (bundle == null) {
            for (int i2 = 0; i2 < 3; i2++) {
                this.f6870f[i2] = 0;
            }
            this.f6866b = new MapFragment();
            RecordListFragment recordListFragment = new RecordListFragment();
            this.f6867c = recordListFragment;
            new RecordListPresenter(recordListFragment);
            TaskListFragment taskListFragment = new TaskListFragment();
            this.f6868d = taskListFragment;
            new TaskListPresenter(taskListFragment);
            this.f6869e = null;
            this.f6871g = -1;
            return;
        }
        LogUtil.d("onCreate have instance");
        this.f6870f = bundle.getIntArray("FRAGMENT_ARRAY_KEY");
        this.f6871g = bundle.getInt("FRAGMENT_LAST_KEY");
        LogUtil.d("lastTag: " + String.valueOf(this.f6871g));
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        FragmentTransaction beginTransaction = supportFragmentManager.beginTransaction();
        for (int i3 = 0; i3 < 3; i3++) {
            if (this.f6870f[i3] == 1) {
                Fragment findFragmentByTag = supportFragmentManager.findFragmentByTag(String.valueOf(i3));
                if (this.f6871g != i3) {
                    beginTransaction.hide(findFragmentByTag);
                } else {
                    this.f6869e = findFragmentByTag;
                }
                setFragmentById(i3, findFragmentByTag);
            } else {
                setFragmentById(i3, null);
            }
        }
        beginTransaction.commit();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void F(Weather weather) {
        this.f6875k.edit().putString("weather.nmc.v1", SerializeHelper.objectToString(weather)).apply();
        Toast.makeText(this, weather.getDescription(this), 1).show();
        D(weather);
    }

    private static boolean J() {
        File[] listFiles = new File("/dev").listFiles();
        Pattern compile = Pattern.compile("vbox.*");
        if (listFiles != null && listFiles.length != 0) {
            for (File file : listFiles) {
                if (compile.matcher(file.getName()).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void N(Boolean bool) {
        if (this.f6876l.getBoolean(IntroActivity.GuidePreferencesKey, false)) {
            if (Data.getUser() == null) {
                startActivity(new Intent(this, (Class<?>) LoginActivity.class));
            }
        } else {
            Intent intent = new Intent(this, (Class<?>) IntroActivity.class);
            intent.putExtra("jumpToLogin", true);
            startActivity(intent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void P(DialogInterface dialogInterface, int i2) {
        Intent intent = new Intent(this, (Class<?>) LoginActivity.class);
        intent.putExtra("logout", true);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void a0(final Throwable th) {
        if ((th instanceof ServerException) && ((ServerException) th).getErrorCode() == 15) {
            ClientUpdateNotice.showVersionLowDialog(this).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.c1
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MainActivity.this.X((Boolean) obj);
                }
            });
        } else if ((th instanceof HttpException) && ((HttpException) th).code() == 401) {
            TokenInvalidNotice.showTokenInvalidDialog(this).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.d1
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MainActivity.Y((Boolean) obj);
                }
            });
        } else {
            Network.interceptIfSocketTimeout(th, new Callback.Callable() { // from class: cn.edu.pku.pkurunner.e1
                @Override // org.xutils.common.Callback.Callable
                public final void call(Object obj) {
                    MainActivity.this.Z(th, (Void) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void c0(Serializable serializable) {
        Weather weather = (Weather) serializable;
        Network.weather = weather;
        D(weather);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void e0() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(Uri.parse(Config.FEEDBACK_URL));
        startActivity(intent);
    }

    private void f0() {
        new AlertDialog.Builder(this).setTitle(R.string.a_main_logout_dialog_title).setMessage(R.string.a_main_logout_dialog_content).setPositiveButton(R.string.a_main_logout_dialog_positive, new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.g1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                MainActivity.this.P(dialogInterface, i2);
            }
        }).setNegativeButton(R.string.a_main_logout_dialog_negative, new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.h1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                MainActivity.Q(dialogInterface, i2);
            }
        }).setCancelable(true).create().show();
    }

    private void h0() {
        this.f6878n = new WeatherUpdateReceiver(new b());
        registerReceiver(this.f6878n, new IntentFilter("cn.edu.pku.pkurunner.MainActivity.weatherUpdate"));
    }

    private void j0() {
        SharedPreferences sharedPreferences = getSharedPreferences("WeatherService", 0);
        this.f6875k = sharedPreferences;
        if (sharedPreferences.contains("weather.nmc.v1")) {
            String string = this.f6875k.getString("weather.nmc.v1", null);
            if (string == null) {
                Network.weather = null;
            } else {
                SerializeHelper.stringToObject(string).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.o1
                    @Override // io.reactivex.functions.Consumer
                    public final void accept(Object obj) {
                        MainActivity.this.c0((Serializable) obj);
                    }
                }, new Consumer() { // from class: cn.edu.pku.pkurunner.p1
                    @Override // io.reactivex.functions.Consumer
                    public final void accept(Object obj) {
                        MainActivity.this.d0((Throwable) obj);
                    }
                });
            }
        }
    }

    private void m0(Fragment fragment) {
        Fragment fragment2 = this.f6869e;
        if (fragment2 != fragment) {
            this.f6869e = fragment;
            FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
            if (fragment2 != null) {
                beginTransaction.hide(fragment2);
            }
            int findIdByFragment = findIdByFragment(fragment);
            if (fragment.isAdded()) {
                beginTransaction.show(fragment);
            } else {
                this.f6870f[findIdByFragment] = 1;
                beginTransaction.add(R.id.v_main_frame, fragment, String.valueOf(findIdByFragment));
            }
            beginTransaction.commit();
            this.f6871g = findIdByFragment;
        }
    }

    private void n0(Fragment fragment, int i2, int i3) {
        Fragment fragment2 = this.f6869e;
        if (fragment2 != fragment) {
            this.f6869e = fragment;
            FragmentTransaction customAnimations = getSupportFragmentManager().beginTransaction().setCustomAnimations(i3, i2);
            if (fragment2 != null) {
                customAnimations.hide(fragment2);
            }
            int findIdByFragment = findIdByFragment(fragment);
            if (fragment.isAdded()) {
                customAnimations.show(fragment).commit();
            } else {
                this.f6870f[findIdByFragment] = 1;
                customAnimations.add(R.id.v_main_frame, fragment, String.valueOf(findIdByFragment)).commit();
            }
            this.f6871g = findIdByFragment;
        }
    }

    public int findIdByFragment(Fragment fragment) {
        if (fragment.equals(this.f6866b)) {
            return 0;
        }
        if (fragment.equals(this.f6867c)) {
            return 1;
        }
        return fragment.equals(this.f6868d) ? 2 : -1;
    }

    public void refreshUserStatusNotice() {
        User user = Data.getUser();
        if (user == null) {
            this.f6879o.setText("");
            this.f6880p.setText("");
        } else {
            this.f6879o.setText(user.getName());
            this.f6880p.setText(user.getDepartment());
        }
        if (user == null || user.isOffline().booleanValue()) {
            return;
        }
        UserStatus userStatus = Data.getUserStatus();
        if (userStatus == null) {
            this.f6881q.reset();
            return;
        }
        double current = userStatus.getCurrent();
        double bonus = userStatus.getBonus();
        double d2 = current + bonus;
        double target = userStatus.getTarget();
        int time = (int) ((((userStatus.getEndDate().getTime() - userStatus.getBeginDate().getTime()) / 1000) / 3600) / 24);
        int time2 = (int) ((((userStatus.getEndDate().getTime() - System.currentTimeMillis()) / 1000) / 3600) / 24);
        this.f6881q.setCollapseMode(false);
        if (System.currentTimeMillis() < userStatus.getBeginDate().getTime()) {
            this.f6881q.setActiveMode(false);
            this.f6881q.setSleepingIndicatorText(getString(R.string.status_pre_duration, getString(R.string.status_inactive_prompt), Integer.valueOf(((int) Math.ceil((((userStatus.getBeginDate().getTime() - System.currentTimeMillis()) / 1000) / 3600) / 24)) + 1), new SimpleDateFormat().format(Long.valueOf(userStatus.getBeginDate().getTime()))));
        } else {
            this.f6881q.setActiveMode(true);
            int i2 = time - time2;
            this.f6881q.setSecondaryProgress(i2 / (time + BitmapDescriptorFactory.HUE_RED));
            if (System.currentTimeMillis() <= userStatus.getEndDate().getTime()) {
                this.f6881q.setSecondaryText(getString(R.string.status_main_day, Integer.valueOf(Math.min(i2, time)), Integer.valueOf(time)));
            } else {
                this.f6881q.setSecondaryText(getString(R.string.status_post_duration, new SimpleDateFormat().format(Long.valueOf(userStatus.getEndDate().getTime()))));
            }
            if (target == 0.0d) {
                this.f6881q.setMainText(getString(R.string.status_main_distance_notarget, Double.valueOf(current / 1000.0d), getString(R.string.status_prefinished, Integer.valueOf(userStatus.getValidCount()))));
                this.f6881q.setMainProgress(1.0f);
                this.f6881q.setMainBonusProgress(1.0f);
            } else {
                boolean isPassed = userStatus.isPassed();
                Object[] objArr = new Object[4];
                objArr[0] = Double.valueOf(current / 1000.0d);
                objArr[1] = Double.valueOf(bonus / 1000.0d);
                objArr[2] = Double.valueOf(target / 1000.0d);
                objArr[3] = isPassed ? getString(R.string.status_finished) : getString(R.string.status_prefinished, Integer.valueOf(userStatus.getValidCount()));
                this.f6881q.setMainText(getString(R.string.status_main_distance, objArr));
                this.f6881q.setMainProgress((float) (current / target));
                this.f6881q.setMainBonusProgress((float) (d2 / target));
            }
        }
        this.f6881q.setReferenceTime(new Date());
    }

    public void setFragmentById(int i2, Fragment fragment) {
        if (i2 == 0) {
            if (fragment != null) {
                this.f6866b = (MapFragment) fragment;
                return;
            } else {
                this.f6866b = new MapFragment();
                return;
            }
        }
        if (i2 == 1) {
            if (fragment != null) {
                this.f6867c = (RecordListFragment) fragment;
            } else {
                this.f6867c = new RecordListFragment();
            }
            new RecordListPresenter(this.f6867c);
            return;
        }
        if (i2 != 2) {
            return;
        }
        if (fragment != null) {
            this.f6868d = (TaskListFragment) fragment;
        } else {
            this.f6868d = new TaskListFragment();
        }
        new TaskListPresenter(this.f6868d);
    }

    public void switchFromRunningToRecordList() {
        if (this.f6869e != this.f6866b) {
            LogUtil.e("Not in Map state.");
        }
        ((AppBarLayout) findViewById(R.id.v_main_appbar)).setExpanded(true);
        if (this.f6867c.isStateSaved()) {
            this.f6867c.getArguments().putBoolean("newRecord", true);
        } else {
            Bundle bundle = new Bundle();
            bundle.putBoolean("newRecord", true);
            this.f6867c.setArguments(bundle);
        }
        n0(this.f6867c, 0, R.anim.fade_in);
        ((NavigationView) findViewById(R.id.a_main_nav)).getMenu().findItem(R.id.nav_run_record).setChecked(true);
    }

    private void D(Weather weather) {
        if (weather.getNow() == null) {
            return;
        }
        String str = "https://image.nmc.cn/assets/img/w/40x40/4/" + nmcIconCode(weather.getNow().getCond().getCode()) + ".png";
        this.f6881q.setWeatherDrawable(Glide.with((FragmentActivity) this).load(str).placeholder(R.drawable.ic_autorenew_white_24dp).error(R.drawable.ic_cloud_off_black_24dp).centerCrop().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC));
        Glide.with((FragmentActivity) this).asBitmap().load(str).into(new c(100, 100));
    }

    private static String nmcIconCode(String code) {
        // Migrate weather objects cached by the former HeWeather adapter.
        if ("100".equals(code)) return "0";
        if ("101".equals(code)) return "1";
        if ("102".equals(code)) return "2";
        if ("103".equals(code)) return "3";
        if ("104".equals(code)) return "4";
        return code;
    }

    private void E() {
        boolean z2;
        Network.getMinVersion().observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.u0
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource K;
                K = MainActivity.this.K((Version) obj);
                return K;
            }
        }).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.f1
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                MainActivity.this.L((Boolean) obj);
            }
        }, new i1());
        if (Data.getUser() != null) {
            z2 = Data.getUser().isOffline().booleanValue();
        } else {
            z2 = false;
        }
        Network.getLatestVersion(z2).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.j1
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                MainActivity.this.M((Version) obj);
            }
        }, new i1());
    }

    private void G() {
        ServerException.setResources(getResources());
        this.f6876l = getSharedPreferences(IntroActivity.GuidePreferencesName, 0);
        Data.init(this).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.k1
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                MainActivity.this.N((Boolean) obj);
            }
        }, new Consumer() { // from class: cn.edu.pku.pkurunner.l1
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                MainActivity.this.O((Throwable) obj);
            }
        });
    }

    private void H() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.v_main_toolbar);
        setSupportActionBar(toolbar);
        this.f6873i = (DrawerLayout) findViewById(R.id.a_main_drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.a_main_nav);
        this.f6872h = navigationView;
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() { // from class: cn.edu.pku.pkurunner.m1
            @Override // com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
            public final boolean onNavigationItemSelected(MenuItem menuItem) {
                boolean g02;
                g02 = MainActivity.this.g0(menuItem);
                return g02;
            }
        });
        this.f6874j = (CoordinatorLayout) findViewById(R.id.a_main_coordinator);
        View headerView = this.f6872h.getHeaderView(0);
        this.f6879o = (TextView) headerView.findViewById(R.id.nav_txt_name);
        this.f6880p = (TextView) headerView.findViewById(R.id.nav_txt_department);
        ProgressableView progressableView = (ProgressableView) findViewById(R.id.v_status_progress);
        this.f6881q = progressableView;
        progressableView.setReferenceTime(new Date());
        d dVar = new d(this, this.f6873i, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        this.f6873i.addDrawerListener(dVar);
        dVar.syncState();
    }

    private void I() {
        j0();
        h0();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource K(Version version) {
        if (version.getVersion() > 652) {
            return UpdateHelper.showVersionLowDialog(this, version.getVersion(), ", ", "", Config.APK_URL, null, null, true);
        }
        return Observable.just(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void L(Boolean bool) {
        if (!bool.booleanValue()) {
            moveTaskToBack(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void M(Version version) {
        if (version.getVersion() > 652) {
            UpdateHelper.showVersionLowDialog(this, version.getVersion(), ", ", "", Config.APK_URL, null, null, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void R(DialogInterface dialogInterface, int i2) {
        dialogInterface.dismiss();
        this.f6866b.presenter.stopAndSwitchToIdle();
        moveTaskToBack(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void T(DialogInterface dialogInterface, int i2) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void W(UserStatus userStatus) {
        refreshUserStatusNotice();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void X(Boolean bool) {
        if (!bool.booleanValue()) {
            moveTaskToBack(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void b0(Boolean bool) {
        float f2 = getResources().getDisplayMetrics().density;
        TapTargetView.showFor(this, TapTarget.forBounds(new Rect((int) (16.0f * f2), (int) (504.0f * f2), (int) (32.0f * f2), (int) (f2 * 520.0f)), getString(R.string.g_main_t_feedback), getString(R.string.g_main_c_feedback)).transparentTarget(true).outerCircleColor(R.color.teal_500), new f());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void d0(Throwable th) {
        th.printStackTrace();
        this.f6875k.edit().remove("weather").remove("weather.nmc.v1").apply();
        sendBroadcast(new Intent("cn.edu.pku.pkurunner.MainActivity.weatherUpdate"));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean g0(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_announcement) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Network.announcementUrl)));
        } else if (itemId == R.id.nav_feedback) {
            e0();
        } else if (itemId == R.id.nav_logout) {
            f0();
        } else if (itemId == R.id.nav_run) {
            m0(this.f6866b);
        } else if (itemId == R.id.nav_run_record) {
            m0(this.f6867c);
        } else if (itemId == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (itemId == R.id.nav_tasks) {
            m0(this.f6868d);
        } else if (itemId == R.id.nav_update) {
            User user = Data.getUser();
            ClientUpdateNotice.downloadLatestVersion(this, user != null && user.isOffline().booleanValue());
        } else if (itemId == R.id.nav_weather) {
            startActivity(new Intent(this, WeatherActivity.class));
        }
        this.f6873i.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean k0() {
        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.v_main_fab_switch);
        if (floatingActionButton == null || floatingActionButton.getVisibility() != 0 || this.f6873i.isDrawerOpen(GravityCompat.START)) {
            return false;
        }
        TapTargetView.showFor(this, TapTarget.forView(floatingActionButton, getString(R.string.g_main_t_switch_to_running), getString(R.string.g_main_c_switch_to_running)).outerCircleColor(R.color.cyan_500).transparentTarget(true), new e());
        return true;
    }

    private boolean l0() {
        View findViewById = findViewById(R.id.f_recordlist_img);
        if (findViewById != null && findViewById.getVisibility() == 0) {
            TapTargetView.showFor(this, TapTarget.forView(findViewById, getString(R.string.g_main_t_pull_down), getString(R.string.g_main_c_pull_down)).outerCircleColor(R.color.orange_500).transparentTarget(true).targetRadius(96));
            return true;
        }
        return false;
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.a_main_drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        if (System.currentTimeMillis() - this.f6877m > 1500) {
            Toast.makeText(getApplicationContext(), R.string.a_main_exit_press_again, 0).show();
            this.f6877m = System.currentTimeMillis();
            return;
        }
        this.f6877m = 0L;
        if (this.f6866b.presenter.isRunning()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.a_main_exit_dialog_title);
            builder.setMessage(R.string.a_main_exit_dialog_content);
            builder.setPositiveButton(R.string.a_main_exit_dialog_positive, new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.a1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i2) {
                    MainActivity.this.R(dialogInterface, i2);
                }
            });
            builder.setNegativeButton(R.string.a_main_exit_dialog_negative, new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.b1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i2) {
                    dialogInterface.cancel();
                }
            });
            builder.create().show();
            return;
        }
        moveTaskToBack(true);
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        setTheme(R.style.BaseTheme);
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        i0(bundle);
        if (ThreatDetectUtil.isX86()) {
            new AlertDialog.Builder(this).setMessage("请勿使用模拟器运行本软件！").setPositiveButton("确定", new a()).setCancelable(false).create().show();
        }
        E();
        G();
        H();
        I();
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        this.f6866b = null;
        this.f6867c = null;
        this.f6868d = null;
        this.f6869e = null;
        unregisterReceiver(this.f6878n);
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.a_main_permission_management) {
            new PermissionDialog().show(getSupportFragmentManager(), "Permission dialog");
            return true;
        }
        if (itemId == R.id.a_main_clear_preferences) {
            this.f6876l.edit().remove("drawer").remove("fab").remove("pullDown").apply();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // androidx.appcompat.app.AppCompatActivity, android.app.Activity
    protected void onPostCreate(@Nullable Bundle bundle) {
        super.onPostCreate(bundle);
        ((NavigationView) findViewById(R.id.a_main_nav)).getMenu().findItem(R.id.nav_run_record).setChecked(true);
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        if (J()) {
            new AlertDialog.Builder(this).setTitle("非常事态！！！").setMessage("请不要在非手机的Android端使用本软件哦！\n小心用着用着设备飞出去哦！（大雾").setCancelable(false).setPositiveButton("原地爆炸", new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.n1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i2) {
                    MainActivity.this.T(dialogInterface, i2);
                }
            }).show();
        }
    }

    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putIntArray("FRAGMENT_ARRAY_KEY", this.f6870f);
        bundle.putInt("FRAGMENT_LAST_KEY", this.f6871g);
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onStart() {
        super.onStart();
        if (Data.getUser() == null) {
            return;
        }
        if (!Data.isValid()) {
            Data.loadByUser().subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.v0
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MainActivity.U((Boolean) obj);
                }
            }, new Consumer() { // from class: cn.edu.pku.pkurunner.w0
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MainActivity.this.V((Throwable) obj);
                }
            });
        }
        refreshUserStatusNotice();
        if (!Data.getUser().isOffline().booleanValue()) {
            Data.refreshUserStatus().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.x0
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MainActivity.this.W((UserStatus) obj);
                }
            }, new Consumer() { // from class: cn.edu.pku.pkurunner.y0
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MainActivity.this.a0((Throwable) obj);
                }
            });
        } else {
            this.f6881q.setActiveMode(true);
            this.f6881q.setCollapseMode(true);
            double d2 = 0.0d;
            Iterator<Record> records = Data.getUser().getRecords().iterator();
            while (records.hasNext()) {
                d2 += records.next().getDistance();
            }
            this.f6881q.setMainText(getString(R.string.status_offline, Integer.valueOf((int) (d2 / 1000.0d))));
        }
        if (this.f6869e == null) {
            m0(this.f6867c);
        }
        if (this.f6869e == this.f6867c && this.f6876l.getBoolean("drawer", true)) {
            this.f6876l.edit().putBoolean("drawer", false).apply();
            this.f6873i.openDrawer(GravityCompat.START, true);
            Observable.just(Boolean.TRUE).delay(300L, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.z0
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MainActivity.this.b0((Boolean) obj);
                }
            });
        } else if (this.f6869e == this.f6867c && this.f6876l.getBoolean("fab", true)) {
            if (k0()) {
                this.f6876l.edit().putBoolean("fab", false).apply();
            }
        } else if (this.f6869e == this.f6867c && this.f6876l.getBoolean("pullDown", true) && l0()) {
            this.f6876l.edit().putBoolean("pullDown", false).apply();
        }
    }

    public void switchFromRecordListToRunning() {
        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.v_main_fab_switch);
        if (this.f6866b.isStateSaved()) {
            this.f6866b.getArguments().putInt("FabX", (int) (floatingActionButton.getX() + (floatingActionButton.getWidth() / 2)));
            this.f6866b.getArguments().putInt("FabY", (int) (floatingActionButton.getY() + (floatingActionButton.getHeight() / 2)));
        } else {
            Bundle bundle = new Bundle();
            bundle.putInt("FabX", (int) (floatingActionButton.getX() + (floatingActionButton.getWidth() / 2)));
            bundle.putInt("FabY", (int) (floatingActionButton.getY() + (floatingActionButton.getHeight() / 2)));
            this.f6866b.setArguments(bundle);
        }
        n0(this.f6866b, R.anim.fade_out, 0);
        this.f6866b.shouldRevealAnimation = true;
        ((NavigationView) findViewById(R.id.a_main_nav)).getMenu().findItem(R.id.nav_run).setChecked(true);
    }
}
