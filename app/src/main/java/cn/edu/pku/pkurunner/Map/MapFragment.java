package cn.edu.pku.pkurunner.Map;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.Group;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import cn.edu.pku.pkurunner.MainActivity;
import cn.edu.pku.pkurunner.Map.MapContract;
import cn.edu.pku.pkurunner.Map.MapFragment;
import cn.edu.pku.pkurunner.Map.SpeedHelper;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Service.NotificationDisplayService;
import cn.edu.pku.pkurunner.Utils.KeepAliveUtil;
import cn.edu.pku.pkurunner.View.GPSInfoView;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import org.xutils.common.util.LogUtil;

public class MapFragment extends Fragment implements MapContract.View {

    /* renamed from: d, reason: collision with root package name */
    private MapView f6903d;

    /* renamed from: e, reason: collision with root package name */
    private AMap f6904e;

    /* renamed from: f, reason: collision with root package name */
    private MyLocationStyle f6905f;

    /* renamed from: g, reason: collision with root package name */
    private Button f6906g;

    /* renamed from: h, reason: collision with root package name */
    private Button f6907h;

    /* renamed from: i, reason: collision with root package name */
    private TextView f6908i;

    /* renamed from: j, reason: collision with root package name */
    private TextView f6909j;

    /* renamed from: k, reason: collision with root package name */
    private TextView f6910k;

    /* renamed from: l, reason: collision with root package name */
    private TextView f6911l;

    /* renamed from: m, reason: collision with root package name */
    private TextView f6912m;

    /* renamed from: n, reason: collision with root package name */
    private TextView f6913n;

    /* renamed from: o, reason: collision with root package name */
    private Group f6914o;

    /* renamed from: p, reason: collision with root package name */
    private GPSInfoView f6915p;

    /* renamed from: q, reason: collision with root package name */
    private ProgressDialog f6916q;

    /* renamed from: r, reason: collision with root package name */
    private View f6917r;

    /* renamed from: s, reason: collision with root package name */
    private View f6918s;

    /* renamed from: t, reason: collision with root package name */
    private BottomSheetBehavior f6919t;

    /* renamed from: u, reason: collision with root package name */
    private int f6920u;

    /* renamed from: v, reason: collision with root package name */
    private PowerManager f6921v;

    /* renamed from: w, reason: collision with root package name */
    private PowerManager.WakeLock f6922w;

    /* renamed from: x, reason: collision with root package name */
    private LocationSource.OnLocationChangedListener f6923x;
    public boolean shouldRevealAnimation = false;
    public MapContract.Presenter presenter = new MapPresenter(this);

    class a implements LocationSource {
        a() {
        }

        @Override // com.amap.api.maps2d.LocationSource
        public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
            MapFragment.this.f6923x = onLocationChangedListener;
        }

