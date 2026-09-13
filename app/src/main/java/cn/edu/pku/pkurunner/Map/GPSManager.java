package cn.edu.pku.pkurunner.Map;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import java.util.HashMap;
import java.util.Iterator;
import org.xutils.common.util.LogUtil;

/* JADX INFO: Access modifiers changed from: package-private */
public abstract class GPSManager {

    /* renamed from: a, reason: collision with root package name */
    private static HashMap f6889a = new HashMap();

    /* renamed from: b, reason: collision with root package name */
    private static HashMap f6890b = new HashMap();

    /* renamed from: c, reason: collision with root package name */
    private static LocationManager f6891c;

    /* renamed from: d, reason: collision with root package name */
    private static LocationListener f6892d;

    /* renamed from: e, reason: collision with root package name */
    private static GpsStatus.Listener f6893e;

    /* renamed from: f, reason: collision with root package name */
    private static Location f6894f;

    public interface GPSLocationListener {
        void onLocationUpdate(Location location);
    }

    public interface GPSStatusListener {
        void onStatusUpdate(GpsStatus gpsStatus);
    }

    static class a implements LocationListener {
        @Override // android.location.LocationListener
        public void onLocationChanged(Location location) {
            LogUtil.d("Incoming new location.");
            GPSManager.i(location);
        }

        @Override // android.location.LocationListener
        public void onProviderDisabled(String str) {
            LogUtil.d(str + " is disabled.");
        }

        @Override // android.location.LocationListener
        public void onProviderEnabled(String str) {
            LogUtil.d(str + " is enabled.");
        }

        @Override // android.location.LocationListener
        public void onStatusChanged(String str, int i2, Bundle bundle) {
            LogUtil.d(str + " status changed.");
        }

        a() {
        }
    }

    static class b extends CountDownTimer {

        /* renamed from: a, reason: collision with root package name */
        private int f6897a;

        /* renamed from: b, reason: collision with root package name */
        private long f6898b;

        /* renamed from: c, reason: collision with root package name */
        private ObservableEmitter f6899c;

        private void a(boolean z2) {
            if (!z2) {
                this.f6899c.onNext(new a(false, false, "Not accurate"));
                return;
            }
            GPSManager.p();
            cancel();
            this.f6899c.onNext(new a(true, false, "Accurate"));
        }

        @Override // android.os.CountDownTimer
        public void onFinish() {
            a(false);
        }

        static class a {

            /* renamed from: a, reason: collision with root package name */
            public boolean f6900a;

            /* renamed from: b, reason: collision with root package name */
            public boolean f6901b;

            /* renamed from: c, reason: collision with root package name */
            public String f6902c;

            public a(boolean z2, boolean z3, String str) {
                this.f6900a = z2;
                this.f6901b = z3;
                this.f6902c = str;
            }
        }

        b(long j2, long j3, ObservableEmitter observableEmitter) {
            super(j2, j3);
            this.f6897a = 15;
            this.f6898b = j2;
            this.f6899c = observableEmitter;
            GPSManager.t();
        }

