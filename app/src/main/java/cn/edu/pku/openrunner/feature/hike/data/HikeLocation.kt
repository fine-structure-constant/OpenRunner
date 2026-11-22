package cn.edu.pku.openrunner.feature.hike.data

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import cn.edu.pku.openrunner.feature.hike.domain.GeoPoint

class HikeLocation(
    context: Context,
    private val onFix: (RecordedFix) -> Unit,
    private val onAvailability: (Boolean) -> Unit
) : LocationListener {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var registered = false

    fun start(interval: Long, looper: Looper): Boolean {
        if (registered) return true
        return try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, 0f, this, looper)
            registered = true
            onAvailability(manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun stop() {
        if (registered) manager.removeUpdates(this)
        registered = false
    }

    override fun onLocationChanged(location: Location) {
        val monotonicMs = location.elapsedRealtimeNanos / 1_000_000
        if (SystemClock.elapsedRealtime() - monotonicMs !in 0..20_000L) return
        if (if (Build.VERSION.SDK_INT >= 31) location.isMock else location.isFromMockProvider) return
        if (!location.hasAccuracy() || !location.accuracy.isFinite() || location.accuracy < 0f) return
        val altitude = location.altitude.takeIf { location.hasAltitude() && it.isFinite() }
        val point = GeoPoint(location.latitude, location.longitude, altitude)
        if (!point.valid) return
        val accuracy = if (Build.VERSION.SDK_INT >= 26 && location.hasVerticalAccuracy()) {
            location.verticalAccuracyMeters.takeIf { it.isFinite() && it >= 0f }
        } else null
        onFix(RecordedFix(point, location.time, location.accuracy, accuracy, monotonicMs))
    }

    override fun onProviderEnabled(provider: String) = onAvailability(true)
    override fun onProviderDisabled(provider: String) = onAvailability(false)

    @Deprecated("Legacy LocationListener callback")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}
