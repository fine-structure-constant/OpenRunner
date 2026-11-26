package cn.edu.pku.openrunner.feature.hike

import cn.edu.pku.openrunner.core.orientation.magneticAzimuth
import cn.edu.pku.openrunner.core.orientation.mapHeading
import cn.edu.pku.openrunner.core.orientation.smoothHeading
import cn.edu.pku.openrunner.core.orientation.trueNorthHeading
import cn.edu.pku.openrunner.ui.amapMarkerAngle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompassHeadingTest {
    @Test
    fun flatAndUprightPhonesCanPointEastOnTheMap() {
        val flat = floatArrayOf(0f, 1f, 0f, -1f, 0f, 0f, 0f, 0f, 1f)
        val upright = floatArrayOf(0f, 0f, -1f, -1f, 0f, 0f, 0f, 1f, 0f)
        assertEquals(90f, magneticAzimuth(flat, allowUpright = true)!!, 0.001f)
        assertEquals(90f, magneticAzimuth(upright, allowUpright = true)!!, 0.001f)
        assertNull(magneticAzimuth(upright, allowUpright = false))
    }

    @Test
    fun smoothingCrossesNorthWithoutTurningTheLongWay() {
        assertEquals(359.44f, smoothHeading(359f, 1f), 0.001f)
        assertEquals(0.56f, smoothHeading(1f, 359f), 0.001f)
    }

    @Test
    fun mapConversionUsesTrueNorthAndTheSdkCounterclockwiseAngle() {
        assertEquals(0, mapHeading(356f, 4f))
        assertEquals(270f, amapMarkerAngle(mapHeading(87f, 3f)), 0.001f)
        assertNull(mapHeading(null, 3f))
        assertNull(mapHeading(87f, null))
    }

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
