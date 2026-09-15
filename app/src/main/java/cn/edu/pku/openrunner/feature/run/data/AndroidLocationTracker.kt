package cn.edu.pku.openrunner.feature.run.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import cn.edu.pku.openrunner.feature.run.domain.TrackPoint
import cn.edu.pku.openrunner.feature.run.domain.Wgs84Gcj02

data class LocationSample(
    val point: TrackPoint,
    val accuracyMeters: Float,
    val timestampMillis: Long
)

class AndroidLocationTracker(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var listener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun start(onLocation: (LocationSample) -> Unit): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return false

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (providers.isEmpty()) return false

        fun dispatch(location: Location) {
            if (location.isMockLocation()) return
            // Match the legacy GPSManager: map and record points use GCJ-02.
            val (longitude, latitude) = Wgs84Gcj02.convert(
                location.longitude,
                location.latitude
            )
            onLocation(
                LocationSample(
                    point = TrackPoint(
                        longitude = longitude,
                        latitude = latitude
                    ),
                    accuracyMeters = location.accuracy,
                    timestampMillis = location.time
                )
            )
        }

        val newListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                dispatch(location)
            }
        }
        stop()
        listener = newListener
        providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull(Location::getTime)?.let(::dispatch)
        providers.forEach { provider ->
            locationManager.requestLocationUpdates(provider, 1_000L, 0f, newListener)
        }
        return true
    }

    fun stop() {
        listener?.let(locationManager::removeUpdates)
        listener = null
    }

    private fun Location.isMockLocation(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isMock
        } else {
            @Suppress("DEPRECATION")
            isFromMockProvider
        }
    }
}
