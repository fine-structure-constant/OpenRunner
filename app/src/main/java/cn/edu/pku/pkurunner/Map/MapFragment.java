package cn.edu.pku.pkurunner.Map;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import cn.edu.pku.pkurunner.View.OrLoadingDialog;
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

    private MapView mapView;

    private AMap aMap;

    private MyLocationStyle myLocationStyle;

    private Button startButton;

    private Button pauseButton;

    private TextView distanceValueText;

    private TextView durationValueText;

    private TextView speedValueText;

    private TextView distanceLabelText;

    private TextView durationLabelText;

    private TextView speedLabelText;

    private Group runningGroup;

    private GPSInfoView gpsInfoView;

    private OrLoadingDialog progressDialog;

    private View rootView;

    private View coordinatorView;

    private BottomSheetBehavior bottomSheetBehavior;

    private int bottomSheetPeekHeight;

    private PowerManager powerManager;

    private PowerManager.WakeLock wakeLock;

    private LocationSource.OnLocationChangedListener locationChangedListener;
    public boolean shouldRevealAnimation = false;
    public MapContract.Presenter presenter = new MapPresenter(this);

    class MapLocationSource implements LocationSource {
        MapLocationSource() {
        }

        @Override
        public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
            MapFragment.this.locationChangedListener = onLocationChangedListener;
        }

        @Override
        public void deactivate() {
            MapFragment.this.locationChangedListener = null;
        }
    }

    class HideGpsInfoAnimatorListener implements Animator.AnimatorListener {
        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }

        @Override
        public void onAnimationStart(Animator animator) {
        }

        HideGpsInfoAnimatorListener() {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            MapFragment.this.coordinatorView.setBackground(null);
            MapFragment.this.gpsInfoView.setAppear(false);
        }
    }

    class CallbackAnimatorListener implements Animator.AnimatorListener {

        final /* synthetic */ AnimationEndCallback callback;

        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }

        @Override
        public void onAnimationStart(Animator animator) {
        }

        CallbackAnimatorListener(AnimationEndCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            this.callback.onFinished();
            MapFragment.this.coordinatorView.setBackground(null);
        }
    }

    interface AnimationEndCallback {
        void onFinished();
    }

    @Override
    public AMap getAMap() {
        return this.aMap;
    }

    @Override
    public LocationSource.OnLocationChangedListener getLocationListener() {
        return this.locationChangedListener;
    }

    @Override
    public void setPresenter(@NonNull MapContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void unregisterMapCenterHelper() {
    }

    @Override
    public void updateTextView(double value, double value2, double value3, boolean z2) {
        if (z2) {
            this.speedValueText.setText(getString(R.string.f_map_format_speed_sci, Double.valueOf(value3)));
            this.distanceValueText.setText(getString(R.string.f_map_format_distance_sci, Double.valueOf(SpeedHelper.meterToPlanckLength(value * 1000.0d))));
            this.durationValueText.setText(getString(R.string.f_map_format_time_sci, Double.valueOf(SpeedHelper.secondToPlanckTime(value2))));
            return;
        }
        this.speedValueText.setText(getString(R.string.f_map_format_speed, Double.valueOf(value3)));
        this.distanceValueText.setText(getString(R.string.f_map_format_distance, Float.valueOf(((int) (value * 10.0d)) / 10.0f)));
        int index = (int) (value2 / 3600.0d);
        double d5 = value2 - (index * 3600);
        int index2 = (int) (d5 / 60.0d);
        int index3 = (int) (d5 - (index2 * 60));
        this.durationValueText.setText(index == 0 ? getString(R.string.f_map_format_time_short, Integer.valueOf(index2), Integer.valueOf(index3)) : getString(R.string.f_map_format_time_long, Integer.valueOf(index), Integer.valueOf(index2), Integer.valueOf(index3)));
    }

    public /* synthetic */ void A(final ObservableEmitter observableEmitter) {
        this.aMap.setOnMapTouchListener(new AMap.OnMapTouchListener() {
            @Override
            public final void onTouch(MotionEvent motionEvent) {
                MapFragment.z(observableEmitter, motionEvent);
            }
        });
    }

    public /* synthetic */ void B(ValueAnimator valueAnimator) {
        this.coordinatorView.setBackgroundColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
    }

    private void playRevealAnimation(AnimationEndCallback callback) {
        int sqrt = (int) Math.sqrt(Math.pow(this.rootView.getWidth(), 2.0d) + Math.pow(this.rootView.getHeight(), 2.0d));
        Bundle arguments = getArguments();
        int width = this.rootView.getWidth();
        if (arguments != null) {
            width = arguments.getInt("FabX", width);
        }
        Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(this.rootView, width, 0, sqrt, BitmapDescriptorFactory.HUE_RED);
        createCircularReveal.setInterpolator(new AccelerateInterpolator());
        createCircularReveal.setDuration(1000L);
        createCircularReveal.start();
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setIntValues(getResources().getColor(R.color.red_400), getResources().getColor(R.color.primary));
        valueAnimator.setEvaluator(new ArgbEvaluator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MapFragment.this.y(valueAnimator2);
            }
        });
        valueAnimator.setDuration(1000L);
        valueAnimator.addListener(new CallbackAnimatorListener(callback));
        this.coordinatorView.setBackgroundResource(R.color.primary);
        valueAnimator.start();
    }

    private void playConcealAnimation() {
        int sqrt = (int) Math.sqrt(Math.pow(this.rootView.getWidth(), 2.0d) + Math.pow(this.rootView.getHeight(), 2.0d));
        Bundle arguments = getArguments();
        int width = this.rootView.getWidth();
        if (arguments != null) {
            width = arguments.getInt("FabX", width);
        }
        Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(this.rootView, width, 0, BitmapDescriptorFactory.HUE_RED, sqrt);
        createCircularReveal.setInterpolator(new AccelerateInterpolator());
        createCircularReveal.setDuration(1000L);
        createCircularReveal.start();
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setIntValues(getResources().getColor(R.color.primary), getResources().getColor(R.color.red_400));
        valueAnimator.setEvaluator(new ArgbEvaluator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MapFragment.this.B(valueAnimator2);
            }
        });
        valueAnimator.setDuration(500L);
        valueAnimator.addListener(new HideGpsInfoAnimatorListener());
        this.coordinatorView.setBackgroundResource(R.color.primary);
        valueAnimator.setStartDelay(500L);
        valueAnimator.start();
    }

    public /* synthetic */ void t(View view) {
        this.presenter.onFabRunClick(false);
    }

    public /* synthetic */ boolean u(View view) {
        return this.presenter.onFabRunClick(true);
    }

    public /* synthetic */ void v(View view) {
        this.presenter.onFabPauseClick(false);
    }

    public /* synthetic */ boolean w(View view) {
        return this.presenter.onFabPauseClick(true);
    }

    public /* synthetic */ void y(ValueAnimator valueAnimator) {
        this.coordinatorView.setBackgroundColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
    }

    @Override
    public void dismissWaitingDialog() {
        this.progressDialog.dismiss();
    }

    @Override
    public void indicatorShowUpAnimation() {
        BottomSheetBehavior bottomSheetBehavior = this.bottomSheetBehavior;
        int index = this.bottomSheetPeekHeight;
        ObjectAnimator ofInt = ObjectAnimator.ofInt(bottomSheetBehavior, "peekHeight", index, (index * 192) / 108);
        ofInt.setInterpolator(new LinearOutSlowInInterpolator());
        ofInt.setDuration(750L);
        ofInt.setStartDelay(2500L);
        BottomSheetBehavior bottomSheetBehavior2 = this.bottomSheetBehavior;
        int index2 = this.bottomSheetPeekHeight;
        ObjectAnimator ofInt2 = ObjectAnimator.ofInt(bottomSheetBehavior2, "peekHeight", (index2 * 192) / 108, index2);
        ofInt2.setInterpolator(new FastOutSlowInInterpolator());
        ofInt2.setDuration(750L);
        ofInt2.setStartDelay(2500L);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(ofInt, ofInt2);
        animatorSet.start();
    }

    @Override
    public void makeSnackBar(@StringRes int index, int index2, Object... objArr) {
        Snackbar.make(this.rootView, getString(index, objArr), index2).show();
    }

    @Override
    public void makeWaitingDialog(@StringRes int index, Object... objArr) {
        OrLoadingDialog progressDialog = new OrLoadingDialog(getContext());
        this.progressDialog = progressDialog;
        progressDialog.setProgressStyle(0);
        this.progressDialog.setMessage(getString(index, objArr));
        this.progressDialog.setIndeterminate(false);
        this.progressDialog.setCancelable(false);
        this.progressDialog.show();
    }

    @Override
    public void notifyGPSInfo() {
        this.gpsInfoView.notifyVisible();
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        LogUtil.d("MAP:onCreate");
        super.onCreate(bundle);
        GPSManager.initialize(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        LogUtil.d("MAP:onCreateView");
        View inflate = layoutInflater.inflate(R.layout.fragment_map, viewGroup, false);
        this.rootView = inflate;
        Button button = (Button) inflate.findViewById(R.id.f_map_btn_run);
        this.startButton = button;
        int index = R.string.f_map_pause;
        if (button != null) {
            button.setVisibility(0);
            Button button2 = this.startButton;
            MapContract.Presenter presenter = this.presenter;
            button2.setText(getString((presenter == null || !presenter.isRunning()) ? R.string.f_map_run : R.string.f_map_pause));
            this.startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    MapFragment.this.t(view);
                }
            });
            this.startButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public final boolean onLongClick(View view) {
                    boolean u2;
                    u2 = MapFragment.this.u(view);
                    return u2;
                }
            });
        }
        Button button3 = (Button) this.rootView.findViewById(R.id.f_map_btn_pause);
        this.pauseButton = button3;
        if (button3 != null) {
            MapContract.Presenter presenter2 = this.presenter;
            button3.setVisibility((presenter2 == null || !presenter2.isRunning()) ? 8 : 0);
            Button button4 = this.pauseButton;
            MapContract.Presenter presenter3 = this.presenter;
            if (presenter3 != null && presenter3.isRunningPaused()) {
                index = R.string.f_map_resume;
            }
            button4.setText(getString(index));
            this.pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    MapFragment.this.v(view);
                }
            });
            this.pauseButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public final boolean onLongClick(View view) {
                    boolean w2;
                    w2 = MapFragment.this.w(view);
                    return w2;
                }
            });
        }
        this.distanceValueText = (TextView) this.rootView.findViewById(R.id.f_map_txt_distance_number);
        this.durationValueText = (TextView) this.rootView.findViewById(R.id.f_map_txt_duration_number);
        this.speedValueText = (TextView) this.rootView.findViewById(R.id.f_map_txt_speed_number);
        this.distanceLabelText = (TextView) this.rootView.findViewById(R.id.f_map_txt_distance_text);
        this.durationLabelText = (TextView) this.rootView.findViewById(R.id.f_map_txt_duration_text);
        this.speedLabelText = (TextView) this.rootView.findViewById(R.id.f_map_txt_speed_text);
        this.gpsInfoView = (GPSInfoView) this.rootView.findViewById(R.id.f_map_gpsinfo);
        this.runningGroup = (Group) this.rootView.findViewById(R.id.f_map_group_running);
        MapView mapView = (MapView) this.rootView.findViewById(R.id.f_map_mapview);
        this.mapView = mapView;
        mapView.onCreate(bundle);
        if (this.aMap == null) {
            this.aMap = this.mapView.getMap();
        }
        this.aMap.setLocationSource(new MapLocationSource());
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        this.myLocationStyle = myLocationStyle;
        myLocationStyle.myLocationType(2);
        this.myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_location_point));
        this.myLocationStyle.anchor(0.5f, 0.5f);
        this.myLocationStyle.strokeColor(getResources().getColor(R.color.map_location_stroke));
        this.myLocationStyle.radiusFillColor(getResources().getColor(R.color.map_location_fill));
        this.aMap.setMyLocationStyle(this.myLocationStyle);
        this.aMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.aMap.getUiSettings().setScaleControlsEnabled(false);
        this.aMap.getUiSettings().setZoomControlsEnabled(false);
        this.aMap.getUiSettings().setCompassEnabled(false);
        this.aMap.getUiSettings().setScrollGesturesEnabled(true);
        this.aMap.getUiSettings().setZoomGesturesEnabled(true);
        this.aMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(39.99281d, 116.31088d)));
        this.aMap.moveCamera(CameraUpdateFactory.zoomTo(18.0f));
        this.coordinatorView = getActivity().findViewById(R.id.a_main_coordinator);
        BottomSheetBehavior from = BottomSheetBehavior.from((NestedScrollView) this.rootView.findViewById(R.id.f_map_bottomsheet));
        this.bottomSheetBehavior = from;
        this.bottomSheetPeekHeight = from.getPeekHeight();
        setHasOptionsMenu(true);
        return this.rootView;
    }

    @Override
    public void onDestroy() {
        LogUtil.d("MAP:onDestroy");
        GPSManager.release();
        releaseWakeLock();
        dismissNotification();
        super.onDestroy();
        this.mapView.onDestroy();
        this.mapView = null;
        this.aMap = null;
        this.presenter = null;
        this.rootView = null;
    }

    @Override
    public void onPause() {
        LogUtil.d("MAP:onPause");
        super.onPause();
        this.mapView.onPause();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.presenter.syncOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        LogUtil.d("MAP:onResume");
        super.onResume();
        if (isVisible()) {
            ((AppBarLayout) getActivity().findViewById(R.id.v_main_appbar)).setExpanded(false);
        }
        this.mapView.onResume();
        this.presenter.start();
        if (this.shouldRevealAnimation) {
            playConcealAnimation();
            this.shouldRevealAnimation = false;
        }
        this.presenter.updateUnitPreference();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        LogUtil.d("MAP:onSaveInstanceState");
        super.onSaveInstanceState(bundle);
        this.mapView.onSaveInstanceState(bundle);
    }

    @Override
    public void openDevelopSettings() {
        Intent intent = new Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
        intent.setFlags(268435456);
        startActivity(intent);
    }

    @Override
    public void openGPSSettings() {
        Intent intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
        intent.setFlags(268435456);
        startActivity(intent);
    }

    @Override
    public Observable<Boolean> registerMapCenterHelper() {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                MapFragment.this.A(observableEmitter);
            }
        });
    }

    @Override
    public void releaseWakeLock() {
        PowerManager.WakeLock wakeLock = this.wakeLock;
        if (wakeLock != null) {
            wakeLock.release();
        }
        this.wakeLock = null;
        LogUtil.d("Wake lock released");
    }

    @Override
    public void requireWakeLock() {
        if (this.powerManager == null) {
            this.powerManager = (PowerManager) getActivity().getSystemService("power");
        }
        PowerManager.WakeLock newWakeLock = this.powerManager.newWakeLock(1, "pku runner");
        this.wakeLock = newWakeLock;
        newWakeLock.acquire(86400000L);
        LogUtil.d("Wake lock acquired");
    }

    @Override
    public void setAssistantText(@StringRes int index, double value, Object... objArr) {
        this.gpsInfoView.setInfoText(getString(index, objArr));
        this.gpsInfoView.setSignalStrength(value);
    }

    @Override
    public void setLocatingPointEnabled(boolean z2) {
        this.myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_location_point));
        this.aMap.setMyLocationStyle(this.myLocationStyle);
        this.aMap.setMyLocationEnabled(z2);
    }

    @Override
    public void switchToPaused() {
        this.pauseButton.setText(getString(R.string.f_map_resume));
    }

    @Override
    public void switchToReset() {
        this.aMap.clear();
        this.startButton.setText(getString(R.string.f_map_run));
        this.pauseButton.setVisibility(8);
        this.bottomSheetBehavior.setPeekHeight(this.bottomSheetPeekHeight);
        updateTextView(0.0d, 0.0d, 0.0d, false);
        playRevealAnimation(new AnimationEndCallback() {
            @Override
            public final void onFinished() {
                MapFragment.this.returnToRecordList();
            }
        });
    }

    @Override
    public void switchToRunning() {
        this.startButton.setText(getString(R.string.f_map_stop));
        this.pauseButton.setVisibility(0);
        this.pauseButton.setText(getString(R.string.f_map_pause));
        this.bottomSheetBehavior.setPeekHeight(this.bottomSheetPeekHeight);
    }

    @Override
    public void toggleGPSAssistantIndication(boolean z2) {
        this.gpsInfoView.setPersistent(z2);
    }

    @Override
    public void toggleRunningIndication(boolean z2) {
        this.runningGroup.setVisibility(z2 ? 0 : 8);
    }

    @Override
    public void updateWaitingDialog(@StringRes int index, Object... objArr) {
        this.progressDialog.setMessage(getString(index, objArr));
    }

    public /* synthetic */ void returnToRecordList() {
        ((MainActivity) getActivity()).switchFromRunningToRecordList();
    }

    public static /* synthetic */ void z(ObservableEmitter observableEmitter, MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() != 1) {
            return;
        }
        observableEmitter.onNext(Boolean.TRUE);
    }

    @Override
    public boolean checkKeepAlive() {
        return KeepAliveUtil.check(getActivity());
    }

    @Override
    public void dismissNotification() {
        getActivity().stopService(new Intent(getActivity(), (Class<?>) NotificationDisplayService.class));
    }

    @Override
    public MainActivity getActivityFromContract() {
        return (MainActivity) getActivity();
    }

    @Override
    public Context getFragmentContext() {
        return getContext();
    }

    @Override
    public void makeToast(@StringRes int index, int index2, Object... objArr) {
        Toast.makeText(getContext(), getString(index, objArr), index2).show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_map, menu);
    }

    @Override
    public void onHiddenChanged(boolean z2) {
        super.onHiddenChanged(z2);
        if (z2) {
            this.startButton.setVisibility(8);
            return;
        }
        this.startButton.setVisibility(0);
        ((AppBarLayout) getActivity().findViewById(R.id.v_main_appbar)).setExpanded(false);
        if (this.shouldRevealAnimation) {
            playConcealAnimation();
            this.shouldRevealAnimation = false;
        }
        this.presenter.updateUnitPreference();
    }

    @Override
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
                playRevealAnimation(new AnimationEndCallback() {
                    @Override
                    public final void onFinished() {
                        MapFragment.this.returnToRecordList();
                    }
                });
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void showNotification() {
        getActivity().startService(new Intent(getActivity(), (Class<?>) NotificationDisplayService.class));
    }

    @Override
    public void updateTextSci(SpeedHelper.SPEED_UNIT speed_unit) {
        this.speedLabelText.setText(getString(R.string.f_map_hint_speed_main, getResources().getStringArray(R.array.f_map_hint_speed_units)[speed_unit.ordinal()]));
        if (speed_unit == SpeedHelper.SPEED_UNIT.C) {
            this.distanceLabelText.setText(R.string.f_map_hint_distance_sci);
            this.durationLabelText.setText(R.string.f_map_hint_time_sci);
            this.distanceValueText.setTextSize(2, 25.0f);
            this.durationValueText.setTextSize(2, 25.0f);
            this.speedValueText.setTextSize(2, 25.0f);
            return;
        }
        this.distanceLabelText.setText(R.string.f_map_hint_distance_normal);
        this.durationLabelText.setText(R.string.f_map_hint_time_normal);
        this.distanceValueText.setTextSize(2, 33.0f);
        this.durationValueText.setTextSize(2, 33.0f);
        this.speedValueText.setTextSize(2, 33.0f);
    }
}
