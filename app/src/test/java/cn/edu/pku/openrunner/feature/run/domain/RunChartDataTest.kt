package cn.edu.pku.openrunner.feature.run.domain

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
}
