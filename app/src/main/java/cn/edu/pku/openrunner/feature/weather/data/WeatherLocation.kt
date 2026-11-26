package cn.edu.pku.openrunner.feature.weather.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class WeatherLocation(private val context: Context) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val executor = ContextCompat.getMainExecutor(context)
    private val hasPrecisePermission: Boolean get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val hasPermission: Boolean get() = hasPrecisePermission || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    val isEnabled: Boolean get() = LocationManagerCompat.isLocationEnabled(manager)

    suspend fun current(): Location? = withTimeoutOrNull(15_000L) {
        val enabled = manager.getProviders(true)
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (hasPrecisePermission) add(LocationManager.GPS_PROVIDER)
        }.filter { it in enabled }

        coroutineScope {
            val results = Channel<Location?>(providers.size)
            val requests = providers.map { provider -> launch { results.send(fixFrom(provider)) } }
            try {
                repeat(providers.size) {
                    val fix = results.receive()
                    if (fix != null && isRecent(fix)) return@coroutineScope fix
                }
                null
            } finally {
                requests.forEach { it.cancel() }
                results.cancel()
            }
        }
    }

    @SuppressLint("MissingPermission") // The caller checks permission, and revocation is handled below.
    @Suppress("DEPRECATION") // androidx.core 1.12 provides the CancellationSignal compatibility overload.
    private suspend fun fixFrom(provider: String): Location? = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, executor) {
                continuation.resume(it)
            }
        } catch (_: SecurityException) {
            continuation.resume(null)
        } catch (_: IllegalArgumentException) {
            continuation.resume(null)
        }
    }

    suspend fun placeName(fix: Location): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.SIMPLIFIED_CHINESE)
        val latitude = (fix.latitude * 100).roundToInt() / 100.0
        val longitude = (fix.longitude * 100).roundToInt() / 100.0
        val address = try {
            if (Build.VERSION.SDK_INT >= 33) {
                withTimeoutOrNull(3_000L) {
                    suspendCancellableCoroutine<Address?> { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: List<Address>) = continuation.resume(addresses.firstOrNull())
                            override fun onError(errorMessage: String?) = continuation.resume(null)
                        })
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        if (address == null) return null
        val names = listOfNotNull(address.locality ?: address.subAdminArea ?: address.adminArea, address.subLocality)
            .filter(String::isNotBlank).distinct()
        return names.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun isRecent(location: Location): Boolean =
        !LocationCompat.isMock(location) &&
            SystemClock.elapsedRealtime() - LocationCompat.getElapsedRealtimeMillis(location) in 0..60_000L &&
            location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