        @Override // android.os.CountDownTimer
        public void onTick(long j2) {
            double accuracy;
            if (GPSManager.f6894f == null) {
                accuracy = Double.POSITIVE_INFINITY;
            } else {
                accuracy = GPSManager.f6894f.getAccuracy();
            }
            LogUtil.d("Accuracy: " + String.valueOf(accuracy));
            if (GPSManager.f6894f != null && GPSManager.f6894f.isFromMockProvider()) {
                this.f6899c.onError(new GPSException(GPSException.CAUSE.MOCK_LOCATION));
                GPSManager.p();
                cancel();
                return;
            }
            if (accuracy <= this.f6897a) {
                a(true);
            }
            if (j2 < this.f6898b / 2) {
                this.f6897a += 10;
            }
            this.f6899c.onNext(new a(false, true, accuracy + "/" + this.f6897a));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void i(Location location) {
        LogUtil.d(String.format("Location at %s now.", location.toString()));
        f6894f = location;
        if (location.isFromMockProvider()) {
            return;
        }
        double[] convert = WGS84_GCJ02.convert(location.getLongitude(), location.getLatitude());
        location.setLongitude(convert[0]);
        location.setLatitude(convert[1]);
        LogUtil.d(f6889a.size() + " listeners");
        Iterator it = f6889a.values().iterator();
        while (it.hasNext()) {
            ((GPSLocationListener) it.next()).onLocationUpdate(location);
        }
    }

    static class GPSException extends Exception {

        /* renamed from: a, reason: collision with root package name */
        CAUSE f6895a;

        public enum CAUSE {
            GPS_NOT_PROVIDED,
            MOCK_LOCATION
        }

        GPSException(CAUSE cause) {
            this.f6895a = cause;
        }
    }

    static void g(GPSLocationListener gPSLocationListener, String str) {
        if (f6889a.isEmpty()) {
            t();
        }
        f6889a.put(str, gPSLocationListener);
    }

    static void h(GPSStatusListener gPSStatusListener, String str) {
        if (f6890b.isEmpty()) {
            u();
        }
        f6890b.put(str, gPSStatusListener);
    }

    private static void j(GpsStatus gpsStatus) {
        LogUtil.d("GPSStatus updated! " + gpsStatus);
        Iterator it = f6890b.values().iterator();
        while (it.hasNext()) {
            ((GPSStatusListener) it.next()).onStatusUpdate(gpsStatus);
        }
    }

    static Observable k() {
        return Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.Map.b
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                GPSManager.l(observableEmitter);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void l(ObservableEmitter observableEmitter) {
        if (f6891c.isProviderEnabled("gps")) {
            new b(10010L, 1000L, observableEmitter).start();
        } else {
            observableEmitter.onError(new GPSException(GPSException.CAUSE.GPS_NOT_PROVIDED));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void m(int i2) {
        try {
            j(f6891c.getGpsStatus(null));
        } catch (SecurityException e2) {
            e2.printStackTrace();
        }
    }

    static void n(Context context) {
        f6891c = (LocationManager) context.getSystemService("location");
        f6892d = new a();
        f6893e = new GpsStatus.Listener() { // from class: cn.edu.pku.pkurunner.Map.a
            @Override // android.location.GpsStatus.Listener
            public final void onGpsStatusChanged(int i2) {
                GPSManager.m(i2);
            }
        };
    }

    static void o() {
        LocationListener locationListener;
        LocationManager locationManager = f6891c;
        if (locationManager != null && (locationListener = f6892d) != null) {
            locationManager.removeUpdates(locationListener);
            f6891c.removeGpsStatusListener(f6893e);
        }
        f6889a.clear();
        f6890b.clear();
        f6892d = null;
        f6891c = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void p() {
        if (f6891c == null || f6892d == null || !f6889a.isEmpty()) {
            return;
        }
        f6891c.removeUpdates(f6892d);
    }

    private static void q() {
        if (f6891c == null || f6892d == null || !f6890b.isEmpty()) {
            return;
        }
        f6891c.removeGpsStatusListener(f6893e);
    }

    static void r(String str) {
        f6889a.remove(str);
        if (f6889a.isEmpty()) {
            p();
        }
    }

    static void s(String str) {
        f6890b.remove(str);
        if (f6890b.isEmpty()) {
            q();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void t() {
        try {
            f6891c.requestLocationUpdates("gps", 1000L, BitmapDescriptorFactory.HUE_RED, f6892d);
        } catch (SecurityException e2) {
            e2.printStackTrace();
        }
    }

    private static void u() {
        try {
            f6891c.addGpsStatusListener(f6893e);
        } catch (SecurityException e2) {
            e2.printStackTrace();
        }
    }
}
