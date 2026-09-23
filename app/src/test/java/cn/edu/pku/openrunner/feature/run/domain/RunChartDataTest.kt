package cn.edu.pku.openrunner.feature.run.domain

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunChartDataTest {
    @Test
    fun `pace is the differential of elapsed time and distance`() {
        val pace = RunChartData.paceMinutesPerKm(
            listOf(
                RunMetricSample(0L, 0.0),
                RunMetricSample(300_000L, 1_000.0)
            )
        )

        assertEquals(1, pace.size)
        assertEquals(5.0, pace.single().value, 0.0001)
    }

    @Test
    fun `pace values are clipped to chart boundaries`() {
        val fast = RunChartData.paceMinutesPerKm(
            listOf(RunMetricSample(0L, 0.0), RunMetricSample(60_000L, 1_000.0))
        )
        val slow = RunChartData.paceMinutesPerKm(
            listOf(RunMetricSample(0L, 0.0), RunMetricSample(120_000L, 100.0))
        )
        val stationary = RunChartData.paceMinutesPerKm(
            listOf(RunMetricSample(0L, 0.0), RunMetricSample(60_000L, 0.0))
        )

        assertEquals(RunChartData.MIN_PACE_MINUTES_PER_KM, fast.single().value, 0.0001)
        assertEquals(RunChartData.MAX_PACE_MINUTES_PER_KM, slow.single().value, 0.0001)
        assertEquals(RunChartData.MAX_PACE_MINUTES_PER_KM, stationary.single().value, 0.0001)
    }

    @Test
    fun `distance chart remains monotonic after a noisy checkpoint`() {
        val chart = RunChartData.distanceKilometres(
            listOf(
                RunMetricSample(0L, 0.0),
                RunMetricSample(10_000L, 100.0),
                RunMetricSample(20_000L, 95.0),
                RunMetricSample(30_000L, 180.0)
            )
        )

        assertEquals(listOf(0.0, 0.1, 0.1, 0.18), chart.map { it.value })
        assertTrue(chart.zipWithNext().all { (a, b) -> b.value >= a.value })
    }

    @Test
    fun `pace matches the backward window scan it replaced`() {
        val random = Random(SEED)
        repeat(120) { case ->
            assertSameAsReference(
                case,
                randomSamples(
                    count = 2 + random.nextInt(400),
                    stepMillis = { random.nextInt(1, 3_001) },
                    stepMeters = { random.nextInt(0, 9).toDouble() }
                )
            )
        }
    }

    @Test
    fun `pace matches the backward window scan when samples are much denser than the tracker emits`() {
        // The replaced search stepped backwards once per sample inside the window, so its
        // cost grew with the sampling rate. These datasets put thousands of samples inside
        // a single ten second window, which is the regime that used to be quadratic.
        val random = Random(SEED + 1)
        repeat(20) { case ->
            assertSameAsReference(
                case,
                randomSamples(
                    count = 1_000 + random.nextInt(2_000),
                    stepMillis = { random.nextInt(1, 6) },
                    stepMeters = { random.nextInt(0, 4) / 1_000.0 }
                )
            )
        }
    }

    @Test
    fun `pace matches the backward window scan when the runner stands still`() {
        // A flat distance keeps the distance threshold out of reach for the whole run, so
        // the window is decided by elapsed time alone.
        val random = Random(SEED + 2)
        repeat(30) { case ->
            val samples = ArrayList<RunMetricSample>(500)
            var elapsed = 0L
            samples += RunMetricSample(0L, 0.0)
            repeat(499) {
                elapsed += random.nextInt(1, 2_001)
                samples += RunMetricSample(elapsed, 0.0)
            }
            assertSameAsReference(case, samples)
        }
    }

    @Test
    fun `series agrees with the single purpose derivations`() {
        val random = Random(SEED + 3)
        repeat(40) { case ->
            val samples = randomSamples(
                count = 2 + random.nextInt(300),
                stepMillis = { random.nextInt(1, 5_001) },
                stepMeters = { random.nextInt(0, 12).toDouble() }
            )
            val series = RunChartData.series(samples)

            assertEquals(
                "case $case distance",
                RunChartData.distanceKilometres(samples),
                series.distanceKilometres
            )
            assertEquals(
                "case $case pace",
                RunChartData.paceMinutesPerKm(samples),
                series.paceMinutesPerKm
            )
            assertEquals(
                "case $case fastest",
                RunChartData.fastestPaceMinutesPerKm(samples),
                series.fastestPaceMinutesPerKm
            )
        }
    }

    /**
     * Guards the complexity of the window search, not just its output.
     *
     * 200k samples one millisecond apart put roughly 8k samples inside every ten second
     * window. The two cursor pass is linear and finishes in well under a second; the
     * backward scan this replaced would have taken billions of steps here. The timeout is
     * generous enough not to flake on a slow machine, but tight enough that reintroducing
     * the nested loop fails instead of quietly slowing the detail screen down again.
     */
    @Test(timeout = 20_000)
    fun `pace derivation stays linear on a long densely sampled record`() {
        val count = 200_000
        val samples = ArrayList<RunMetricSample>(count)
        repeat(count) { index ->
            samples += RunMetricSample(index.toLong(), index * 0.003)
        }

        val pace = RunChartData.paceMinutesPerKm(samples)

        assertEquals(count - 1, pace.size)
        // The very first point only spans one millisecond and three millimetres, which is
        // under MIN_DISTANCE_DELTA_METERS, so it saturates rather than reporting a pace.
        assertEquals(RunChartData.MAX_PACE_MINUTES_PER_KM, pace.first().value, 0.0)
        // Once the window has filled, 3 m/s settles at a 5'33" km.
        assertTrue(
            "last pace was ${pace.last().value}, expected roughly 5.56",
            pace.last().value in 5.5..5.6
        )
    }

    private fun assertSameAsReference(case: Int, samples: List<RunMetricSample>) {
        val expected = referencePace(samples)
        val actual = RunChartData.paceMinutesPerKm(samples)

        assertEquals("case $case size", expected.size, actual.size)
        expected.forEachIndexed { index, point ->
            val other = actual[index]
            assertEquals(
                "case $case point $index elapsed",
                point.elapsedMillis,
                other.elapsedMillis
            )
            // Both implementations run the same arithmetic on the same operands, so the
            // results must agree exactly rather than merely closely.
            assertEquals("case $case point $index value", point.value, other.value, 0.0)
        }
    }

    /**
     * The window search exactly as it was written before the two cursor rewrite, kept as
     * the oracle for the equivalence tests.
     *
     * The window constants are spelled out here on purpose. This is a snapshot of the
     * behaviour the rewrite had to preserve, so changing a window in [RunChartData]
     * should force a deliberate update of this oracle rather than silently redefining
     * what "unchanged" means.
     */
    private fun referencePace(samples: List<RunMetricSample>): List<RunChartPoint> {
        if (samples.size < 2) return emptyList()
        return samples.indices.drop(1).map { index ->
            val current = samples[index]
            var previousIndex = index - 1
            while (previousIndex > 0 &&
                current.elapsedMillis - samples[previousIndex].elapsedMillis < 10_000L &&
                current.distanceMeters - samples[previousIndex].distanceMeters < 25.0
            ) {
                previousIndex--
            }
            val previous = samples[previousIndex]
            val elapsedDelta = current.elapsedMillis - previous.elapsedMillis
            val distanceDelta = current.distanceMeters - previous.distanceMeters
            val rawPace = if (elapsedDelta <= 0L || distanceDelta < 0.5) {
                10.0
            } else {
                elapsedDelta.toDouble() / distanceDelta / 60.0
            }
            RunChartPoint(
                elapsedMillis = current.elapsedMillis,
                value = rawPace.coerceIn(1.5, 10.0)
            )
        }
    }

    /**
     * Builds samples that are already in the shape [RunChartData.normalize] would produce:
     * strictly increasing elapsed time, non-decreasing distance, all finite. That keeps the
     * oracle above focused on the window search alone.
     */
    private fun randomSamples(
        count: Int,
        stepMillis: () -> Int,
        stepMeters: () -> Double
    ): List<RunMetricSample> {
        val samples = ArrayList<RunMetricSample>(count)
        var elapsed = 0L
        var distance = 0.0
        samples += RunMetricSample(0L, 0.0)
        repeat(count - 1) {
            elapsed += stepMillis().coerceAtLeast(1)
            distance += stepMeters().coerceAtLeast(0.0)
            samples += RunMetricSample(elapsed, distance)
        }
        return samples
    }

    private companion object {
        const val SEED = 20260923L
    }
}