        @Override // com.amap.api.maps2d.LocationSource
        public void deactivate() {
            MapFragment.this.f6923x = null;
        }
    }

    class b implements Animator.AnimatorListener {
        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
        }

        b() {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            MapFragment.this.f6918s.setBackground(null);
            MapFragment.this.f6915p.setAppear(false);
        }
    }

    class c implements Animator.AnimatorListener {

        /* renamed from: a, reason: collision with root package name */
        final /* synthetic */ d f6926a;

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
        }

        c(d dVar) {
            this.f6926a = dVar;
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            this.f6926a.a();
            MapFragment.this.f6918s.setBackground(null);
        }
    }

    interface d {
        void a();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public AMap getAMap() {
        return this.f6904e;
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public LocationSource.OnLocationChangedListener getLocationListener() {
        return this.f6923x;
    }

    @Override // cn.edu.pku.pkurunner.Contract.BaseView
    public void setPresenter(@NonNull MapContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void unregisterMapCenterHelper() {
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void updateTextView(double d2, double d3, double d4, boolean z2) {
        if (z2) {
            this.f6910k.setText(getString(R.string.f_map_format_speed_sci, Double.valueOf(d4)));
            this.f6908i.setText(getString(R.string.f_map_format_distance_sci, Double.valueOf(SpeedHelper.meterToPlanckLength(d2 * 1000.0d))));
            this.f6909j.setText(getString(R.string.f_map_format_time_sci, Double.valueOf(SpeedHelper.secondToPlanckTime(d3))));
            return;
        }
        this.f6910k.setText(getString(R.string.f_map_format_speed, Double.valueOf(d4)));
        this.f6908i.setText(getString(R.string.f_map_format_distance, Float.valueOf(((int) (d2 * 10.0d)) / 10.0f)));
        int i2 = (int) (d3 / 3600.0d);
        double d5 = d3 - (i2 * 3600);
        int i3 = (int) (d5 / 60.0d);
        int i4 = (int) (d5 - (i3 * 60));
        this.f6909j.setText(i2 == 0 ? getString(R.string.f_map_format_time_short, Integer.valueOf(i3), Integer.valueOf(i4)) : getString(R.string.f_map_format_time_long, Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void A(final ObservableEmitter observableEmitter) {
        this.f6904e.setOnMapTouchListener(new AMap.OnMapTouchListener() { // from class: r.h
            @Override // com.amap.api.maps2d.AMap.OnMapTouchListener
            public final void onTouch(MotionEvent motionEvent) {
                MapFragment.z(observableEmitter, motionEvent);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void B(ValueAnimator valueAnimator) {
        this.f6918s.setBackgroundColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
    }

    private void D(d dVar) {
        int sqrt = (int) Math.sqrt(Math.pow(this.f6917r.getWidth(), 2.0d) + Math.pow(this.f6917r.getHeight(), 2.0d));
        Bundle arguments = getArguments();
        int width = this.f6917r.getWidth();
        if (arguments != null) {
            width = arguments.getInt("FabX", width);
        }
        Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(this.f6917r, width, 0, sqrt, BitmapDescriptorFactory.HUE_RED);
        createCircularReveal.setInterpolator(new AccelerateInterpolator());
        createCircularReveal.setDuration(1000L);
        createCircularReveal.start();
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setIntValues(getResources().getColor(R.color.red_400), getResources().getColor(R.color.primary));
        valueAnimator.setEvaluator(new ArgbEvaluator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: r.g
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MapFragment.this.y(valueAnimator2);
            }
        });
        valueAnimator.setDuration(1000L);
        valueAnimator.addListener(new c(dVar));
        this.f6918s.setBackgroundResource(R.color.primary);
        valueAnimator.start();
    }

    private void E() {
        int sqrt = (int) Math.sqrt(Math.pow(this.f6917r.getWidth(), 2.0d) + Math.pow(this.f6917r.getHeight(), 2.0d));
        Bundle arguments = getArguments();
        int width = this.f6917r.getWidth();
        if (arguments != null) {
            width = arguments.getInt("FabX", width);
        }
        Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(this.f6917r, width, 0, BitmapDescriptorFactory.HUE_RED, sqrt);
        createCircularReveal.setInterpolator(new AccelerateInterpolator());
        createCircularReveal.setDuration(1000L);
        createCircularReveal.start();
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setIntValues(getResources().getColor(R.color.primary), getResources().getColor(R.color.red_400));
        valueAnimator.setEvaluator(new ArgbEvaluator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: r.e
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MapFragment.this.B(valueAnimator2);
            }
        });
        valueAnimator.setDuration(500L);
        valueAnimator.addListener(new b());
        this.f6918s.setBackgroundResource(R.color.primary);
        valueAnimator.setStartDelay(500L);
        valueAnimator.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void t(View view) {
        this.presenter.onFabRunClick(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean u(View view) {
        return this.presenter.onFabRunClick(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void v(View view) {
        this.presenter.onFabPauseClick(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean w(View view) {
        return this.presenter.onFabPauseClick(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void y(ValueAnimator valueAnimator) {
        this.f6918s.setBackgroundColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void dismissWaitingDialog() {
        this.f6916q.dismiss();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void indicatorShowUpAnimation() {
        BottomSheetBehavior bottomSheetBehavior = this.f6919t;
        int i2 = this.f6920u;
        ObjectAnimator ofInt = ObjectAnimator.ofInt(bottomSheetBehavior, "peekHeight", i2, (i2 * 192) / 108);
        ofInt.setInterpolator(new LinearOutSlowInInterpolator());
        ofInt.setDuration(750L);
        ofInt.setStartDelay(2500L);
        BottomSheetBehavior bottomSheetBehavior2 = this.f6919t;
        int i3 = this.f6920u;
        ObjectAnimator ofInt2 = ObjectAnimator.ofInt(bottomSheetBehavior2, "peekHeight", (i3 * 192) / 108, i3);
        ofInt2.setInterpolator(new FastOutSlowInInterpolator());
        ofInt2.setDuration(750L);
        ofInt2.setStartDelay(2500L);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(ofInt, ofInt2);
        animatorSet.start();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void makeSnackBar(@StringRes int i2, int i3, Object... objArr) {
        Snackbar.make(this.f6917r, getString(i2, objArr), i3).show();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void makeWaitingDialog(@StringRes int i2, Object... objArr) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        this.f6916q = progressDialog;
        progressDialog.setProgressStyle(0);
        this.f6916q.setMessage(getString(i2, objArr));
        this.f6916q.setIndeterminate(false);
        this.f6916q.setCancelable(false);
        this.f6916q.show();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void notifyGPSInfo() {
        this.f6915p.notifyVisible();
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(@Nullable Bundle bundle) {
        LogUtil.d("MAP:onCreate");
        super.onCreate(bundle);
        GPSManager.n(getContext());
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        LogUtil.d("MAP:onCreateView");
        View inflate = layoutInflater.inflate(R.layout.fragment_map, viewGroup, false);
        this.f6917r = inflate;
        Button button = (Button) inflate.findViewById(R.id.f_map_btn_run);
        this.f6906g = button;
        int i2 = R.string.f_map_pause;
        if (button != null) {
            button.setVisibility(0);
            Button button2 = this.f6906g;
            MapContract.Presenter presenter = this.presenter;
            button2.setText(getString((presenter == null || !presenter.isRunning()) ? R.string.f_map_run : R.string.f_map_pause));
            this.f6906g.setOnClickListener(new View.OnClickListener() { // from class: r.a
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MapFragment.this.t(view);
                }
            });
            this.f6906g.setOnLongClickListener(new View.OnLongClickListener() { // from class: r.b
                @Override // android.view.View.OnLongClickListener
                public final boolean onLongClick(View view) {
                    boolean u2;
                    u2 = MapFragment.this.u(view);
                    return u2;
                }
            });
        }
        Button button3 = (Button) this.f6917r.findViewById(R.id.f_map_btn_pause);
        this.f6907h = button3;
        if (button3 != null) {
            MapContract.Presenter presenter2 = this.presenter;
            button3.setVisibility((presenter2 == null || !presenter2.isRunning()) ? 8 : 0);
            Button button4 = this.f6907h;
            MapContract.Presenter presenter3 = this.presenter;
            if (presenter3 != null && presenter3.isRunningPaused()) {
                i2 = R.string.f_map_resume;
            }
            button4.setText(getString(i2));
            this.f6907h.setOnClickListener(new View.OnClickListener() { // from class: r.c
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MapFragment.this.v(view);
                }
            });
            this.f6907h.setOnLongClickListener(new View.OnLongClickListener() { // from class: r.d
                @Override // android.view.View.OnLongClickListener
                public final boolean onLongClick(View view) {
                    boolean w2;
                    w2 = MapFragment.this.w(view);
                    return w2;
                }
            });
        }
        this.f6908i = (TextView) this.f6917r.findViewById(R.id.f_map_txt_distance_number);
        this.f6909j = (TextView) this.f6917r.findViewById(R.id.f_map_txt_duration_number);
        this.f6910k = (TextView) this.f6917r.findViewById(R.id.f_map_txt_speed_number);
        this.f6911l = (TextView) this.f6917r.findViewById(R.id.f_map_txt_distance_text);
        this.f6912m = (TextView) this.f6917r.findViewById(R.id.f_map_txt_duration_text);
        this.f6913n = (TextView) this.f6917r.findViewById(R.id.f_map_txt_speed_text);
        this.f6915p = (GPSInfoView) this.f6917r.findViewById(R.id.f_map_gpsinfo);
        this.f6914o = (Group) this.f6917r.findViewById(R.id.f_map_group_running);
        MapView mapView = (MapView) this.f6917r.findViewById(R.id.f_map_mapview);
        this.f6903d = mapView;
        mapView.onCreate(bundle);
        if (this.f6904e == null) {
            this.f6904e = this.f6903d.getMap();
        }
        this.f6904e.setLocationSource(new a());
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        this.f6905f = myLocationStyle;
        myLocationStyle.myLocationType(2);
        this.f6905f.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_location_point));
        this.f6905f.anchor(0.5f, 0.5f);
        this.f6905f.strokeColor(getResources().getColor(R.color.map_location_stroke));
        this.f6905f.radiusFillColor(getResources().getColor(R.color.map_location_fill));
        this.f6904e.setMyLocationStyle(this.f6905f);
        this.f6904e.getUiSettings().setMyLocationButtonEnabled(false);
        this.f6904e.getUiSettings().setScaleControlsEnabled(false);
        this.f6904e.getUiSettings().setZoomControlsEnabled(false);
        this.f6904e.getUiSettings().setCompassEnabled(false);
        this.f6904e.getUiSettings().setScrollGesturesEnabled(true);
        this.f6904e.getUiSettings().setZoomGesturesEnabled(true);
        this.f6904e.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(39.99281d, 116.31088d)));
        this.f6904e.moveCamera(CameraUpdateFactory.zoomTo(18.0f));
        this.f6918s = getActivity().findViewById(R.id.a_main_coordinator);
        BottomSheetBehavior from = BottomSheetBehavior.from((NestedScrollView) this.f6917r.findViewById(R.id.f_map_bottomsheet));
        this.f6919t = from;
        this.f6920u = from.getPeekHeight();
        setHasOptionsMenu(true);
        return this.f6917r;
    }

    @Override // androidx.fragment.app.Fragment
    public void onDestroy() {
        LogUtil.d("MAP:onDestroy");
        GPSManager.o();
        releaseWakeLock();
        dismissNotification();
        super.onDestroy();
        this.f6903d.onDestroy();
        this.f6903d = null;
        this.f6904e = null;
        this.presenter = null;
        this.f6917r = null;
    }

    @Override // androidx.fragment.app.Fragment
    public void onPause() {
        LogUtil.d("MAP:onPause");
        super.onPause();
        this.f6903d.onPause();
    }

    @Override // androidx.fragment.app.Fragment
    public void onPrepareOptionsMenu(Menu menu) {
        this.presenter.syncOptionsMenu(menu);
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        LogUtil.d("MAP:onResume");
        super.onResume();
        if (isVisible()) {
            ((AppBarLayout) getActivity().findViewById(R.id.v_main_appbar)).setExpanded(false);
        }
        this.f6903d.onResume();
        this.presenter.start();
        if (this.shouldRevealAnimation) {
            E();
            this.shouldRevealAnimation = false;
        }
        this.presenter.updateUnitPreference();
    }

    @Override // androidx.fragment.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        LogUtil.d("MAP:onSaveInstanceState");
        super.onSaveInstanceState(bundle);
        this.f6903d.onSaveInstanceState(bundle);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void openDevelopSettings() {
        Intent intent = new Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
        intent.setFlags(268435456);
        startActivity(intent);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void openGPSSettings() {
        Intent intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
        intent.setFlags(268435456);
        startActivity(intent);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public Observable<Boolean> registerMapCenterHelper() {
        return Observable.create(new ObservableOnSubscribe() { // from class: r.f
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                MapFragment.this.A(observableEmitter);
            }
        });
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void releaseWakeLock() {
        PowerManager.WakeLock wakeLock = this.f6922w;
        if (wakeLock != null) {
            wakeLock.release();
        }
        this.f6922w = null;
        LogUtil.d("Wake lock released");
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void requireWakeLock() {
        if (this.f6921v == null) {
            this.f6921v = (PowerManager) getActivity().getSystemService("power");
        }
        PowerManager.WakeLock newWakeLock = this.f6921v.newWakeLock(1, "pku runner");
        this.f6922w = newWakeLock;
        newWakeLock.acquire(86400000L);
        LogUtil.d("Wake lock acquired");
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void setAssistantText(@StringRes int i2, double d2, Object... objArr) {
        this.f6915p.setInfoText(getString(i2, objArr));
        this.f6915p.setSignalStrength(d2);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void setLocatingPointEnabled(boolean z2) {
        this.f6905f.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_location_point));
        this.f6904e.setMyLocationStyle(this.f6905f);
        this.f6904e.setMyLocationEnabled(z2);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void switchToPaused() {
        this.f6907h.setText(getString(R.string.f_map_resume));
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void switchToReset() {
        this.f6904e.clear();
        this.f6906g.setText(getString(R.string.f_map_run));
        this.f6907h.setVisibility(8);
        this.f6919t.setPeekHeight(this.f6920u);
        updateTextView(0.0d, 0.0d, 0.0d, false);
        D(new d() { // from class: cn.edu.pku.pkurunner.Map.c
            @Override // cn.edu.pku.pkurunner.Map.MapFragment.d
            public final void a() {
                MapFragment.this.C();
            }
        });
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void switchToRunning() {
        this.f6906g.setText(getString(R.string.f_map_stop));
        this.f6907h.setVisibility(0);
        this.f6907h.setText(getString(R.string.f_map_pause));
        this.f6919t.setPeekHeight(this.f6920u);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void toggleGPSAssistantIndication(boolean z2) {
        this.f6915p.setPersistent(z2);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void toggleRunningIndication(boolean z2) {
        this.f6914o.setVisibility(z2 ? 0 : 8);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void updateWaitingDialog(@StringRes int i2, Object... objArr) {
        this.f6916q.setMessage(getString(i2, objArr));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void C() {
        ((MainActivity) getActivity()).switchFromRunningToRecordList();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void x() {
        ((MainActivity) getActivity()).switchFromRunningToRecordList();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void z(ObservableEmitter observableEmitter, MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() != 1) {
            return;
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public boolean checkKeepAlive() {
        return KeepAliveUtil.check(getActivity());
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void dismissNotification() {
        getActivity().stopService(new Intent(getActivity(), (Class<?>) NotificationDisplayService.class));
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public MainActivity getActivityFromContract() {
        return (MainActivity) getActivity();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public Context getFragmentContext() {
        return getContext();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void makeToast(@StringRes int i2, int i3, Object... objArr) {
        Toast.makeText(getContext(), getString(i2, objArr), i3).show();
    }

    @Override // androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_map, menu);
    }

    @Override // androidx.fragment.app.Fragment
    public void onHiddenChanged(boolean z2) {
        super.onHiddenChanged(z2);
        if (z2) {
            this.f6906g.setVisibility(8);
            return;
        }
        this.f6906g.setVisibility(0);
        ((AppBarLayout) getActivity().findViewById(R.id.v_main_appbar)).setExpanded(false);
        if (this.shouldRevealAnimation) {
            E();
            this.shouldRevealAnimation = false;
        }
        this.presenter.updateUnitPreference();
    }

    @Override // androidx.fragment.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.f_m_map_checkbox_gps_status_on) {
                if (menuItem.isChecked()) {
                    LogUtil.d("Unchecking!");
                    menuItem.setChecked(false);
                    this.presenter.pauseGPSAssistant();
                } else {
                    LogUtil.d("Checking!");
                    menuItem.setChecked(true);
                    this.presenter.startGPSAssistant();
                }
                return true;
        } else if (itemId == R.id.f_m_map_checkbox_location_auto_on) {
                if (menuItem.isChecked()) {
                    LogUtil.d("Unchecking!");
                    menuItem.setChecked(false);
                    this.presenter.pauseAutoLocating();
                } else {
                    LogUtil.d("Checking!");
                    menuItem.setChecked(true);
                    this.presenter.startAutoLocating();
                }
                return true;
        } else if (itemId == R.id.f_m_map_return) {
                D(new d() { // from class: cn.edu.pku.pkurunner.Map.d
                    @Override // cn.edu.pku.pkurunner.Map.MapFragment.d
                    public final void a() {
                        MapFragment.this.x();
                    }
                });
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // androidx.fragment.app.Fragment
    public void onStart() {
        super.onStart();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void showNotification() {
        getActivity().startService(new Intent(getActivity(), (Class<?>) NotificationDisplayService.class));
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.View
    public void updateTextSci(SpeedHelper.SPEED_UNIT speed_unit) {
        this.f6913n.setText(getString(R.string.f_map_hint_speed_main, getResources().getStringArray(R.array.f_map_hint_speed_units)[speed_unit.ordinal()]));
        if (speed_unit == SpeedHelper.SPEED_UNIT.C) {
            this.f6911l.setText(R.string.f_map_hint_distance_sci);
            this.f6912m.setText(R.string.f_map_hint_time_sci);
            this.f6908i.setTextSize(2, 25.0f);
            this.f6909j.setTextSize(2, 25.0f);
            this.f6910k.setTextSize(2, 25.0f);
            return;
        }
        this.f6911l.setText(R.string.f_map_hint_distance_normal);
        this.f6912m.setText(R.string.f_map_hint_time_normal);
        this.f6908i.setTextSize(2, 33.0f);
        this.f6909j.setTextSize(2, 33.0f);
        this.f6910k.setTextSize(2, 33.0f);
    }
}
