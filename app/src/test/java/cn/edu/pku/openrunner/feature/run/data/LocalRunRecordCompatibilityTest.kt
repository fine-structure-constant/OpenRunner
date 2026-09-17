package cn.edu.pku.openrunner.feature.run.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalRunRecordCompatibilityTest {
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
    }
}
