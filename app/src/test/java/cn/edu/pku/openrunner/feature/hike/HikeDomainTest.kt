package cn.edu.pku.openrunner.feature.hike

import cn.edu.pku.openrunner.feature.hike.data.KmlReader
import cn.edu.pku.openrunner.feature.hike.domain.GeoPoint
import cn.edu.pku.openrunner.feature.hike.domain.HikeTrack
import cn.edu.pku.openrunner.feature.hike.domain.MapArea
import cn.edu.pku.openrunner.feature.hike.domain.distanceMeters
import cn.edu.pku.openrunner.feature.hike.domain.worldX
import cn.edu.pku.openrunner.feature.hike.domain.worldY
import org.junit.Assert.*
import org.junit.Test
import org.xml.sax.SAXException

class HikeDomainTest {
    @Test
    fun importsKmlLineStringTrackAndNamedWaypoint() {
        val route = KmlReader.read(
            """
            <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">
              <Document><name>测试路线</name>
                <Placemark><LineString><coordinates>
                  115.45,40.02,1800 115.46,40.03,1850 115.47,40.04
                </coordinates></LineString></Placemark>
                <Placemark><gx:MultiTrack><gx:Track>
                  <when>2026-11-21T00:00:00Z</when>
                  <gx:coord>115.47 40.04 1850</gx:coord>
                  <gx:coord>115.48 40.05 1910</gx:coord>
                </gx:Track></gx:MultiTrack></Placemark>
                <Placemark><name>山峰</name><Point><coordinates>115.48,40.05,1910</coordinates></Point></Placemark>
              </Document>
            </kml>
            """.trimIndent().byteInputStream(),
            "test.kml"
        )
        assertEquals("测试路线", route.name)
        assertEquals(listOf(3, 2), route.lines.map { it.size })
        assertEquals(GeoPoint(40.02, 115.45, 1800.0), route.lines.first().first())
        assertNull(route.lines.first().last().altitude)
        assertEquals("山峰", route.waypoints.single().name)
    }

    @Test
    fun usesFilenameWhenOnlyTheWaypointHasAName() {
        val route = KmlReader.read(
            """
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Placemark><LineString><coordinates>115.45,40.02 115.46,40.03</coordinates></LineString></Placemark>
              <Placemark><name>山峰</name><Point><coordinates>115.46,40.03</coordinates></Point></Placemark>
            </kml>
            """.trimIndent().byteInputStream(),
            "unnamed-route.kml"
        )
        assertEquals("unnamed-route.kml", route.name)
    }

    @Test(expected = SAXException::class)
    fun rejectsDocumentTypeBeforeEntityExpansion() {
        KmlReader.read(
            """<!DOCTYPE kml [<!ENTITY title "expanded">]><kml><Document><name>&title;</name></Document></kml>"""
                .byteInputStream(),
            "test.kml"
        )
    }

    @Test
    fun storesAscentWithoutCountingNoiseOrConnectingSignalGaps() {
        val track = HikeTrack()
        val timely = listOf(100.0, 102.0, 99.0, 101.0, 109.0, 113.0, 105.0, 110.0)
        timely.forEachIndexed { index, altitude ->
            track.append(GeoPoint(40.0, 115.0 + index * 0.0001, altitude), index * 3_000L, 4f)
        }
        assertEquals(listOf(null, 0.0, 0.0, 0.0, 10.0, 4.0, 0.0, 5.0),
            track.snapshot().map { it.segmentAscent })
        assertEquals(19.0, track.ascent, 0.0)
        val distance = track.distance
        val afterGap = track.append(GeoPoint(40.1, 115.1, 1_100.0), 100_000, 4f, breakBefore = true)
        assertEquals(1, afterGap.section)
        assertNull(afterGap.segmentAscent)
        assertEquals(distance, track.distance, 0.0)
        assertNull(track.append(GeoPoint(40.1, 115.1001, 1_200.0), 103_000, 40f).segmentAscent)
        assertEquals(19.0, track.ascent, 0.0)
    }

    @Test
    fun cachesTheWholeReferenceAreaAndHandlesLongitudeWrap() {
        val first = GeoPoint(40.0, 179.98)
        val last = GeoPoint(40.02, -179.97)
        val area = requireNotNull(MapArea.around(sequenceOf(first, last), 5))
        assertTrue(area.east - area.west < 0.2)
        assertTrue(area.contains(worldX(first.longitude), worldY(first.latitude)))
        assertTrue(area.contains(worldX(last.longitude), worldY(last.latitude)))
        assertTrue(area.normalizedBoxes().size == 2)
        assertTrue(area.tiles(13).all { it.wrappedX in 0 until it.scale })
        assertTrue(distanceMeters(first, last) < 5_000)
        assertNotNull(MapArea.around(sequenceOf(first), 0))
    }
}
