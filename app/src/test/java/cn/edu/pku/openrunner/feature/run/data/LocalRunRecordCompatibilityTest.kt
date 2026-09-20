package cn.edu.pku.openrunner.feature.run.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class LocalRunRecordCompatibilityTest {
    @Test
    fun existingLocalTrack_withoutChartSamplesCanOpenDetailsAndSurvivesPersistence() {
        val gson = Gson()
        val original = gson.fromJson(
            """{"localId":"local-route","track":[
                {"longitude":116.31,"latitude":39.99,"status":1},
                {"longitude":116.32,"latitude":39.99,"status":2}
            ]}""".trimIndent(),
            LocalRunRecord::class.java
        )
        val restored = gson.fromJson(gson.toJson(original), LocalRunRecord::class.java)

        assertNull(restored.metricSamples)
        assertTrue(restored.hasLocalDetails)
        assertEquals(original.track, restored.track)
        assertEquals(116.31, restored.track.first().longitude, 0.000001)
        assertEquals(39.99, restored.track.first().latitude, 0.000001)
        assertEquals(2, restored.track.last().status)
    }

    @Test
    fun missingOrInvalidTrack_withoutSamplesDoesNotExposeDetails() {
        val gson = Gson()
        val missing = gson.fromJson("""{"localId":"missing"}""", LocalRunRecord::class.java)
        val invalid = gson.fromJson(
            """{"track":[{"longitude":181,"latitude":39}]}""",
            LocalRunRecord::class.java
        )

        assertFalse(missing.hasLocalDetails)
        assertFalse(invalid.hasLocalDetails)
        assertTrue(missing.asDto().track.orEmpty().isEmpty())
    }

    @Test
    fun chartOnlyRecord_stillHasDetailsEvenWithoutTrack() {
        val record = Gson().fromJson(
            """{"metricSamples":[
                {"elapsedMillis":0,"distanceMeters":0},
                {"elapsedMillis":1000,"distanceMeters":2}
            ]}""".trimIndent(),
            LocalRunRecord::class.java
        )

        assertTrue(record.hasLocalDetails)
    }

    @Test
    fun virtualProvenance_survivesJsonRoundTripAndCannotBeUploaded() {
        val gson = Gson()
        val virtual = gson.fromJson(
            """{"localId":"test-1","track":[],"usedVirtualLocation":true}""",
            LocalRunRecord::class.java
        )
        val restored = gson.fromJson(gson.toJson(virtual), LocalRunRecord::class.java)

        assertTrue(restored.usedVirtualLocation)
        assertFalse(restored.canUpload)
        assertTrue(restored.copy(lastUploadError = "网络失败").usedVirtualLocation)
    }

    @Test
    fun legacyNumericServerId_isReadAsStringWithoutLosingChartSamples() {
        val record = Gson().fromJson(
            """{
                "localId":"local-1",
                "serverRecordId":42,
                "metricSamples":[{"elapsedMillis":1000,"distanceMeters":2.5}]
            }""".trimIndent(),
            LocalRunRecord::class.java
        )

        assertEquals("42", record.serverRecordId)
        assertEquals(2.5, record.metricSamples!!.single().distanceMeters, 0.0001)
    }

    @Test
    fun oldRecordWithoutSamples_stillLoads() {
        val record = Gson().fromJson(
            """{"localId":"local-1","serverRecordId":42}""",
            LocalRunRecord::class.java
        )

        assertEquals("42", record.serverRecordId)
        assertNull(record.metricSamples)
        assertFalse(record.usedVirtualLocation)
        assertTrue(record.canUpload)
    }
}
