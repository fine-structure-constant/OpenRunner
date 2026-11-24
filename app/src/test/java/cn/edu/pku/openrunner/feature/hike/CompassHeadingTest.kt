package cn.edu.pku.openrunner.feature.hike

import cn.edu.pku.openrunner.feature.hike.domain.trueNorthHeading
import org.junit.Assert.assertEquals
import org.junit.Test

class CompassHeadingTest {
    @Test
    fun addsEastDeclinationAndSubtractsWestDeclination() {
        assertEquals(102f, trueNorthHeading(90f, 12f), 0.001f)
        assertEquals(78f, trueNorthHeading(90f, -12f), 0.001f)
    }

    @Test
    fun wrapsBothDirectionsAtNorth() {
        assertEquals(7f, trueNorthHeading(355f, 12f), 0.001f)
        assertEquals(353f, trueNorthHeading(5f, -12f), 0.001f)
        assertEquals(0f, trueNorthHeading(355f, 5f), 0.001f)
    }
}
