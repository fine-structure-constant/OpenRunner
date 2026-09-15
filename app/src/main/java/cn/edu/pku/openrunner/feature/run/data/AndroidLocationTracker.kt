package cn.edu.pku.openrunner.feature.run.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import cn.edu.pku.openrunner.MainActivity
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.run.domain.TrackPoint
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener

data class LocationSample(
    val point: TrackPoint,
    val accuracyMeters: Float,
    val timestampMillis: Long
)

/**
 * Uses AMap's sport location mode and foreground-location capability. The foreground
 * notification plus a bounded partial wake lock keeps GPS and sensor callbacks active
 * when the app is backgrounded or the screen is switched off during a run.
 */
class AndroidLocationTracker(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val wakeLock = (appContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${appContext.packageName}:run-tracking")
        .apply { setReferenceCounted(false) }
    private var client: AMapLocationClient? = null
    private var callback: ((LocationSample) -> Unit)? = null
    private var backgroundTracking = false

    private val listener = AMapLocationListener { location ->
        if (location == null || location.errorCode != 0) return@AMapLocationListener
        if (location.isMockLocation()) return@AMapLocationListener
        callback?.invoke(
            LocationSample(
                // AMap location results are GCJ-02 by default, matching the map and upload format.
                point = TrackPoint(
                    longitude = location.longitude,
                    latitude = location.latitude
                ),
                accuracyMeters = location.accuracy.coerceAtLeast(0f),
                timestampMillis = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
            )
        )
    }

    @SuppressLint("MissingPermission")
    fun start(onLocation: (LocationSample) -> Unit): Boolean {
        if (!hasLocationPermission()) return false
        val locationClient = client ?: runCatching {
            AMapLocationClient(appContext).also {
                it.setLocationListener(listener)
                client = it
            }
        }.getOrNull() ?: return false

        callback = onLocation
        val option = AMapLocationClientOption().apply {
            locationPurpose = AMapLocationClientOption.AMapLocationPurpose.Sport
            interval = LOCATION_INTERVAL_MILLIS
            isNeedAddress = false
            isMockEnable = false
            isLocationCacheEnable = false
        }
        locationClient.stopLocation()
        locationClient.setLocationOption(option)
        locationClient.startLocation()
        return true
    }

    /** Must be called while the activity is visible, immediately after the user starts a run. */
    fun enableBackgroundTracking(): Boolean {
        if (backgroundTracking) return true
        val locationClient = client ?: return false
        return runCatching {
            ensureNotificationChannel()
            locationClient.enableBackgroundLocation(NOTIFICATION_ID, buildNotification())
            if (!wakeLock.isHeld) wakeLock.acquire(MAX_WAKE_LOCK_MILLIS)
            backgroundTracking = true
            true
        }.getOrDefault(false)
    }

    fun disableBackgroundTracking() {
        if (backgroundTracking) {
            runCatching { client?.disableBackgroundLocation(true) }
            backgroundTracking = false
        }
        if (wakeLock.isHeld) wakeLock.release()
    }

    fun stop() {
        disableBackgroundTracking()
        client?.stopLocation()
        callback = null
    }

    fun destroy() {
        stop()
        client?.onDestroy()
        client = null
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                appContext.getString(R.string.run_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = appContext.getString(R.string.run_notification_channel_description)
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(): Notification {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(appContext.getString(R.string.run_notification_title))
            .setContentText(appContext.getString(R.string.run_notification_text))
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun AMapLocation.isMockLocation(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        @Suppress("DEPRECATION")
        isFromMockProvider
    }

    companion object {
        private const val LOCATION_INTERVAL_MILLIS = 1_000L
        private const val MAX_WAKE_LOCK_MILLIS = 6L * 60L * 60L * 1_000L
        private const val NOTIFICATION_ID = 2_401
        private const val NOTIFICATION_CHANNEL_ID = "active_run_tracking"
    }
}
