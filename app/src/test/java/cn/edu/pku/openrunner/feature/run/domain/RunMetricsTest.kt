package cn.edu.pku.openrunner.feature.run.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunMetricsTest {
    @Test
    fun `five kilometres in thirty minutes is six minute pace`() {
        assertEquals(360, RunMetrics.paceSecondsPerKm(1_800, 5_000))
        assertEquals("6:00", RunMetrics.formatPace(360))
    }

    @Test
    fun `pace waits for enough distance to avoid noisy startup values`() {
        assertNull(RunMetrics.paceSecondsPerKm(5, 9))
        assertEquals("--", RunMetrics.formatPace(null))
    }

    @Test
    fun `duration supports runs longer than one hour`() {
        assertEquals("01:05", RunMetrics.formatDuration(65))
        assertEquals("1:01:01", RunMetrics.formatDuration(3_661))
    }
}
