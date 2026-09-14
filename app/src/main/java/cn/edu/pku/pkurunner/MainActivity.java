package cn.edu.pku.pkurunner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
import cn.edu.pku.pkurunner.View.RunningFabView;
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

    private MapFragment mapFragment;

    private RecordListFragment recordListFragment;

    private TaskListFragment taskListFragment;

    private Fragment currentFragment;

    private NavigationView navigationView;

    private DrawerLayout drawerLayout;

    private CoordinatorLayout coordinatorLayout;

    private SharedPreferences weatherPreferences;

    private SharedPreferences guidePreferences;

    private WeatherUpdateReceiver weatherUpdateReceiver;

    private TextView navUserNameText;

    private TextView navDepartmentText;

    private ProgressableView progressableView;

    private int[] fragmentAttachedFlags = new int[3];

    private int lastFragmentId = -1;

    private long lastBackPressTime = 0;

    class EmulatorWarningClickListener implements DialogInterface.OnClickListener {
        EmulatorWarningClickListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int index) {
            PerfectExitUtil.exit();
        }
    }

    class WeatherObserver implements Observer<Weather> {
        @Override
        public void onComplete() {
        }

        @Override
        public void onSubscribe(Disposable disposable) {
        }

        WeatherObserver() {
        }

        @Override
        public void onNext(Weather weather) {
            MainActivity.this.F(weather);
        }

        @Override
        public void onError(Throwable th) {
            MainActivity mainActivity = MainActivity.this;
            Toast.makeText(mainActivity, mainActivity.getString(R.string.a_main_error_loading_weather, th.getMessage()), 0).show();
        }
    }

    class NavHeaderBitmapTarget extends SimpleTarget<Bitmap> {
        NavHeaderBitmapTarget(int index, int index2) {
            super(index, index2);
        }

        @Override
        public void onResourceReady(Bitmap bitmap, Transition transition) {
            MainActivity.this.navigationView.getMenu().findItem(R.id.nav_weather).setIcon(new BitmapDrawable(MainActivity.this.getResources(), bitmap));
        }
    }

    class MainDrawerToggle extends ActionBarDrawerToggle {
        MainDrawerToggle(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar, int index, int index2) {
            super(activity, drawerLayout, toolbar, index, index2);
        }

        @Override
        public void onDrawerOpened(View view) {
            super.onDrawerOpened(view);
            MainActivity.this.refreshUserStatusNotice();
        }
    }

    class SwitchToRunningGuideListener extends TapTargetView.Listener {
        SwitchToRunningGuideListener() {
        }

        @Override
        public void onTargetClick(TapTargetView tapTargetView) {
            super.onTargetClick(tapTargetView);
            MainActivity.this.switchFromRecordListToRunning();
        }
    }

    class FeedbackGuideListener extends TapTargetView.Listener {
        FeedbackGuideListener() {
        }

        @Override
        public void onOuterCircleClick(TapTargetView tapTargetView) {
            super.onOuterCircleClick(tapTargetView);
            tapTargetView.dismiss(false);
        }

        @Override
        public void onTargetClick(TapTargetView tapTargetView) {
            super.onTargetClick(tapTargetView);
            MainActivity.this.e0();
        }
    }

    public /* synthetic */ void O(Throwable th) {
        Toast.makeText(this, getString(R.string.a_main_error_loading_data, th.getMessage()), 0).show();
    }

    public static /* synthetic */ void Q(DialogInterface dialogInterface, int index) {
    }

    public static /* synthetic */ void U(Boolean bool) {
    }

    public /* synthetic */ void V(Throwable th) {
        Toast.makeText(this, getString(R.string.a_main_error_loading_data, th.getMessage()), 0).show();
    }

    public static /* synthetic */ void Y(Boolean bool) {
    }

    public /* synthetic */ void Z(Throwable th, Void r3) {
        Toast.makeText(this, getString(R.string.a_main_error_loading_status, th.getMessage()), 0).show();
    }

    private void i0(Bundle bundle) {
        if (bundle == null) {
            for (int index = 0; index < 3; index++) {
                this.fragmentAttachedFlags[index] = 0;
            }
            this.mapFragment = new MapFragment();
            RecordListFragment recordListFragment = new RecordListFragment();
            this.recordListFragment = recordListFragment;
            new RecordListPresenter(recordListFragment);
            TaskListFragment taskListFragment = new TaskListFragment();
            this.taskListFragment = taskListFragment;
            new TaskListPresenter(taskListFragment);
            this.currentFragment = null;
            this.lastFragmentId = -1;
            return;
        }
        LogUtil.d("onCreate have instance");
        this.fragmentAttachedFlags = bundle.getIntArray("FRAGMENT_ARRAY_KEY");
        this.lastFragmentId = bundle.getInt("FRAGMENT_LAST_KEY");
        LogUtil.d("lastTag: " + String.valueOf(this.lastFragmentId));
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        FragmentTransaction beginTransaction = supportFragmentManager.beginTransaction();
        for (int index2 = 0; index2 < 3; index2++) {
            if (this.fragmentAttachedFlags[index2] == 1) {
                Fragment findFragmentByTag = supportFragmentManager.findFragmentByTag(String.valueOf(index2));
                if (this.lastFragmentId != index2) {
                    beginTransaction.hide(findFragmentByTag);
                } else {
                    this.currentFragment = findFragmentByTag;
                }
                setFragmentById(index2, findFragmentByTag);
            } else {
                setFragmentById(index2, null);
            }
        }
        beginTransaction.commit();
    }

    public void F(Weather weather) {
        this.weatherPreferences.edit().putString("weather.nmc.v1", SerializeHelper.objectToString(weather)).apply();
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

    public /* synthetic */ void N(Boolean bool) {
        if (this.guidePreferences.getBoolean(IntroActivity.GuidePreferencesKey, false)) {
            if (Data.getUser() == null) {
                startActivity(new Intent(this, (Class<?>) LoginActivity.class));
            }
        } else {
            Intent intent = new Intent(this, (Class<?>) IntroActivity.class);
            intent.putExtra("jumpToLogin", true);
            startActivity(intent);
        }
    }

    public /* synthetic */ void P(DialogInterface dialogInterface, int index) {
        Intent intent = new Intent(this, (Class<?>) LoginActivity.class);
        intent.putExtra("logout", true);
        startActivity(intent);
    }

    public /* synthetic */ void a0(final Throwable th) {
        if ((th instanceof ServerException) && ((ServerException) th).getErrorCode() == 15) {
            ClientUpdateNotice.showVersionLowDialog(this).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MainActivity.this.X((Boolean) obj);
                }
            });
        } else if ((th instanceof HttpException) && ((HttpException) th).code() == 401) {
            TokenInvalidNotice.showTokenInvalidDialog(this).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MainActivity.Y((Boolean) obj);
                }
            });
        } else {
            Network.interceptIfSocketTimeout(th, new Callback.Callable() {
                @Override
                public final void call(Object obj) {
                    MainActivity.this.Z(th, (Void) obj);
                }
            });
        }
    }

    public /* synthetic */ void c0(Serializable serializable) {
        Weather weather = (Weather) serializable;
        Network.weather = weather;
        D(weather);
    }

    public void e0() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(Uri.parse(Config.FEEDBACK_URL));
        startActivity(intent);
    }

    private void f0() {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.a_main_logout_dialog_title).setMessage(R.string.a_main_logout_dialog_content).setPositiveButton(R.string.a_main_logout_dialog_positive, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index) {
                MainActivity.this.P(dialogInterface, index);
            }
        }).setNegativeButton(R.string.a_main_logout_dialog_negative, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index) {
                MainActivity.Q(dialogInterface, index);
            }
        }).setCancelable(true).create().show();
    }

    private void h0() {
        this.weatherUpdateReceiver = new WeatherUpdateReceiver(new WeatherObserver());
        registerReceiver(this.weatherUpdateReceiver, new IntentFilter("cn.edu.pku.pkurunner.MainActivity.weatherUpdate"));
    }

    private void j0() {
        SharedPreferences sharedPreferences = getSharedPreferences("WeatherService", 0);
        this.weatherPreferences = sharedPreferences;
        if (sharedPreferences.contains("weather.nmc.v1")) {
            String string = this.weatherPreferences.getString("weather.nmc.v1", null);
            if (string == null) {
                Network.weather = null;
            } else {
                SerializeHelper.stringToObject(string).subscribe(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        MainActivity.this.c0((Serializable) obj);
                    }
                }, new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        MainActivity.this.d0((Throwable) obj);
                    }
                });
            }
        }
    }

    private void m0(Fragment fragment) {
        Fragment fragment2 = this.currentFragment;
        if (fragment2 != fragment) {
            this.currentFragment = fragment;
            updateRunningFab(fragment);
            FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
            if (fragment2 != null) {
                beginTransaction.hide(fragment2);
            }
            int findIdByFragment = findIdByFragment(fragment);
            if (fragment.isAdded()) {
                beginTransaction.show(fragment);
            } else {
                this.fragmentAttachedFlags[findIdByFragment] = 1;
                beginTransaction.add(R.id.v_main_frame, fragment, String.valueOf(findIdByFragment));
            }
            beginTransaction.commit();
            this.lastFragmentId = findIdByFragment;
        }
    }

    private void n0(Fragment fragment, int index, int index2) {
        Fragment fragment2 = this.currentFragment;
        if (fragment2 != fragment) {
            this.currentFragment = fragment;
            updateRunningFab(fragment);
            FragmentTransaction customAnimations = getSupportFragmentManager().beginTransaction().setCustomAnimations(index2, index);
            if (fragment2 != null) {
                customAnimations.hide(fragment2);
            }
            int findIdByFragment = findIdByFragment(fragment);
            if (fragment.isAdded()) {
                customAnimations.show(fragment).commit();
            } else {
                this.fragmentAttachedFlags[findIdByFragment] = 1;
                customAnimations.add(R.id.v_main_frame, fragment, String.valueOf(findIdByFragment)).commit();
            }
            this.lastFragmentId = findIdByFragment;
        }
    }

    /**
     * The "start running" FAB only makes sense while browsing records / tasks -- on the running
     * map it would sit on top of the map for no reason. The map also collapses the AppBar, so
     * leaving it has to expand the hero card again, otherwise the FAB would be re-shown while its
     * anchor is still scrolled out of view.
     */
    private void updateRunningFab(Fragment fragment) {
        View fab = findViewById(R.id.v_main_fab_switch);
        AppBarLayout appBar = (AppBarLayout) findViewById(R.id.v_main_appbar);
        if (fragment == this.mapFragment) {
            if (fab != null) {
                fab.setVisibility(View.GONE);
            }
            return;
        }
        if (appBar != null) {
            appBar.setExpanded(true, false);
        }
        if (fab != null) {
            fab.setVisibility(View.VISIBLE);
            if (fab instanceof RunningFabView) {
                ((RunningFabView) fab).show();
            }
        }
    }

    public int findIdByFragment(Fragment fragment) {
        if (fragment.equals(this.mapFragment)) {
            return 0;
        }
        if (fragment.equals(this.recordListFragment)) {
            return 1;
        }
        return fragment.equals(this.taskListFragment) ? 2 : -1;
    }

    public void refreshUserStatusNotice() {
        User user = Data.getUser();
        if (user == null) {
            this.navUserNameText.setText("");
            this.navDepartmentText.setText("");
        } else {
            this.navUserNameText.setText(user.getName());
            this.navDepartmentText.setText(user.getDepartment());
        }
        if (user == null || user.isOffline().booleanValue()) {
            return;
        }
        UserStatus userStatus = Data.getUserStatus();
        if (userStatus == null) {
            this.progressableView.reset();
            return;
        }
        double current = userStatus.getCurrent();
        double bonus = userStatus.getBonus();
        double value = current + bonus;
        double target = userStatus.getTarget();
        int time = (int) ((((userStatus.getEndDate().getTime() - userStatus.getBeginDate().getTime()) / 1000) / 3600) / 24);
        int time2 = (int) ((((userStatus.getEndDate().getTime() - System.currentTimeMillis()) / 1000) / 3600) / 24);
        this.progressableView.setCollapseMode(false);
        if (System.currentTimeMillis() < userStatus.getBeginDate().getTime()) {
            this.progressableView.setActiveMode(false);
            this.progressableView.setSleepingIndicatorText(getString(R.string.status_pre_duration, getString(R.string.status_inactive_prompt), Integer.valueOf(((int) Math.ceil((((userStatus.getBeginDate().getTime() - System.currentTimeMillis()) / 1000) / 3600) / 24)) + 1), new SimpleDateFormat().format(Long.valueOf(userStatus.getBeginDate().getTime()))));
        } else {
            this.progressableView.setActiveMode(true);
            int index = time - time2;
            this.progressableView.setSecondaryProgress(index / (time + BitmapDescriptorFactory.HUE_RED));
            if (System.currentTimeMillis() <= userStatus.getEndDate().getTime()) {
                this.progressableView.setSecondaryText(getString(R.string.status_main_day, Integer.valueOf(Math.min(index, time)), Integer.valueOf(time)));
            } else {
                this.progressableView.setSecondaryText(getString(R.string.status_post_duration, new SimpleDateFormat().format(Long.valueOf(userStatus.getEndDate().getTime()))));
            }
            if (target == 0.0d) {
                this.progressableView.setMainValue(getString(R.string.or_hero_value_plain, Double.valueOf(current / 1000.0d)));
                this.progressableView.setMainUnit(getString(R.string.v_record_km));
                this.progressableView.setMainCaption(getString(R.string.status_prefinished, Integer.valueOf(userStatus.getValidCount())));
                this.progressableView.setMainProgress(1.0f);
                this.progressableView.setMainBonusProgress(1.0f);
            } else {
                boolean isPassed = userStatus.isPassed();
                this.progressableView.setMainValue(getString(R.string.or_hero_value_plain, Double.valueOf(current / 1000.0d)));
                this.progressableView.setMainUnit(getString(R.string.or_hero_unit, Double.valueOf(target / 1000.0d)));
                this.progressableView.setMainCaption(isPassed ? getString(R.string.status_finished) : getString(R.string.status_prefinished, Integer.valueOf(userStatus.getValidCount())));
                this.progressableView.setMainProgress((float) (current / target));
                this.progressableView.setMainBonusProgress((float) (value / target));
            }
        }
        this.progressableView.setReferenceTime(new Date());
    }

    public void setFragmentById(int index, Fragment fragment) {
        if (index == 0) {
            if (fragment != null) {
                this.mapFragment = (MapFragment) fragment;
                return;
            } else {
                this.mapFragment = new MapFragment();
                return;
            }
        }
        if (index == 1) {
            if (fragment != null) {
                this.recordListFragment = (RecordListFragment) fragment;
            } else {
                this.recordListFragment = new RecordListFragment();
            }
            new RecordListPresenter(this.recordListFragment);
            return;
        }
        if (index != 2) {
            return;
        }
        if (fragment != null) {
            this.taskListFragment = (TaskListFragment) fragment;
        } else {
            this.taskListFragment = new TaskListFragment();
        }
        new TaskListPresenter(this.taskListFragment);
    }

    public void switchFromRunningToRecordList() {
        if (this.currentFragment != this.mapFragment) {
            LogUtil.e("Not in Map state.");
        }
        ((AppBarLayout) findViewById(R.id.v_main_appbar)).setExpanded(true);
        if (this.recordListFragment.isStateSaved()) {
            this.recordListFragment.getArguments().putBoolean("newRecord", true);
        } else {
            Bundle bundle = new Bundle();
            bundle.putBoolean("newRecord", true);
            this.recordListFragment.setArguments(bundle);
        }
        n0(this.recordListFragment, 0, R.anim.fade_in);
        ((NavigationView) findViewById(R.id.a_main_nav)).getMenu().findItem(R.id.nav_run_record).setChecked(true);
    }

    private void D(Weather weather) {
        if (weather.getNow() == null) {
            return;
        }
        String str = "https://image.nmc.cn/assets/img/w/40x40/4/" + nmcIconCode(weather.getNow().getCond().getCode()) + ".png";
        this.progressableView.setWeatherDrawable(Glide.with((FragmentActivity) this).load(str).placeholder(R.drawable.ic_autorenew_white_24dp).error(R.drawable.ic_cloud_off_black_24dp).centerCrop().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC));
        Glide.with((FragmentActivity) this).asBitmap().load(str).into(new NavHeaderBitmapTarget(100, 100));
    }

    private static String nmcIconCode(String code) {
        if ("100".equals(code)) return "0";
        if ("101".equals(code)) return "1";
        if ("102".equals(code)) return "2";
        if ("103".equals(code)) return "3";
        if ("104".equals(code)) return "4";
        return code;
    }

    private void E() {
        boolean z2;
        Network.getMinVersion().observeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource K;
                K = MainActivity.this.K((Version) obj);
                return K;
            }
        }).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                MainActivity.this.L((Boolean) obj);
            }
        }, new StackTracePrintingConsumer());
        if (Data.getUser() != null) {
            z2 = Data.getUser().isOffline().booleanValue();
        } else {
            z2 = false;
        }
        Network.getLatestVersion(z2).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                MainActivity.this.M((Version) obj);
            }
        }, new StackTracePrintingConsumer());
    }

    private void G() {
        ServerException.setResources(getResources());
        this.guidePreferences = getSharedPreferences(IntroActivity.GuidePreferencesName, 0);
        Data.init(this).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                MainActivity.this.N((Boolean) obj);
            }
        }, new Consumer() {
            @Override
            public final void accept(Object obj) {
                MainActivity.this.O((Throwable) obj);
            }
        });
    }

    private void H() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.v_main_toolbar);
        setSupportActionBar(toolbar);
        this.drawerLayout = (DrawerLayout) findViewById(R.id.a_main_drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.a_main_nav);
        this.navigationView = navigationView;
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public final boolean onNavigationItemSelected(MenuItem menuItem) {
                boolean g02;
                g02 = MainActivity.this.g0(menuItem);
                return g02;
            }
        });
        this.coordinatorLayout = (CoordinatorLayout) findViewById(R.id.a_main_coordinator);
        View headerView = this.navigationView.getHeaderView(0);
        this.navUserNameText = (TextView) headerView.findViewById(R.id.nav_txt_name);
        this.navDepartmentText = (TextView) headerView.findViewById(R.id.nav_txt_department);
        ProgressableView progressableView = (ProgressableView) findViewById(R.id.v_status_progress);
        this.progressableView = progressableView;
        progressableView.setReferenceTime(new Date());
        MainDrawerToggle drawerToggle = new MainDrawerToggle(this, this.drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        this.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    private void I() {
        j0();
        h0();
    }

    public /* synthetic */ ObservableSource K(Version version) {
        if (version.getVersion() > 652) {
            return UpdateHelper.showVersionLowDialog(this, version.getVersion(), ", ", "", Config.APK_URL, null, null, true);
        }
        return Observable.just(Boolean.TRUE);
    }

    public /* synthetic */ void L(Boolean bool) {
        if (!bool.booleanValue()) {
            moveTaskToBack(true);
        }
    }

    public /* synthetic */ void M(Version version) {
        if (version.getVersion() > 652) {
            UpdateHelper.showVersionLowDialog(this, version.getVersion(), ", ", "", Config.APK_URL, null, null, false);
        }
    }

    public /* synthetic */ void R(DialogInterface dialogInterface, int index) {
        dialogInterface.dismiss();
        this.mapFragment.presenter.stopAndSwitchToIdle();
        moveTaskToBack(true);
    }

    public /* synthetic */ void T(DialogInterface dialogInterface, int index) {
        finish();
    }

    public /* synthetic */ void W(UserStatus userStatus) {
        refreshUserStatusNotice();
    }

    public /* synthetic */ void X(Boolean bool) {
        if (!bool.booleanValue()) {
            moveTaskToBack(true);
        }
    }

    public /* synthetic */ void b0(Boolean bool) {
        float value2 = getResources().getDisplayMetrics().density;
        TapTargetView.showFor(this, TapTarget.forBounds(new Rect((int) (16.0f * value2), (int) (504.0f * value2), (int) (32.0f * value2), (int) (value2 * 520.0f)), getString(R.string.g_main_t_feedback), getString(R.string.g_main_c_feedback)).transparentTarget(true).outerCircleColor(R.color.teal_500), new FeedbackGuideListener());
    }

    public /* synthetic */ void d0(Throwable th) {
        th.printStackTrace();
        this.weatherPreferences.edit().remove("weather").remove("weather.nmc.v1").apply();
        sendBroadcast(new Intent("cn.edu.pku.pkurunner.MainActivity.weatherUpdate"));
    }

    public boolean g0(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_announcement) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Network.announcementUrl)));
        } else if (itemId == R.id.nav_feedback) {
            e0();
        } else if (itemId == R.id.nav_logout) {
            f0();
        } else if (itemId == R.id.nav_run) {
            m0(this.mapFragment);
        } else if (itemId == R.id.nav_run_record) {
            m0(this.recordListFragment);
        } else if (itemId == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (itemId == R.id.nav_tasks) {
            m0(this.taskListFragment);
        } else if (itemId == R.id.nav_update) {
            User user = Data.getUser();
            ClientUpdateNotice.downloadLatestVersion(this, user != null && user.isOffline().booleanValue());
        } else if (itemId == R.id.nav_weather) {
            startActivity(new Intent(this, WeatherActivity.class));
        }
        this.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean k0() {
        View floatingActionButton = findViewById(R.id.v_main_fab_switch);
        if (floatingActionButton == null || floatingActionButton.getVisibility() != 0 || this.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            return false;
        }
        TapTargetView.showFor(this, TapTarget.forView(floatingActionButton, getString(R.string.g_main_t_switch_to_running), getString(R.string.g_main_c_switch_to_running)).outerCircleColor(R.color.cyan_500).transparentTarget(true), new SwitchToRunningGuideListener());
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

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.a_main_drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        if (System.currentTimeMillis() - this.lastBackPressTime > 1500) {
            Toast.makeText(getApplicationContext(), R.string.a_main_exit_press_again, 0).show();
            this.lastBackPressTime = System.currentTimeMillis();
            return;
        }
        this.lastBackPressTime = 0L;
        if (this.mapFragment.presenter.isRunning()) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.a_main_exit_dialog_title);
            builder.setMessage(R.string.a_main_exit_dialog_content);
            builder.setPositiveButton(R.string.a_main_exit_dialog_positive, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int index) {
                    MainActivity.this.R(dialogInterface, index);
                }
            });
            builder.setNegativeButton(R.string.a_main_exit_dialog_negative, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int index) {
                    dialogInterface.cancel();
                }
            });
            builder.create().show();
            return;
        }
        moveTaskToBack(true);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        setTheme(R.style.BaseTheme);
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        i0(bundle);
        if (ThreatDetectUtil.isX86()) {
            new MaterialAlertDialogBuilder(this).setMessage("请勿使用模拟器运行本软件！").setPositiveButton("确定", new EmulatorWarningClickListener()).setCancelable(false).create().show();
        }
        E();
        G();
        H();
        I();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mapFragment = null;
        this.recordListFragment = null;
        this.taskListFragment = null;
        this.currentFragment = null;
        unregisterReceiver(this.weatherUpdateReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.a_main_permission_management) {
            new PermissionDialog().show(getSupportFragmentManager(), "Permission dialog");
            return true;
        }
        if (itemId == R.id.a_main_clear_preferences) {
            this.guidePreferences.edit().remove("drawer").remove("fab").remove("pullDown").apply();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle bundle) {
        super.onPostCreate(bundle);
        ((NavigationView) findViewById(R.id.a_main_nav)).getMenu().findItem(R.id.nav_run_record).setChecked(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (J()) {
            new MaterialAlertDialogBuilder(this).setTitle("非常事态！！！").setMessage("请不要在非手机的Android端使用本软件哦！\n小心用着用着设备飞出去哦！（大雾").setCancelable(false).setPositiveButton("原地爆炸", new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int index) {
                    MainActivity.this.T(dialogInterface, index);
                }
            }).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putIntArray("FRAGMENT_ARRAY_KEY", this.fragmentAttachedFlags);
        bundle.putInt("FRAGMENT_LAST_KEY", this.lastFragmentId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Data.getUser() == null) {
            return;
        }
        if (!Data.isValid()) {
            Data.loadByUser().subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MainActivity.U((Boolean) obj);
                }
            }, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MainActivity.this.V((Throwable) obj);
                }
            });
        }
        refreshUserStatusNotice();
        if (!Data.getUser().isOffline().booleanValue()) {
            Data.refreshUserStatus().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MainActivity.this.W((UserStatus) obj);
                }
            }, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MainActivity.this.a0((Throwable) obj);
                }
            });
        } else {
            this.progressableView.setActiveMode(true);
            this.progressableView.setCollapseMode(true);
            double value = 0.0d;
            Iterator<Record> records = Data.getUser().getRecords().iterator();
            while (records.hasNext()) {
                value += records.next().getDistance();
            }
            this.progressableView.setMainValue(getString(R.string.or_hero_value_plain, Double.valueOf(value / 1000.0d)));
            this.progressableView.setMainUnit(getString(R.string.v_record_km));
            this.progressableView.setMainCaption(getString(R.string.status_offline, Integer.valueOf((int) (value / 1000.0d))));
        }
        if (this.currentFragment == null) {
            m0(this.recordListFragment);
        }
        if (this.currentFragment == this.recordListFragment && this.guidePreferences.getBoolean("drawer", true)) {
            this.guidePreferences.edit().putBoolean("drawer", false).apply();
            this.drawerLayout.openDrawer(GravityCompat.START, true);
            Observable.just(Boolean.TRUE).delay(300L, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MainActivity.this.b0((Boolean) obj);
                }
            });
        } else if (this.currentFragment == this.recordListFragment && this.guidePreferences.getBoolean("fab", true)) {
            if (k0()) {
                this.guidePreferences.edit().putBoolean("fab", false).apply();
            }
        } else if (this.currentFragment == this.recordListFragment && this.guidePreferences.getBoolean("pullDown", true) && l0()) {
            this.guidePreferences.edit().putBoolean("pullDown", false).apply();
        }
    }

    public void switchFromRecordListToRunning() {
        View floatingActionButton = findViewById(R.id.v_main_fab_switch);
        if (this.mapFragment.isStateSaved()) {
            this.mapFragment.getArguments().putInt("FabX", (int) (floatingActionButton.getX() + (floatingActionButton.getWidth() / 2)));
            this.mapFragment.getArguments().putInt("FabY", (int) (floatingActionButton.getY() + (floatingActionButton.getHeight() / 2)));
        } else {
            Bundle bundle = new Bundle();
            bundle.putInt("FabX", (int) (floatingActionButton.getX() + (floatingActionButton.getWidth() / 2)));
            bundle.putInt("FabY", (int) (floatingActionButton.getY() + (floatingActionButton.getHeight() / 2)));
            this.mapFragment.setArguments(bundle);
        }
        n0(this.mapFragment, R.anim.fade_out, 0);
        this.mapFragment.shouldRevealAnimation = true;
        ((NavigationView) findViewById(R.id.a_main_nav)).getMenu().findItem(R.id.nav_run).setChecked(true);
    }
}
