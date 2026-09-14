package cn.edu.pku.pkurunner.Map;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.Exception.DataException;
import cn.edu.pku.pkurunner.Map.GPSManager;
import cn.edu.pku.pkurunner.Map.MapContract;
import cn.edu.pku.pkurunner.Map.MapPresenter;
import cn.edu.pku.pkurunner.Map.SpeedHelper;
import cn.edu.pku.pkurunner.Model.PartialRecord;
import cn.edu.pku.pkurunner.Model.Point;
import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Utils.SecUtil;
import cn.edu.pku.pkurunner.Utils.VibratorUtil;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.PolylineOptions;
import com.instacart.library.truetime.TrueTime;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.xutils.common.util.LogUtil;

public class MapPresenter implements MapContract.Presenter {

    private static Date startDate;

    private double distance;

    private double durationSeconds;

    private int stepCount;

    private MapContract.View mapView;

    private Disposable mapCenterDisposable;

    private Point lastPoint;

    private boolean lastSpeedValid;

    private State state = State.IDLE;

    private SpeedHelper.SPEED_UNIT speedUnit = SpeedHelper.SPEED_UNIT.KilometerPerHour;

    private ArrayList trackPoints = new ArrayList();

    private int lastUploadIndex = 0;

    private boolean autoLocating = false;

    private boolean gpsAssistantEnabled = false;

    private boolean userMovedCamera = false;

    private SpeedFilter speedFilter = new SpeedFilter();

    private StepDetector stepDetector = new StepDetector();

    class InitTrueTimeListener implements DialogInterface.OnClickListener {
        InitTrueTimeListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int index) {
            new Thread(new Runnable() {
                @Override
                public final void run() {
                    MapPresenter.InitTrueTimeListener.b();
                }
            }).start();
        }

