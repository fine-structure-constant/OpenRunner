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
        assertEquals(345, record.duration.toInt())
        assertNotNull(record.date)
        assertEquals(116.3131, record.track!![0][0], 0.000001)
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
