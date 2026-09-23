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

    /**
     * Both chart series plus the headline pace, derived from one normalisation pass.
     *
     * The detail screen needs all three. Deriving them separately re-normalised the
     * samples three times and re-derived the pace series twice.
     */
    data class Series(
        val distanceKilometres: List<RunChartPoint>,
        val paceMinutesPerKm: List<RunChartPoint>,
        val fastestPaceMinutesPerKm: Double?
    )

    fun series(samples: List<RunMetricSample>): Series {
        val normalized = normalize(samples)
        val pace = paceFrom(normalized)
        return Series(
            distanceKilometres = normalized.map {
                RunChartPoint(it.elapsedMillis, it.distanceMeters / 1_000.0)
            },
            paceMinutesPerKm = pace,
            fastestPaceMinutesPerKm = pace.minOfOrNull(RunChartPoint::value)
        )
    }

    fun distanceKilometres(samples: List<RunMetricSample>): List<RunChartPoint> =
        normalize(samples).map { sample ->
            RunChartPoint(sample.elapsedMillis, sample.distanceMeters / 1_000.0)
        }

    fun paceMinutesPerKm(samples: List<RunMetricSample>): List<RunChartPoint> =
        paceFrom(normalize(samples))

    fun fastestPaceMinutesPerKm(samples: List<RunMetricSample>): Double? =
        paceFrom(normalize(samples)).minOfOrNull(RunChartPoint::value)

    /**
     * Derives local pace from the slope of the time-distance samples.
     *
     * A small backward window reduces GPS point-to-point noise. The window ends at the
     * most recent sample that is either at least [PACE_WINDOW_MILLIS] or at least
     * [PACE_WINDOW_METERS] behind the current one. The displayed values are then
     * saturated at 1.5 and 10 min/km, which keeps both extreme GPS jumps and stationary
     * periods readable on one chart.
     *
     * Runs in a single forward pass. The window start used to be found by stepping
     * backwards from every sample until one of the two thresholds was crossed, which is
     * a nested loop: the work per sample grew with the sampling rate, and a record whose
     * samples were denser than the 1 Hz the tracker emits would have degraded towards
     * quadratic. Both thresholds are monotonic in the sample index, so the window start
     * for sample `i` is never earlier than the one for `i - 1`; two cursors that only
     * ever move forward therefore find the same window without rescanning.
     */
    private fun paceFrom(normalized: List<RunMetricSample>): List<RunChartPoint> {
        if (normalized.size < 2) return emptyList()
        val points = ArrayList<RunChartPoint>(normalized.size - 1)
        // Cursor of the newest sample that is >= PACE_WINDOW_MILLIS behind the current
        // one, and likewise for PACE_WINDOW_METERS. -1 means "no sample qualifies yet".
        var timeCursor = -1
        var distanceCursor = -1
        for (index in 1 until normalized.size) {
            val current = normalized[index]
            val lastCandidate = index - 1
            while (timeCursor < lastCandidate &&
                current.elapsedMillis - normalized[timeCursor + 1].elapsedMillis >=
                PACE_WINDOW_MILLIS
            ) {
                timeCursor++
            }
            while (distanceCursor < lastCandidate &&
                current.distanceMeters - normalized[distanceCursor + 1].distanceMeters >=
                PACE_WINDOW_METERS
            ) {
                distanceCursor++
            }
            // Either threshold ends the window, so the start is whichever cursor went
            // furthest. Falling back to index 0 reproduces the old loop's floor at 0.
            val previous = normalized[maxOf(timeCursor, distanceCursor, 0)]
            val elapsedDelta = current.elapsedMillis - previous.elapsedMillis
            val distanceDelta = current.distanceMeters - previous.distanceMeters
            val rawPace = if (elapsedDelta <= 0L || distanceDelta < MIN_DISTANCE_DELTA_METERS) {
                MAX_PACE_MINUTES_PER_KM
            } else {
                elapsedDelta.toDouble() / distanceDelta / 60.0
            }
            points += RunChartPoint(
                elapsedMillis = current.elapsedMillis,
                value = rawPace.coerceIn(
                    MIN_PACE_MINUTES_PER_KM,
                    MAX_PACE_MINUTES_PER_KM
                )
            )
        }
        return points
    }

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
