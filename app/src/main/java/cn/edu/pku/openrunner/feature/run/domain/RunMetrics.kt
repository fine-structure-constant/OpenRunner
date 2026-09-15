package cn.edu.pku.openrunner.feature.run.domain

import kotlin.math.roundToInt

object RunMetrics {
    /** Average pace in seconds per kilometre. */
    fun paceSecondsPerKm(durationSeconds: Int, distanceMeters: Int): Int? {
        if (durationSeconds <= 0 || distanceMeters < MIN_PACE_DISTANCE_METERS) return null
        return (durationSeconds * 1_000.0 / distanceMeters).roundToInt()
    }

    fun formatDuration(totalSeconds: Int): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val hours = safeSeconds / 3_600
        val minutes = safeSeconds % 3_600 / 60
        val seconds = safeSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun formatPace(secondsPerKm: Int?): String {
        if (secondsPerKm == null) return "--"
        return "%d:%02d".format(secondsPerKm / 60, secondsPerKm % 60)
    }

    private const val MIN_PACE_DISTANCE_METERS = 10
}
