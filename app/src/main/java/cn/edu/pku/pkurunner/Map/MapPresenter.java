package cn.edu.pku.pkurunner.Map;

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

    /* renamed from: q, reason: collision with root package name */
    private static Date f6928q;

    /* renamed from: b, reason: collision with root package name */
    private double f6930b;

    /* renamed from: c, reason: collision with root package name */
    private double f6931c;

    /* renamed from: d, reason: collision with root package name */
    private int f6932d;

    /* renamed from: g, reason: collision with root package name */
    private MapContract.View f6935g;

    /* renamed from: m, reason: collision with root package name */
    private Disposable f6941m;

    /* renamed from: n, reason: collision with root package name */
    private Point f6942n;

    /* renamed from: p, reason: collision with root package name */
    private boolean f6944p;

    /* renamed from: a, reason: collision with root package name */
    private e f6929a = e.IDLE;

    /* renamed from: e, reason: collision with root package name */
    private SpeedHelper.SPEED_UNIT f6933e = SpeedHelper.SPEED_UNIT.KilometerPerHour;

    /* renamed from: f, reason: collision with root package name */
    private ArrayList f6934f = new ArrayList();

    /* renamed from: i, reason: collision with root package name */
    private int f6937i = 0;

    /* renamed from: j, reason: collision with root package name */
    private boolean f6938j = false;

    /* renamed from: k, reason: collision with root package name */
    private boolean f6939k = false;

    /* renamed from: l, reason: collision with root package name */
    private boolean f6940l = false;

    /* renamed from: o, reason: collision with root package name */
    private c f6943o = new c(this, null);

    /* renamed from: h, reason: collision with root package name */
    private d f6936h = new d();

    class a implements DialogInterface.OnClickListener {
        a() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i2) {
            new Thread(new Runnable() { // from class: cn.edu.pku.pkurunner.Map.f
                @Override // java.lang.Runnable
                public final void run() {
                    MapPresenter.a.b();
                }
            }).start();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void b() {
            try {
                TrueTime.build().initialize();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    private class c {

        /* renamed from: a, reason: collision with root package name */
        ArrayList f6948a;

        /* renamed from: b, reason: collision with root package name */
        int f6949b;

        /* renamed from: c, reason: collision with root package name */
        Point f6950c;

        /* renamed from: d, reason: collision with root package name */
        long f6951d;

        private c() {
            this.f6948a = new ArrayList();
            this.f6949b = 0;
        }

        boolean a(Point point, float f2, long j2) {
            if (f2 > 24.0f) {
                this.f6948a.add(point);
                this.f6949b++;
                return false;
            }
            if (this.f6950c == null) {
                this.f6950c = point;
                this.f6951d = j2;
                return true;
            }
            if (AMapUtils.calculateLineDistance(point.toLatLng(), this.f6950c.toLatLng()) > (this.f6949b + 1) * 26) {
                this.f6948a.add(point);
                this.f6949b++;
                return false;
            }
            this.f6950c = point;
            this.f6951d = j2;
            return true;
        }

        void b(Point point, float f2, long j2) {
            int i2 = 1;
            if (f2 > 24.0f) {
                this.f6948a.add(point);
                this.f6949b++;
                MapPresenter.this.f6944p = false;
                VibratorUtil.vibrate();
                MapPresenter.this.f6935g.makeSnackBar(R.string.p_map_error_invalid_speed, -1, new Object[0]);
                return;
            }
            if (this.f6950c == null) {
                this.f6950c = point;
                this.f6951d = j2;
                MapPresenter.this.f6934f.add(point);
                MapPresenter.this.f6944p = true;
                return;
            }
            float calculateLineDistance = AMapUtils.calculateLineDistance(point.toLatLng(), this.f6950c.toLatLng());
            if (calculateLineDistance > (this.f6949b + 1) * 26) {
                this.f6948a.add(point);
                this.f6949b++;
                MapPresenter.this.f6944p = false;
                VibratorUtil.vibrate();
                MapPresenter.this.f6935g.makeSnackBar(R.string.p_map_error_invalid_speed, -1, new Object[0]);
                return;
            }
            MapPresenter.this.f6944p = true;
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTimeInMillis(this.f6951d);
            if (gregorianCalendar.get(11) < 8) {
                gregorianCalendar.setTimeInMillis(j2);
                if (gregorianCalendar.get(11) >= 8) {
                    MapPresenter.this.f6934f.add(new Point(0, 0, 360.0d, 360.0d, 3));
                }
            }
            if (this.f6949b == 0) {
                MapPresenter.this.f6934f.add(point);
                MapPresenter.r(MapPresenter.this, calculateLineDistance);
                MapPresenter.s(MapPresenter.this, (j2 - this.f6951d) / 1000.0d);
            } else {
                if (this.f6948a.size() != this.f6949b) {
                    ((Point) MapPresenter.this.f6934f.get(MapPresenter.this.f6934f.size() - 1)).setStatus(2);
                    point.setStatus(1);
                    MapPresenter.this.f6934f.add(point);
                } else {
                    double latitude = point.getLatitude() - this.f6950c.getLatitude();
                    double longitude = point.getLongitude() - this.f6950c.getLongitude();
                    int i3 = this.f6949b;
                    double d2 = latitude / (i3 + 1);
                    double d3 = longitude / (i3 + 1);
                    while (i2 <= this.f6949b) {
                        double d4 = i2;
                        MapPresenter.this.f6934f.add(new Point(new LatLng(this.f6950c.getLatitude() + (d2 * d4), this.f6950c.getLongitude() + (d4 * d3))));
                        i2++;
                        d2 = d2;
                    }
                    MapPresenter.this.f6934f.add(point);
                    MapPresenter.r(MapPresenter.this, calculateLineDistance);
                    MapPresenter.s(MapPresenter.this, (j2 - this.f6951d) / 1000.0d);
                }
                this.f6949b = 0;
                this.f6948a.clear();
            }
            this.f6950c = point;
            this.f6951d = j2;
        }

        void c() {
            this.f6948a.clear();
        }

        void d() {
            this.f6948a.clear();
            this.f6949b = 0;
            this.f6950c = null;
        }

        /* synthetic */ c(MapPresenter mapPresenter, a aVar) {
            this();
        }
    }

    private class d implements SensorEventListener {

        /* renamed from: a, reason: collision with root package name */
        private SensorManager f6953a;

        /* renamed from: b, reason: collision with root package name */
        private Sensor f6954b;

        /* renamed from: f, reason: collision with root package name */
        private float[] f6958f;

        /* renamed from: g, reason: collision with root package name */
        private float f6959g;

        /* renamed from: c, reason: collision with root package name */
        private boolean f6955c = false;

        /* renamed from: d, reason: collision with root package name */
        private float f6956d = 10.0f;

        /* renamed from: e, reason: collision with root package name */
        private float[] f6957e = new float[6];

        /* renamed from: h, reason: collision with root package name */
        private float[] f6960h = new float[6];

        /* renamed from: i, reason: collision with root package name */
        private float[][] f6961i = {new float[6], new float[6]};

        /* renamed from: j, reason: collision with root package name */
        private float[] f6962j = new float[6];

        /* renamed from: k, reason: collision with root package name */
        private int f6963k = -1;

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int i2) {
        }

        d() {
            float density = Resources.getSystem().getDisplayMetrics().density;
            this.f6958f = new float[]{-(0.05098581f * density), -(density * 0.016666668f)};
            float f2 = 480 * 0.5f;
            this.f6959g = f2;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void e() {
            if (this.f6955c) {
                this.f6953a.unregisterListener(this);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void f() {
            if (this.f6955c) {
                this.f6953a.registerListener(this, this.f6954b, 3);
            }
        }

        protected void d() {
            SensorManager sensorManager = (SensorManager) MapPresenter.this.f6935g.getFragmentContext().getSystemService(Context.SENSOR_SERVICE);
            this.f6953a = sensorManager;
            Sensor defaultSensor = sensorManager.getDefaultSensor(1);
            this.f6954b = defaultSensor;
            boolean z2 = defaultSensor != null;
            this.f6955c = z2;
            if (z2) {
                return;
            }
            MapPresenter.this.f6935g.makeToast(R.string.p_map_pace_unsupported, 0, new Object[0]);
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (this.f6955c) {
                Sensor sensor = sensorEvent.sensor;
                synchronized (this) {
                    try {
                        boolean z2 = true;
                        if (sensor.getType() == 1) {
                            float f2 = BitmapDescriptorFactory.HUE_RED;
                            for (int i2 = 0; i2 < 3; i2++) {
                                f2 += this.f6959g + (sensorEvent.values[i2] * this.f6958f[1]);
                            }
                            float f3 = f2 / 3.0f;
                            float compare = Float.compare(f3, this.f6957e[0]);
                            if (compare == (-this.f6960h[0])) {
                                int i3 = compare > BitmapDescriptorFactory.HUE_RED ? 0 : 1;
                                float[][] fArr = this.f6961i;
                                float[] fArr2 = fArr[i3];
                                float f4 = this.f6957e[0];
                                fArr2[0] = f4;
                                int i4 = 1 - i3;
                                float abs = Math.abs(f4 - fArr[i4][0]);
                                if (abs > this.f6956d) {
                                    float f5 = this.f6962j[0];
                                    boolean z3 = abs > (2.0f * f5) / 3.0f;
                                    boolean z4 = f5 > abs / 3.0f;
                                    if (this.f6963k == i4) {
                                        z2 = false;
                                    }
                                    if (z3 && z4 && z2) {
                                        MapPresenter.this.onStep();
                                        this.f6963k = i3;
                                    } else {
                                        this.f6963k = -1;
                                    }
                                }
                                this.f6962j[0] = abs;
                            }
                            this.f6960h[0] = compare;
                            this.f6957e[0] = f3;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }
    }

    private enum e {
        IDLE,
        RUNNING,
        PAUSED
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void B(Boolean bool) {
        this.f6940l = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void C(Boolean bool) {
        this.f6940l = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void E(Boolean bool) {
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public void pauseAutoLocating() {
        this.f6938j = false;
        e eVar = this.f6929a;
        if (eVar == e.RUNNING || eVar == e.PAUSED) {
            return;
        }
        L();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public void pauseGPSAssistant() {
        this.f6939k = false;
        GPSManager.s("presenter-status");
        this.f6935g.toggleGPSAssistantIndication(false);
    }

    @Override // cn.edu.pku.pkurunner.Contract.BasePresenter
    public void start() {
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public void startAutoLocating() {
        this.f6938j = true;
        e eVar = this.f6929a;
        if (eVar == e.RUNNING || eVar == e.PAUSED) {
            return;
        }
        K();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public void startGPSAssistant() {
        this.f6939k = true;
        GPSManager.h(new GPSManager.GPSStatusListener() { // from class: r.p
            @Override // cn.edu.pku.pkurunner.Map.GPSManager.GPSStatusListener
            public final void onStatusUpdate(int i2, int i3, double d2) {
                MapPresenter.this.H(i2, i3, d2);
            }
        }, "presenter-status");
        this.f6935g.toggleGPSAssistantIndication(true);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public void syncData() {
        try {
            Date date = new Date(f6928q.getTime() + (((long) this.f6931c) * 1000));
            Record record = new Record(Data.getUser().getId(), (int) this.f6930b, (int) this.f6931c, date, this.f6932d, SecUtil.generateCheckField(Data.getUser().getId(), date));
            if (Data.getUser().isOffline().booleanValue()) {
                record.setUploaded(true);
                record.setVerified(true);
            }
            Data.provideTrackForRecord(Data.saveRecordToDatabase(record), this.f6934f).observeOn(AndroidSchedulers.mainThread()).doOnNext(new Consumer() { // from class: r.i
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    Data.clearPartialData();
                }
            }).subscribe(new Consumer() { // from class: r.n
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MapPresenter.E((Boolean) obj);
                }
            }, new Consumer() { // from class: r.o
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MapPresenter.this.F((Throwable) obj);
                }
            });
        } catch (DataException e2) {
            e2.printStackTrace();
            this.f6935g.makeSnackBar(R.string.p_map_error_save_record, -1, e2.getMessage());
            LogUtil.e(e2.toString());
        }
    }

    static /* synthetic */ class b {

        /* renamed from: a, reason: collision with root package name */
        static final /* synthetic */ int[] f6946a;

        /* renamed from: b, reason: collision with root package name */
        static final /* synthetic */ int[] f6947b;

        static {
            int[] iArr = new int[GPSManager.GPSException.CAUSE.values().length];
            f6947b = iArr;
            try {
                iArr[GPSManager.GPSException.CAUSE.GPS_NOT_PROVIDED.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                f6947b[GPSManager.GPSException.CAUSE.MOCK_LOCATION.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            int[] iArr2 = new int[e.values().length];
            f6946a = iArr2;
            try {
                iArr2[e.PAUSED.ordinal()] = 1;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                f6946a[e.RUNNING.ordinal()] = 2;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void F(Throwable th) {
        this.f6935g.makeSnackBar(R.string.p_map_error_provide_track, -1, th.getMessage());
        LogUtil.e(th.toString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void G(Location location) {
        LogUtil.d(String.format("Current location (%f, %f).", Double.valueOf(location.getLongitude()), Double.valueOf(location.getLatitude())));
        Point point = new Point(0, 0, location.getLongitude(), location.getLatitude(), 0);
        int i2 = b.f6946a[this.f6929a.ordinal()];
        if (i2 == 1) {
            if (!this.f6940l) {
                this.f6935g.getAMap().animateCamera(CameraUpdateFactory.changeLatLng(point.toLatLng()));
            }
            this.f6935g.getLocationListener().onLocationChanged(location);
            boolean a2 = this.f6943o.a(point, location.getAccuracy(), location.getTime());
            this.f6944p = a2;
            if (!a2) {
                this.f6943o.c();
                VibratorUtil.vibrate();
                this.f6935g.makeSnackBar(R.string.p_map_error_invalid_speed, -1, new Object[0]);
            }
            this.f6942n = point;
        } else if (i2 != 2) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (!this.f6940l) {
                this.f6935g.getAMap().animateCamera(CameraUpdateFactory.changeLatLng(latLng));
            }
            this.f6935g.getLocationListener().onLocationChanged(location);
        } else {
            if (!this.f6940l) {
                this.f6935g.getAMap().animateCamera(CameraUpdateFactory.changeLatLng(point.toLatLng()));
            }
            this.f6935g.getLocationListener().onLocationChanged(location);
            if (this.f6942n != null) {
                this.f6935g.getAMap().addPolyline(new PolylineOptions().add(this.f6942n.toLatLng(), point.toLatLng()).color(this.f6935g.getFragmentContext().getResources().getColor(R.color.map_route)));
                this.f6943o.b(point, location.getAccuracy(), location.getTime());
                MapContract.View view = this.f6935g;
                double d2 = this.f6930b;
                double d3 = d2 / 1000.0d;
                double d4 = this.f6931c;
                view.updateTextView(d3, d4, SpeedHelper.toUnitOf(this.f6933e, d2 / d4), this.f6933e == SpeedHelper.SPEED_UNIT.C);
            }
            this.f6942n = point;
        }
        if (this.f6934f.size() - this.f6937i >= 30) {
            t();
            this.f6937i = this.f6934f.size();
        }
    }

    private void J() {
        this.f6935g.switchToRunning();
        this.f6935g.makeToast(R.string.p_map_resume_running, 0, new Object[0]);
        this.f6929a = e.RUNNING;
        Point point = this.f6942n;
        if (point == null || !this.f6944p) {
            return;
        }
        point.setStatus(1);
        this.f6934f.add(this.f6942n);
    }

    private void K() {
        this.f6935g.setLocatingPointEnabled(true);
        GPSManager.g(new GPSManager.GPSLocationListener() { // from class: r.t
            @Override // cn.edu.pku.pkurunner.Map.GPSManager.GPSLocationListener
            public final void onLocationUpdate(Location location) {
                MapPresenter.this.G(location);
            }
        }, "presenter-location");
        GPSManager.h(new GPSManager.GPSStatusListener() { // from class: r.u
            @Override // cn.edu.pku.pkurunner.Map.GPSManager.GPSStatusListener
            public final void onStatusUpdate(int i2, int i3, double d2) {
                MapPresenter.this.I(i2, i3, d2);
            }
        }, "presenter-location");
    }

    private void L() {
        GPSManager.r("presenter-location");
        GPSManager.s("presenter-location");
        this.f6935g.setLocatingPointEnabled(false);
    }

    private void M() {
        this.f6935g.switchToPaused();
        this.f6935g.makeToast(R.string.p_map_pause_running, 0, new Object[0]);
        this.f6929a = e.PAUSED;
        if (this.f6934f.isEmpty()) {
            return;
        }
        ((Point) this.f6934f.get(this.f6934f.size() - 1)).setStatus(2);
    }

    private void N() {
        this.f6935g.showNotification();
        this.f6935g.switchToRunning();
        this.f6935g.makeToast(R.string.p_map_start_running, 0, new Object[0]);
        this.f6935g.indicatorShowUpAnimation();
        this.f6935g.requireWakeLock();
        this.f6940l = false;
        this.f6941m = this.f6935g.registerMapCenterHelper().doOnNext(new Consumer() { // from class: r.j
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                MapPresenter.this.C((Boolean) obj);
            }
        }).debounce(10L, TimeUnit.SECONDS).subscribe(new Consumer() { // from class: r.k
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                MapPresenter.this.B((Boolean) obj);
            }
        });
        K();
        this.f6929a = e.RUNNING;
        this.f6930b = 0.0d;
        this.f6931c = 0.0d;
        this.f6932d = 0;
        this.f6942n = null;
        this.f6934f.clear();
        this.f6943o.d();
        if (!this.f6936h.f6955c) {
            this.f6936h.d();
        }
        this.f6936h.f();
    }

    static /* synthetic */ double r(MapPresenter mapPresenter, double d2) {
        double d3 = mapPresenter.f6930b + d2;
        mapPresenter.f6930b = d3;
        return d3;
    }

    static /* synthetic */ double s(MapPresenter mapPresenter, double d2) {
        double d3 = mapPresenter.f6931c + d2;
        mapPresenter.f6931c = d3;
        return d3;
    }

    private void t() {
        ArrayList arrayList = this.f6934f;
        try {
            Data.provideTrackForPartialRecord(Data.savePartialRecordToDatabase(new PartialRecord((int) this.f6930b, (int) this.f6931c, new Date(), this.f6932d)), arrayList.subList(this.f6937i, arrayList.size())).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: r.l
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MapPresenter.this.v((Boolean) obj);
                }
            }, new Consumer() { // from class: r.m
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MapPresenter.this.w((Throwable) obj);
                }
            });
        } catch (DataException e2) {
            e2.printStackTrace();
            this.f6935g.makeSnackBar(R.string.p_map_error_auto_save_record, -1, e2.getMessage());
            LogUtil.e(e2.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void v(Boolean bool) {
        this.f6935g.makeToast(R.string.p_map_notice_auto_save, 0, new Object[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void w(Throwable th) {
        this.f6935g.makeSnackBar(R.string.p_map_error_provide_track, -1, th.getMessage());
        LogUtil.e(th.toString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void x(GPSManager.b.a aVar) {
        if (aVar.f6901b) {
            this.f6935g.updateWaitingDialog(R.string.p_map_try_locating_accuracy, aVar.f6902c);
            return;
        }
        this.f6935g.dismissWaitingDialog();
        if (aVar.f6900a) {
            N();
        } else {
            this.f6935g.makeToast(R.string.p_map_locating_accuracy_fail, 0, new Object[0]);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void y(Throwable th) {
        this.f6935g.dismissWaitingDialog();
        if (!(th instanceof GPSManager.GPSException)) {
            th.printStackTrace();
            this.f6935g.makeToast(R.string.p_map_error_strange, 0, th);
            return;
        }
        int i2 = b.f6947b[((GPSManager.GPSException) th).f6895a.ordinal()];
        if (i2 == 1) {
            this.f6935g.makeToast(R.string.p_map_error_gps_not_enabled, 0, new Object[0]);
            this.f6935g.openGPSSettings();
        } else if (i2 != 2) {
            this.f6935g.makeToast(R.string.p_map_error_other, 0, th.getMessage());
        } else {
            this.f6935g.makeToast(R.string.p_map_error_mock_location, 0, new Object[0]);
            this.f6935g.openDevelopSettings();
        }
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public boolean isRunning() {
        return this.f6929a == e.RUNNING;
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public boolean isRunningPaused() {
        return this.f6929a == e.PAUSED;
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public boolean onFabPauseClick(boolean z2) {
        int i2 = b.f6946a[this.f6929a.ordinal()];
        if (i2 == 1) {
            J();
        } else if (i2 != 2) {
            LogUtil.d("Unexpected branch when pause button pressed.");
        } else {
            if (!z2) {
                this.f6935g.makeToast(R.string.p_map_long_press_to_pause, 0, new Object[0]);
                return true;
            }
            M();
        }
        return true;
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public boolean onFabRunClick(boolean z2) {
        if (this.f6929a == e.IDLE) {
            if (!this.f6935g.checkKeepAlive()) {
                return true;
            }
            if (!u()) {
                this.f6935g.makeSnackBar(R.string.p_map_notice_wrong_time_period, 0, new Object[0]);
            }
            if (!TrueTime.isInitialized()) {
                new AlertDialog.Builder(this.f6935g.getFragmentContext()).setTitle(R.string.p_map_notice_network_error).setPositiveButton(R.string.p_map_notice_network_error_confirm, new a()).create().show();
                return true;
            }
            f6928q = TrueTime.now();
            this.f6935g.makeWaitingDialog(R.string.p_map_try_locating, new Object[0]);
            GPSManager.k().subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.Map.e
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MapPresenter.this.x((GPSManager.b.a) obj);
                }
            }, new Consumer() { // from class: r.q
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    MapPresenter.this.y((Throwable) obj);
                }
            });
        } else if (z2) {
            new AlertDialog.Builder(this.f6935g.getFragmentContext()).setTitle(R.string.p_map_dialog_title).setMessage(R.string.p_map_dialog_content).setPositiveButton(R.string.p_map_dialog_positive, new DialogInterface.OnClickListener() { // from class: r.r
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i2) {
                    MapPresenter.this.z(dialogInterface, i2);
                }
            }).setNegativeButton(R.string.p_map_dialog_negative, new DialogInterface.OnClickListener() { // from class: r.s
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i2) {
                    dialogInterface.cancel();
                }
            }).create().show();
        } else {
            this.f6935g.makeToast(R.string.p_map_long_press_to_finish, 0, new Object[0]);
        }
        return true;
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public void onStep() {
        if (this.f6929a == e.RUNNING) {
            this.f6932d++;
        }
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public void stopAndSwitchToIdle() {
        this.f6935g.dismissNotification();
        this.f6935g.switchToReset();
        this.f6935g.releaseWakeLock();
        this.f6941m.dispose();
        this.f6935g.unregisterMapCenterHelper();
        if (!this.f6938j) {
            L();
        }
        if (!this.f6939k) {
            pauseGPSAssistant();
        }
        this.f6929a = e.IDLE;
        this.f6937i = 0;
        this.f6936h.e();
    }

    public MapPresenter(@NonNull MapContract.View view) {
        this.f6935g = view;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void H(int i2, int i3, double d2) {
        this.f6935g.setAssistantText(R.string.p_map_gps_info, d2 / 80.0d, Integer.valueOf(i2), Integer.valueOf(i3), Double.valueOf(d2));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void I(int i2, int i3, double d2) {
        if (d2 < 80.0d) {
            this.f6935g.notifyGPSInfo();
            this.f6935g.setAssistantText(R.string.p_map_gps_weak, d2 / 80.0d, new Object[0]);
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

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void z(DialogInterface dialogInterface, int i2) {
        stopAndSwitchToIdle();
        syncData();
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public void syncOptionsMenu(Menu menu) {
        MenuItem findItem = menu.findItem(R.id.f_m_map_checkbox_location_auto_on);
        MenuItem findItem2 = menu.findItem(R.id.f_m_map_checkbox_gps_status_on);
        findItem.setChecked(this.f6938j);
        findItem2.setChecked(this.f6939k);
    }

    @Override // cn.edu.pku.pkurunner.Map.MapContract.Presenter
    public void updateUnitPreference() {
        SpeedHelper.SPEED_UNIT speedUnitPreference = Data.getSpeedUnitPreference();
        this.f6933e = speedUnitPreference;
        Data.setSpeedUnitPreference(speedUnitPreference);
        this.f6935g.updateTextSci(this.f6933e);
    }
}
