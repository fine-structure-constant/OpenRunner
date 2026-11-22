package cn.edu.pku.openrunner.feature.hike.domain

import kotlin.math.*

data class GeoPoint(val latitude: Double, val longitude: Double, val altitude: Double? = null) {
    val valid: Boolean get() = latitude in -90.0..90.0 && longitude in -180.0..180.0
}

data class HikeWaypoint(val point: GeoPoint, val name: String)
data class HikeRoute(
    val name: String,
    val lines: List<List<GeoPoint>>,
    val waypoints: List<HikeWaypoint> = emptyList()
) {
    fun points(): Sequence<GeoPoint> = lines.asSequence().flatten() + waypoints.asSequence().map { it.point }
}

data class HikeSample(
    val point: GeoPoint,
    val timestamp: Long,
    val section: Int,
    val segmentAscent: Double?,
    val verticalAccuracy: Float? = null
)

class HikeTrack {
    private val samples = mutableListOf<HikeSample>()
    private val elevation = ElevationGain()
    val last: HikeSample? get() = samples.lastOrNull()
    var ascent = 0.0
        private set
    var distance = 0.0
        private set

    fun append(point: GeoPoint, time: Long, verticalAccuracy: Float?, breakBefore: Boolean = false): HikeSample {
        val previous = last
        val newSection = previous == null || breakBefore
        if (newSection) elevation.reset()
        val gain = elevation.append(point.altitude, verticalAccuracy)
        val section = (previous?.section ?: -1) + if (newSection) 1 else 0
        if (!newSection) distance += distanceMeters(previous.point, point)
        if (gain != null) ascent += gain
        return HikeSample(point, time, section, gain, verticalAccuracy).also(samples::add)
    }

    fun snapshot(): List<HikeSample> = samples.toList()
}

/** Track extrema with a dead band so GNSS jitter does not count as repeated climbing. */
class ElevationGain(private val minimumChange: Double = 5.0) {
    private var extreme: Double? = null
    private var rising = false

    fun reset() {
        extreme = null
        rising = false
    }

    fun append(altitude: Double?, accuracy: Float? = null): Double? {
        if (altitude == null || !altitude.isFinite() || (accuracy != null && accuracy > 25f)) {
            reset()
            return null
        }
        val previous = extreme
        if (previous == null) {
            extreme = altitude
            return null
        }
        val threshold = max(minimumChange, (accuracy ?: 0f) / 2.0)
        val delta = altitude - previous
        if (delta >= threshold || rising && delta > 0) {
            extreme = altitude
            rising = true
            return delta
        }
        if (delta <= -threshold || !rising && delta < 0) {
            extreme = altitude
            rising = false
        }
        return 0.0
    }
}

fun distanceMeters(first: GeoPoint, second: GeoPoint): Double {
    val lat1 = Math.toRadians(first.latitude)
    val lat2 = Math.toRadians(second.latitude)
    val deltaLat = (lat2 - lat1) / 2
    val deltaLon = Math.toRadians(second.longitude - first.longitude) / 2
    val haversine = sin(deltaLat).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon).pow(2)
    return 12_742_000 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
}
