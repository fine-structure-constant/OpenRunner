package cn.edu.pku.openrunner.feature.run.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class TrackPoint(
    val longitude: Double,
    val latitude: Double,
    val status: Int = 0
) {
    val isValidCoordinate: Boolean
        get() = longitude.isFinite() && latitude.isFinite() &&
            longitude in -180.0..180.0 && latitude in -90.0..90.0
}

object TrackDistance {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun polylineMeters(points: List<TrackPoint>): Double {
        return points.zipWithNext().sumOf { (previous, current) ->
            haversineMeters(previous, current)
        }
    }

    fun haversineMeters(a: TrackPoint, b: TrackPoint): Double {
        val latitudeA = Math.toRadians(a.latitude)
        val latitudeB = Math.toRadians(b.latitude)
        val deltaLatitude = latitudeB - latitudeA
        val deltaLongitude = Math.toRadians(b.longitude - a.longitude)
        val h = sin(deltaLatitude / 2).pow(2) +
            cos(latitudeA) * cos(latitudeB) * sin(deltaLongitude / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
    }
}