        public static /* synthetic */ void b() {
            try {
                TrueTime.build().initialize();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    private class SpeedFilter {

        ArrayList skippedPoints;

        int skippedCount;

        Point lastAcceptedPoint;

        long lastAcceptedTime;

        private SpeedFilter() {
            this.skippedPoints = new ArrayList();
            this.skippedCount = 0;
        }

        boolean a(Point point, float value4, long j2) {
            if (value4 > 24.0f) {
                this.skippedPoints.add(point);
                this.skippedCount++;
                return false;
            }
            if (this.lastAcceptedPoint == null) {
                this.lastAcceptedPoint = point;
                this.lastAcceptedTime = j2;
                return true;
            }
            if (AMapUtils.calculateLineDistance(point.toLatLng(), this.lastAcceptedPoint.toLatLng()) > (this.skippedCount + 1) * 26) {
                this.skippedPoints.add(point);
                this.skippedCount++;
                return false;
            }
            this.lastAcceptedPoint = point;
            this.lastAcceptedTime = j2;
            return true;
        }

        void b(Point point, float value4, long j2) {
            int index = 1;
            if (value4 > 24.0f) {
                this.skippedPoints.add(point);
                this.skippedCount++;
                MapPresenter.this.lastSpeedValid = false;
                VibratorUtil.vibrate();
                MapPresenter.this.mapView.makeSnackBar(R.string.p_map_error_invalid_speed, -1, new Object[0]);
                return;
            }
            if (this.lastAcceptedPoint == null) {
                this.lastAcceptedPoint = point;
                this.lastAcceptedTime = j2;
                MapPresenter.this.trackPoints.add(point);
                MapPresenter.this.lastSpeedValid = true;
                return;
            }
            float calculateLineDistance = AMapUtils.calculateLineDistance(point.toLatLng(), this.lastAcceptedPoint.toLatLng());
            if (calculateLineDistance > (this.skippedCount + 1) * 26) {
                this.skippedPoints.add(point);
                this.skippedCount++;
                MapPresenter.this.lastSpeedValid = false;
                VibratorUtil.vibrate();
                MapPresenter.this.mapView.makeSnackBar(R.string.p_map_error_invalid_speed, -1, new Object[0]);
                return;
            }
            MapPresenter.this.lastSpeedValid = true;
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTimeInMillis(this.lastAcceptedTime);
            if (gregorianCalendar.get(11) < 8) {
                gregorianCalendar.setTimeInMillis(j2);
                if (gregorianCalendar.get(11) >= 8) {
                    MapPresenter.this.trackPoints.add(new Point(0, 0, 360.0d, 360.0d, 3));
                }
            }
            if (this.skippedCount == 0) {
                MapPresenter.this.trackPoints.add(point);
                MapPresenter.r(MapPresenter.this, calculateLineDistance);
                MapPresenter.s(MapPresenter.this, (j2 - this.lastAcceptedTime) / 1000.0d);
            } else {
                if (this.skippedPoints.size() != this.skippedCount) {
                    ((Point) MapPresenter.this.trackPoints.get(MapPresenter.this.trackPoints.size() - 1)).setStatus(2);
                    point.setStatus(1);
                    MapPresenter.this.trackPoints.add(point);
                } else {
                    double latitude = point.getLatitude() - this.lastAcceptedPoint.getLatitude();
                    double longitude = point.getLongitude() - this.lastAcceptedPoint.getLongitude();
                    int index2 = this.skippedCount;
                    double value = latitude / (index2 + 1);
                    double value2 = longitude / (index2 + 1);
                    while (index <= this.skippedCount) {
                        double value3 = index;
                        MapPresenter.this.trackPoints.add(new Point(new LatLng(this.lastAcceptedPoint.getLatitude() + (value * value3), this.lastAcceptedPoint.getLongitude() + (value3 * value2))));
                        index++;
                    }
                    MapPresenter.this.trackPoints.add(point);
                    MapPresenter.r(MapPresenter.this, calculateLineDistance);
                    MapPresenter.s(MapPresenter.this, (j2 - this.lastAcceptedTime) / 1000.0d);
                }
                this.skippedCount = 0;
                this.skippedPoints.clear();
            }
            this.lastAcceptedPoint = point;
            this.lastAcceptedTime = j2;
        }

        void c() {
            this.skippedPoints.clear();
        }

        void d() {
            this.skippedPoints.clear();
            this.skippedCount = 0;
            this.lastAcceptedPoint = null;
        }

    }

    private class StepDetector implements SensorEventListener {

        private SensorManager sensorManager;

        private Sensor accelerometer;

        private float[] calibration;

        private float baseMagnitude;

        private boolean available = false;

        private float stepThreshold = 10.0f;

        private float[] currentMagnitude = new float[6];

        private float[] lastComparison = new float[6];

        private float[][] peakCandidates = {new float[6], new float[6]};

        private float[] lastDelta = new float[6];

        private int lastPeakIndex = -1;

        @Override
        public void onAccuracyChanged(Sensor sensor, int index) {
        }

        StepDetector() {
            float density = Resources.getSystem().getDisplayMetrics().density;
            this.calibration = new float[]{-(0.05098581f * density), -(density * 0.016666668f)};
            float value4 = 480 * 0.5f;
            this.baseMagnitude = value4;
        }

        public void e() {
            if (this.available) {
                this.sensorManager.unregisterListener(this);
            }
        }

        public void f() {
            if (this.available) {
                this.sensorManager.registerListener(this, this.accelerometer, 3);
            }
        }

        protected void d() {
            SensorManager manager = (SensorManager) MapPresenter.this.mapView.getFragmentContext().getSystemService(Context.SENSOR_SERVICE);
            this.sensorManager = manager;
            Sensor defaultSensor = manager == null ? null : manager.getDefaultSensor(1);
            this.accelerometer = defaultSensor;
            boolean z2 = defaultSensor != null;
            this.available = z2;
            if (z2) {
                return;
            }
            MapPresenter.this.mapView.makeToast(R.string.p_map_pace_unsupported, 0, new Object[0]);
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (this.available) {
                Sensor sensor = sensorEvent.sensor;
                synchronized (this) {
                    try {
                        boolean z2 = true;
                        if (sensor.getType() == 1) {
                            float value4 = BitmapDescriptorFactory.HUE_RED;
                            for (int index = 0; index < 3; index++) {
                                value4 += this.baseMagnitude + (sensorEvent.values[index] * this.calibration[1]);
                            }
                            float value5 = value4 / 3.0f;
                            float compare = Float.compare(value5, this.currentMagnitude[0]);
                            if (compare == (-this.lastComparison[0])) {
                                int index2 = compare > BitmapDescriptorFactory.HUE_RED ? 0 : 1;
                                float[][] fArr = this.peakCandidates;
                                float[] fArr2 = fArr[index2];
                                float f4 = this.currentMagnitude[0];
                                fArr2[0] = f4;
                                int index3 = 1 - index2;
                                float abs = Math.abs(f4 - fArr[index3][0]);
                                if (abs > this.stepThreshold) {
                                    float f5 = this.lastDelta[0];
                                    boolean z3 = abs > (2.0f * f5) / 3.0f;
                                    boolean z4 = f5 > abs / 3.0f;
                                    if (this.lastPeakIndex == index3) {
                                        z2 = false;
                                    }
                                    if (z3 && z4 && z2) {
                                        MapPresenter.this.onStep();
                                        this.lastPeakIndex = index2;
                                    } else {
                                        this.lastPeakIndex = -1;
                                    }
                                }
                                this.lastDelta[0] = abs;
                            }
                            this.lastComparison[0] = compare;
                            this.currentMagnitude[0] = value5;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }
    }

    private enum State {
        IDLE,
        RUNNING,
        PAUSED
    }

    public /* synthetic */ void B(Boolean bool) {
        this.userMovedCamera = false;
    }

    public /* synthetic */ void C(Boolean bool) {
        this.userMovedCamera = true;
    }

    public static /* synthetic */ void E(Boolean bool) {
    }

    @Override
    public void pauseAutoLocating() {
        this.autoLocating = false;
        State currentState = this.state;
        if (currentState == State.RUNNING || currentState == State.PAUSED) {
            return;
        }
        L();
    }

    @Override
    public void pauseGPSAssistant() {
        this.gpsAssistantEnabled = false;
        GPSManager.removeStatusListener("presenter-status");
        this.mapView.toggleGPSAssistantIndication(false);
    }

    @Override
    public void start() {
    }

    @Override
    public void startAutoLocating() {
        this.autoLocating = true;
        State currentState = this.state;
        if (currentState == State.RUNNING || currentState == State.PAUSED) {
            return;
        }
        K();
    }

    @Override
    public void startGPSAssistant() {
        this.gpsAssistantEnabled = true;
        GPSManager.addStatusListener(new GPSManager.GPSStatusListener() {
            @Override
            public final void onStatusUpdate(int index, int index2, double value) {
                MapPresenter.this.H(index, index2, value);
            }
        }, "presenter-status");
        this.mapView.toggleGPSAssistantIndication(true);
    }

    @Override
    public void syncData() {
        try {
            Date date = new Date(startDate.getTime() + (((long) this.durationSeconds) * 1000));
            Record record = new Record(Data.getUser().getId(), (int) this.distance, (int) this.durationSeconds, date, this.stepCount, SecUtil.generateCheckField(Data.getUser().getId(), date));
            if (Data.getUser().isOffline().booleanValue()) {
                record.setUploaded(true);
                record.setVerified(true);
            }
            Data.provideTrackForRecord(Data.saveRecordToDatabase(record), this.trackPoints).observeOn(AndroidSchedulers.mainThread()).doOnNext(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    Data.clearPartialData();
                }
            }).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MapPresenter.E((Boolean) obj);
                }
            }, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MapPresenter.this.F((Throwable) obj);
                }
            });
        } catch (DataException e2) {
            e2.printStackTrace();
            this.mapView.makeSnackBar(R.string.p_map_error_save_record, -1, e2.getMessage());
            LogUtil.e(e2.toString());
        }
    }

    static /* synthetic */ class SwitchMap {

        static final /* synthetic */ int[] STATE_SWITCH_MAP;

        static final /* synthetic */ int[] CAUSE_SWITCH_MAP;

        static {
            int[] iArr = new int[GPSManager.GPSException.CAUSE.values().length];
            CAUSE_SWITCH_MAP = iArr;
            try {
                iArr[GPSManager.GPSException.CAUSE.GPS_NOT_PROVIDED.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                CAUSE_SWITCH_MAP[GPSManager.GPSException.CAUSE.MOCK_LOCATION.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            int[] iArr2 = new int[State.values().length];
            STATE_SWITCH_MAP = iArr2;
            try {
                iArr2[State.PAUSED.ordinal()] = 1;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                STATE_SWITCH_MAP[State.RUNNING.ordinal()] = 2;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    public /* synthetic */ void F(Throwable th) {
        this.mapView.makeSnackBar(R.string.p_map_error_provide_track, -1, th.getMessage());
        LogUtil.e(th.toString());
    }

    public void G(Location location) {
        LogUtil.d(String.format("Current location (%f, %f).", Double.valueOf(location.getLongitude()), Double.valueOf(location.getLatitude())));
        Point point = new Point(0, 0, location.getLongitude(), location.getLatitude(), 0);
        int index = SwitchMap.STATE_SWITCH_MAP[this.state.ordinal()];
        if (index == 1) {
            if (!this.userMovedCamera) {
                this.mapView.getAMap().animateCamera(CameraUpdateFactory.changeLatLng(point.toLatLng()));
            }
            this.mapView.getLocationListener().onLocationChanged(location);
            boolean a2 = this.speedFilter.a(point, location.getAccuracy(), location.getTime());
            this.lastSpeedValid = a2;
            if (!a2) {
                this.speedFilter.c();
                VibratorUtil.vibrate();
                this.mapView.makeSnackBar(R.string.p_map_error_invalid_speed, -1, new Object[0]);
            }
            this.lastPoint = point;
        } else if (index != 2) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (!this.userMovedCamera) {
                this.mapView.getAMap().animateCamera(CameraUpdateFactory.changeLatLng(latLng));
            }
            this.mapView.getLocationListener().onLocationChanged(location);
        } else {
            if (!this.userMovedCamera) {
                this.mapView.getAMap().animateCamera(CameraUpdateFactory.changeLatLng(point.toLatLng()));
            }
            this.mapView.getLocationListener().onLocationChanged(location);
            if (this.lastPoint != null) {
                this.mapView.getAMap().addPolyline(new PolylineOptions().add(this.lastPoint.toLatLng(), point.toLatLng()).color(this.mapView.getFragmentContext().getResources().getColor(R.color.map_route)));
                this.speedFilter.b(point, location.getAccuracy(), location.getTime());
                MapContract.View view = this.mapView;
                double value = this.distance;
                double value2 = value / 1000.0d;
                double value3 = this.durationSeconds;
                view.updateTextView(value2, value3, SpeedHelper.toUnitOf(this.speedUnit, value / value3), this.speedUnit == SpeedHelper.SPEED_UNIT.C);
            }
            this.lastPoint = point;
        }
        if (this.trackPoints.size() - this.lastUploadIndex >= 30) {
            t();
            this.lastUploadIndex = this.trackPoints.size();
        }
    }

    private void J() {
        this.mapView.switchToRunning();
        this.mapView.makeToast(R.string.p_map_resume_running, 0, new Object[0]);
        this.state = State.RUNNING;
        Point point = this.lastPoint;
        if (point == null || !this.lastSpeedValid) {
            return;
        }
        point.setStatus(1);
        this.trackPoints.add(this.lastPoint);
    }

    private void K() {
        this.mapView.setLocatingPointEnabled(true);
        GPSManager.addLocationListener(new GPSManager.GPSLocationListener() {
            @Override
            public final void onLocationUpdate(Location location) {
                MapPresenter.this.G(location);
            }
        }, "presenter-location");
        GPSManager.addStatusListener(new GPSManager.GPSStatusListener() {
            @Override
            public final void onStatusUpdate(int index, int index2, double value) {
                MapPresenter.this.I(index, index2, value);
            }
        }, "presenter-location");
    }

    private void L() {
        GPSManager.removeLocationListener("presenter-location");
        GPSManager.removeStatusListener("presenter-location");
        this.mapView.setLocatingPointEnabled(false);
    }

    private void M() {
        this.mapView.switchToPaused();
        this.mapView.makeToast(R.string.p_map_pause_running, 0, new Object[0]);
        this.state = State.PAUSED;
        if (this.trackPoints.isEmpty()) {
            return;
        }
        ((Point) this.trackPoints.get(this.trackPoints.size() - 1)).setStatus(2);
    }

    private void N() {
        this.mapView.showNotification();
        this.mapView.switchToRunning();
        this.mapView.makeToast(R.string.p_map_start_running, 0, new Object[0]);
        this.mapView.indicatorShowUpAnimation();
        this.mapView.requireWakeLock();
        this.userMovedCamera = false;
        this.mapCenterDisposable = this.mapView.registerMapCenterHelper().doOnNext(new Consumer() {
            @Override
            public final void accept(Object obj) {
                MapPresenter.this.C((Boolean) obj);
            }
        }).debounce(10L, TimeUnit.SECONDS).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                MapPresenter.this.B((Boolean) obj);
            }
        });
        K();
        this.state = State.RUNNING;
        this.distance = 0.0d;
        this.durationSeconds = 0.0d;
        this.stepCount = 0;
        this.lastPoint = null;
        this.trackPoints.clear();
        this.speedFilter.d();
        if (!this.stepDetector.available) {
            this.stepDetector.d();
        }
        this.stepDetector.f();
    }

    static /* synthetic */ double r(MapPresenter mapPresenter, double value) {
        double value2 = mapPresenter.distance + value;
        mapPresenter.distance = value2;
        return value2;
    }

    static /* synthetic */ double s(MapPresenter mapPresenter, double value) {
        double value2 = mapPresenter.durationSeconds + value;
        mapPresenter.durationSeconds = value2;
        return value2;
    }

    private void t() {
        ArrayList arrayList = this.trackPoints;
        try {
            Data.provideTrackForPartialRecord(Data.savePartialRecordToDatabase(new PartialRecord((int) this.distance, (int) this.durationSeconds, new Date(), this.stepCount)), arrayList.subList(this.lastUploadIndex, arrayList.size())).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MapPresenter.this.v((Boolean) obj);
                }
            }, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MapPresenter.this.w((Throwable) obj);
                }
            });
        } catch (DataException e2) {
            e2.printStackTrace();
            this.mapView.makeSnackBar(R.string.p_map_error_auto_save_record, -1, e2.getMessage());
            LogUtil.e(e2.toString());
        }
    }

    public /* synthetic */ void v(Boolean bool) {
        this.mapView.makeToast(R.string.p_map_notice_auto_save, 0, new Object[0]);
    }

    public /* synthetic */ void w(Throwable th) {
        this.mapView.makeSnackBar(R.string.p_map_error_provide_track, -1, th.getMessage());
        LogUtil.e(th.toString());
    }

    public /* synthetic */ void x(GPSManager.AccuracyCountdown.Result result) {
        if (result.waiting) {
            this.mapView.updateWaitingDialog(R.string.p_map_try_locating_accuracy, result.message);
            return;
        }
        this.mapView.dismissWaitingDialog();
        if (result.accurate) {
            N();
        } else {
            this.mapView.makeToast(R.string.p_map_locating_accuracy_fail, 0, new Object[0]);
        }
    }

    public /* synthetic */ void y(Throwable th) {
        this.mapView.dismissWaitingDialog();
        if (!(th instanceof GPSManager.GPSException)) {
            th.printStackTrace();
            this.mapView.makeToast(R.string.p_map_error_strange, 0, th);
            return;
        }
        int index = SwitchMap.CAUSE_SWITCH_MAP[((GPSManager.GPSException) th).cause.ordinal()];
        if (index == 1) {
            this.mapView.makeToast(R.string.p_map_error_gps_not_enabled, 0, new Object[0]);
            this.mapView.openGPSSettings();
        } else if (index != 2) {
            this.mapView.makeToast(R.string.p_map_error_other, 0, th.getMessage());
        } else {
            this.mapView.makeToast(R.string.p_map_error_mock_location, 0, new Object[0]);
            this.mapView.openDevelopSettings();
        }
    }

    @Override
    public boolean isRunning() {
        return this.state == State.RUNNING;
    }

    @Override
    public boolean isRunningPaused() {
        return this.state == State.PAUSED;
    }

    @Override
    public boolean onFabPauseClick(boolean z2) {
        int index = SwitchMap.STATE_SWITCH_MAP[this.state.ordinal()];
        if (index == 1) {
            J();
        } else if (index != 2) {
            LogUtil.d("Unexpected branch when pause button pressed.");
        } else {
            if (!z2) {
                this.mapView.makeToast(R.string.p_map_long_press_to_pause, 0, new Object[0]);
                return true;
            }
            M();
        }
        return true;
    }

    @Override
    public boolean onFabRunClick(boolean z2) {
        if (this.state == State.IDLE) {
            if (!this.mapView.checkKeepAlive()) {
                return true;
            }
            if (!u()) {
                this.mapView.makeSnackBar(R.string.p_map_notice_wrong_time_period, 0, new Object[0]);
            }
            if (!TrueTime.isInitialized()) {
                new MaterialAlertDialogBuilder(this.mapView.getFragmentContext()).setTitle(R.string.p_map_notice_network_error).setPositiveButton(R.string.p_map_notice_network_error_confirm, new InitTrueTimeListener()).create().show();
                return true;
            }
            startDate = TrueTime.now();
            this.mapView.makeWaitingDialog(R.string.p_map_try_locating, new Object[0]);
            GPSManager.waitForAccurateLocation().subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MapPresenter.this.x((GPSManager.AccuracyCountdown.Result) obj);
                }
            }, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    MapPresenter.this.y((Throwable) obj);
                }
            });
        } else if (z2) {
            new MaterialAlertDialogBuilder(this.mapView.getFragmentContext()).setTitle(R.string.p_map_dialog_title).setMessage(R.string.p_map_dialog_content).setPositiveButton(R.string.p_map_dialog_positive, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int index) {
                    MapPresenter.this.z(dialogInterface, index);
                }
            }).setNegativeButton(R.string.p_map_dialog_negative, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int index) {
                    dialogInterface.cancel();
                }
            }).create().show();
        } else {
            this.mapView.makeToast(R.string.p_map_long_press_to_finish, 0, new Object[0]);
        }
        return true;
    }

    @Override
    public void onStep() {
        if (this.state == State.RUNNING) {
            this.stepCount++;
        }
    }

    @Override
    public void stopAndSwitchToIdle() {
        this.mapView.dismissNotification();
        this.mapView.switchToReset();
        this.mapView.releaseWakeLock();
        this.mapCenterDisposable.dispose();
        this.mapView.unregisterMapCenterHelper();
        if (!this.autoLocating) {
            L();
        }
        if (!this.gpsAssistantEnabled) {
            pauseGPSAssistant();
        }
        this.state = State.IDLE;
        this.lastUploadIndex = 0;
        this.stepDetector.e();
    }

    public MapPresenter(@NonNull MapContract.View view) {
        this.mapView = view;
    }

    public void H(int index, int index2, double value) {
        this.mapView.setAssistantText(R.string.p_map_gps_info, value / 80.0d, Integer.valueOf(index), Integer.valueOf(index2), Double.valueOf(value));
    }

    public void I(int index, int index2, double value) {
        if (value < 80.0d) {
            this.mapView.notifyGPSInfo();
            this.mapView.setAssistantText(R.string.p_map_gps_weak, value / 80.0d, new Object[0]);
        }
    }

    private boolean u() {
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar.set(11, 6);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar2.set(11, 23);
        calendar2.set(12, 0);
        calendar2.set(13, 59);
        Calendar calendar3 = Calendar.getInstance();
        if (!calendar3.after(calendar) || !calendar3.before(calendar2)) {
            return false;
        }
        return true;
    }

    public /* synthetic */ void z(DialogInterface dialogInterface, int index) {
        stopAndSwitchToIdle();
        syncData();
    }

    @Override
    public void syncOptionsMenu(Menu menu) {
        MenuItem findItem = menu.findItem(R.id.f_m_map_checkbox_location_auto_on);
        MenuItem findItem2 = menu.findItem(R.id.f_m_map_checkbox_gps_status_on);
        findItem.setChecked(this.autoLocating);
        findItem2.setChecked(this.gpsAssistantEnabled);
    }

    @Override
    public void updateUnitPreference() {
        SpeedHelper.SPEED_UNIT speedUnitPreference = Data.getSpeedUnitPreference();
        this.speedUnit = speedUnitPreference;
        Data.setSpeedUnitPreference(speedUnitPreference);
        this.mapView.updateTextSci(this.speedUnit);
    }
}
