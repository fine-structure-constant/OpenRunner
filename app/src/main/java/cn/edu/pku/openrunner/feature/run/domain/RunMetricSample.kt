package cn.edu.pku.openrunner.feature.run.domain

/**
 * A local-only checkpoint captured while a run is in progress.
 *
 * This model is deliberately excluded from the official upload payload. It is persisted only so
 * the client can rebuild distance and pace charts after the run has finished.
 */
data class RunMetricSample(
    val elapsedMillis: Long,
    val distanceMeters: Double
)

data class RunChartPoint(
    val elapsedMillis: Long,
    val value: Double
)

object RunChartData {
    const val MIN_PACE_MINUTES_PER_KM = 1.5
    const val MAX_PACE_MINUTES_PER_KM = 10.0

    private const val PACE_WINDOW_MILLIS = 10_000L
    private const val PACE_WINDOW_METERS = 25.0
    private const val MIN_DISTANCE_DELTA_METERS = 0.5

    fun distanceKilometres(samples: List<RunMetricSample>): List<RunChartPoint> =
        normalize(samples).map { sample ->
            RunChartPoint(sample.elapsedMillis, sample.distanceMeters / 1_000.0)
        }

    /**
     * Derives local pace from the slope of the time-distance samples.
     *
     * A small backward window reduces GPS point-to-point noise. The displayed values are then
     * saturated at 1.5 and 10 min/km, which keeps both extreme GPS jumps and stationary periods
     * readable on one chart.
     */
    fun paceMinutesPerKm(samples: List<RunMetricSample>): List<RunChartPoint> {
        val normalized = normalize(samples)
        if (normalized.size < 2) return emptyList()
        return normalized.indices.drop(1).map { index ->
            val current = normalized[index]
            var previousIndex = index - 1
            while (previousIndex > 0 &&
                current.elapsedMillis - normalized[previousIndex].elapsedMillis <
                    PACE_WINDOW_MILLIS &&
                current.distanceMeters - normalized[previousIndex].distanceMeters <
                    PACE_WINDOW_METERS
            ) {
                previousIndex--
            }
            val previous = normalized[previousIndex]
            val elapsedDelta = current.elapsedMillis - previous.elapsedMillis
            val distanceDelta = current.distanceMeters - previous.distanceMeters
            val rawPace = if (elapsedDelta <= 0L || distanceDelta < MIN_DISTANCE_DELTA_METERS) {
                MAX_PACE_MINUTES_PER_KM
            } else {
                elapsedDelta.toDouble() / distanceDelta / 60.0
            }
            RunChartPoint(
                elapsedMillis = current.elapsedMillis,
                value = rawPace.coerceIn(
                    MIN_PACE_MINUTES_PER_KM,
                    MAX_PACE_MINUTES_PER_KM
                )
            )
        }
    }

    fun fastestPaceMinutesPerKm(samples: List<RunMetricSample>): Double? =
        paceMinutesPerKm(samples).minOfOrNull(RunChartPoint::value)

    private fun normalize(samples: List<RunMetricSample>): List<RunMetricSample> {
        var maximumDistance = 0.0
        var lastElapsed = -1L
        return samples
            .asSequence()
            .filter { it.elapsedMillis >= 0L && it.distanceMeters.isFinite() }
            .sortedBy(RunMetricSample::elapsedMillis)
            .mapNotNull { sample ->
                if (sample.elapsedMillis <= lastElapsed) return@mapNotNull null
                lastElapsed = sample.elapsedMillis
                maximumDistance = maxOf(maximumDistance, sample.distanceMeters.coerceAtLeast(0.0))
                sample.copy(distanceMeters = maximumDistance)
            }
            .toList()
    }
}
