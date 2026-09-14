package cn.edu.pku.pkurunner.Map;

import android.content.Context;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import java.util.HashMap;
import java.util.Iterator;
import org.xutils.common.util.LogUtil;

public abstract class GPSManager {

    private static HashMap locationListeners = new HashMap();

    private static HashMap statusListeners = new HashMap();

    private static LocationManager locationManager;

    private static LocationListener locationListener;

    private static GpsStatus.Listener gpsStatusListener;

    private static Location lastLocation;

    private static GnssStatus.Callback gnssStatusCallback;

    public interface GPSLocationListener {
        void onLocationUpdate(Location location);
    }

    public interface GPSStatusListener {
        void onStatusUpdate(int index, int index2, double value);
    }

    static class LocationUpdateListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            LogUtil.d("Incoming new location.");
            GPSManager.dispatchLocation(location);
        }

        @Override
        public void onProviderDisabled(String str) {
            LogUtil.d(str + " is disabled.");
        }

        @Override
        public void onProviderEnabled(String str) {
            LogUtil.d(str + " is enabled.");
        }

        @Override
        public void onStatusChanged(String str, int index, Bundle bundle) {
            LogUtil.d(str + " status changed.");
        }

        LocationUpdateListener() {
        }
    }

    static class AccuracyCountdown extends CountDownTimer {

        private int accuracyThreshold;

        private long totalMillis;

        private ObservableEmitter emitter;

        private void finish(boolean z2) {
            if (!z2) {
                this.emitter.onNext(new Result(false, false, "Not accurate"));
                return;
            }
            GPSManager.stopLocationUpdates();
            cancel();
            this.emitter.onNext(new Result(true, false, "Accurate"));
        }

        @Override
        public void onFinish() {
            finish(false);
        }

        static class Result {

            public boolean accurate;

            public boolean waiting;

            public String message;

            public Result(boolean z2, boolean z3, String str) {
                this.accurate = z2;
                this.waiting = z3;
                this.message = str;
            }
        }

        AccuracyCountdown(long j2, long j3, ObservableEmitter observableEmitter) {
            super(j2, j3);
            this.accuracyThreshold = 15;
            this.totalMillis = j2;
            this.emitter = observableEmitter;
            GPSManager.requestLocationUpdates();
        }

        @Override
        public void onTick(long j2) {
            double accuracy;
            if (GPSManager.lastLocation == null) {
                accuracy = Double.POSITIVE_INFINITY;
            } else {
                accuracy = GPSManager.lastLocation.getAccuracy();
            }
            LogUtil.d("Accuracy: " + String.valueOf(accuracy));
            if (GPSManager.lastLocation != null && GPSManager.lastLocation.isFromMockProvider()) {
                this.emitter.onError(new GPSException(GPSException.CAUSE.MOCK_LOCATION));
                GPSManager.stopLocationUpdates();
                cancel();
                return;
            }
            if (accuracy <= this.accuracyThreshold) {
                finish(true);
            }
            if (j2 < this.totalMillis / 2) {
                this.accuracyThreshold += 10;
            }
            this.emitter.onNext(new Result(false, true, accuracy + "/" + this.accuracyThreshold));
        }
    }

    public static void dispatchLocation(Location location) {
        LogUtil.d(String.format("Location at %s now.", location.toString()));
        lastLocation = location;
        if (location.isFromMockProvider()) {
            return;
        }
        double[] convert = WGS84_GCJ02.convert(location.getLongitude(), location.getLatitude());
        location.setLongitude(convert[0]);
        location.setLatitude(convert[1]);
        LogUtil.d(locationListeners.size() + " listeners");
        Iterator it = locationListeners.values().iterator();
        while (it.hasNext()) {
            ((GPSLocationListener) it.next()).onLocationUpdate(location);
        }
    }

    static class GPSException extends Exception {

        CAUSE cause;

        public enum CAUSE {
            GPS_NOT_PROVIDED,
            MOCK_LOCATION
        }

        GPSException(CAUSE cause) {
            this.cause = cause;
        }
    }

    static void addLocationListener(GPSLocationListener gPSLocationListener, String str) {
        if (locationListeners.isEmpty()) {
            requestLocationUpdates();
        }
        locationListeners.put(str, gPSLocationListener);
    }

    static void addStatusListener(GPSStatusListener gPSStatusListener, String str) {
        if (statusListeners.isEmpty()) {
            registerStatusListener();
        }
        statusListeners.put(str, gPSStatusListener);
    }

    private static void dispatchGpsStatus(GpsStatus gpsStatus) {
        LogUtil.d("GPSStatus updated! " + gpsStatus);
        int index = 0;
        int index2 = 0;
        double value = 0.0d;
        for (GpsSatellite gpsSatellite : gpsStatus.getSatellites()) {
            value += Math.pow(2.0d, gpsSatellite.getSnr() / 10.0d);
            if (gpsSatellite.usedInFix()) {
                index++;
            }
            index2++;
        }
        dispatchStatus(index, index2, value);
    }

    public static /* synthetic */ void dispatchGnssStatus(GnssStatus gnssStatus) {
        LogUtil.d("GnssStatus updated! " + gnssStatus);
        int satelliteCount = gnssStatus.getSatelliteCount();
        int index = 0;
        double value = 0.0d;
        for (int index2 = 0; index2 < satelliteCount; index2++) {
            value += Math.pow(2.0d, gnssStatus.getCn0DbHz(index2) / 10.0d);
            if (gnssStatus.usedInFix(index2)) {
                index++;
            }
        }
        dispatchStatus(index, satelliteCount, value);
    }

    private static void dispatchStatus(int index, int index2, double value) {
        Iterator it = statusListeners.values().iterator();
        while (it.hasNext()) {
            ((GPSStatusListener) it.next()).onStatusUpdate(index, index2, value);
        }
    }

    static Observable waitForAccurateLocation() {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                GPSManager.startAccuracyTimer(observableEmitter);
            }
        });
    }

    public static /* synthetic */ void startAccuracyTimer(ObservableEmitter observableEmitter) {
        if (locationManager.isProviderEnabled("gps")) {
            new AccuracyCountdown(10010L, 1000L, observableEmitter).start();
        } else {
            observableEmitter.onError(new GPSException(GPSException.CAUSE.GPS_NOT_PROVIDED));
        }
    }

    public static /* synthetic */ void onLegacyGpsStatusChanged(int index) {
        try {
            dispatchGpsStatus(locationManager.getGpsStatus(null));
        } catch (SecurityException | UnsupportedOperationException e2) {
            e2.printStackTrace();
        }
    }

    static void initialize(Context context) {
        locationManager = (LocationManager) context.getSystemService("location");
        locationListener = new LocationUpdateListener();
        if (Build.VERSION.SDK_INT >= 24) {
            gnssStatusCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(GnssStatus gnssStatus) {
                    GPSManager.dispatchGnssStatus(gnssStatus);
                }
            };
        } else {
            gpsStatusListener = new GpsStatus.Listener() {
                @Override
                public final void onGpsStatusChanged(int index) {
                    GPSManager.onLegacyGpsStatusChanged(index);
                }
            };
        }
    }

    static void release() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            if (Build.VERSION.SDK_INT >= 24) {
                locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
            } else {
                locationManager.removeGpsStatusListener(gpsStatusListener);
            }
        }
        locationListeners.clear();
        statusListeners.clear();
        locationListener = null;
        locationManager = null;
    }

    public static void stopLocationUpdates() {
        if (locationManager == null || locationListener == null || !locationListeners.isEmpty()) {
            return;
        }
        locationManager.removeUpdates(locationListener);
    }

    private static void stopStatusUpdates() {
        if (locationManager == null || locationListener == null || !statusListeners.isEmpty()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 24) {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
        } else {
            locationManager.removeGpsStatusListener(gpsStatusListener);
        }
    }

    static void removeLocationListener(String str) {
        locationListeners.remove(str);
        if (locationListeners.isEmpty()) {
            stopLocationUpdates();
        }
    }

    static void removeStatusListener(String str) {
        statusListeners.remove(str);
        if (statusListeners.isEmpty()) {
            stopStatusUpdates();
        }
    }

    public static void requestLocationUpdates() {
        try {
            locationManager.requestLocationUpdates("gps", 1000L, BitmapDescriptorFactory.HUE_RED, locationListener);
        } catch (SecurityException e2) {
            e2.printStackTrace();
        }
    }

    private static void registerStatusListener() {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                locationManager.registerGnssStatusCallback(gnssStatusCallback, null);
            } else {
                locationManager.addGpsStatusListener(gpsStatusListener);
            }
        } catch (SecurityException | UnsupportedOperationException e2) {
            e2.printStackTrace();
        }
    }
}
