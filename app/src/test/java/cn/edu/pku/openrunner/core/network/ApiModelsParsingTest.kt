package cn.edu.pku.openrunner.core.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ApiModelsParsingTest {
    private val gson = Gson()

    @Test
    fun recordPayload_matchesLegacyDateDurationAndTrackShape() {
        val type = object : TypeToken<ApiResponse<List<RunRecordDto>>>() {}.type
        val response: ApiResponse<List<RunRecordDto>> = gson.fromJson(
            """{
                "success": true,
                "code": 0,
                "data": [{
                    "recordId": 42,
                    "distance": 1234,
                    "duration": 345.6,
                    "date": "2024-03-04T12:34:56.000Z",
                    "step": 987,
                    "detail": [[116.3131, 39.9876, 0]]
                }]
            }""".trimIndent(),
            type
        )

        val record = response.requireData().single()
        assertEquals("42", record.recordId)
        assertEquals(345, record.duration.toInt())
        assertNotNull(record.date)
        assertEquals(116.3131, record.track!![0][0], 0.000001)
    }

    @Test
    fun successfulUpload_acceptsOpaqueDatabaseIdAndLegacyNumericRecordId() {
        val type = object : TypeToken<ApiResponse<RunRecordDto>>() {}.type
        val response: ApiResponse<RunRecordDto> = gson.fromJson(
            """{
                "success": true,
                "code": 0,
                "data": {
                    "id": "6aa5b123456789abcdef01234",
                    "recordId": 42,
                    "distance": 2000,
                    "duration": 600,
                    "verified": true
                }
            }""".trimIndent(),
            type
        )

        val record = response.requireData()
        assertEquals("6aa5b123456789abcdef01234", record.id)
        assertEquals("42", record.serverId)
        assertEquals(true, record.verified)
    }

    @Test
    fun recordPayload_acceptsOpaqueRecordIdAndMongoIdAlias() {
        val record = gson.fromJson(
            """{"_id":"6aa5b123456789abcdef01234","recordId":"6aa5b234567890abcdef01234"}""",
            RunRecordDto::class.java
        )

        assertEquals("6aa5b123456789abcdef01234", record.id)
        assertEquals("6aa5b234567890abcdef01234", record.serverId)
    }

    @Test
    fun absentOrLegacySentinelRecordId_fallsBackToDatabaseId() {
        val record = gson.fromJson(
            """{"id":"6aa5b123456789abcdef01234","recordId":-1}""",
            RunRecordDto::class.java
        )

        assertEquals("6aa5b123456789abcdef01234", record.serverId)
    }

    @Test
    fun userStatusPayload_matchesLegacyServerModel() {
        val type = object : TypeToken<ApiResponse<UserStatusDto>>() {}.type
        val response: ApiResponse<UserStatusDto> = gson.fromJson(
            """{
                "success": true,
                "code": 0,
                "data": {
                    "beginDate": "2024-02-26T00:00:00.000Z",
                    "endDate": "2024-06-30T00:00:00.000Z",
                    "current": 12340,
                    "bonus": 1000,
                    "target": 30000,
                    "validCount": 8,
                    "isPassed": false
                }
            }""".trimIndent(),
            type
        )

        val status = response.requireData()
        assertEquals(12340, status.current)
        assertNotNull(status.beginDate)
        assertEquals(8, status.validCount)
    }
}
