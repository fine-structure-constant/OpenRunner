package cn.edu.pku.openrunner.feature.records.domain

import cn.edu.pku.openrunner.core.network.RunRecordDto
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordOrderingTest {
    @Test
    fun accountHistory_sortsBeforeTakingLatestTwelveAndPlacesUndatedLast() {
        val records = (1..14).map { RunRecordDto(id = "$it", date = Date(it * 1_000L)) } +
            RunRecordDto(id = "undated")
        val ordered = RecordOrdering.newestRecordsFirst(records)

        assertEquals((14 downTo 3).map { "$it" }, ordered.take(12).map { it.id })
        assertEquals("undated", ordered.last().id)
    }

    @Test
    fun `newest records are shown first and missing dates are last`() {
        val old = item(id = 1, time = 1_000L)
        val newest = item(id = 2, time = 3_000L)
        val middle = item(id = 3, time = 2_000L)
        val undated = item(id = 4, time = null)

        val ordered = RecordOrdering.newestFirst(listOf(old, undated, newest, middle))

        assertEquals(listOf("2", "3", "1", "4"), ordered.map { it.record.recordId })
    }

    private fun item(id: Int, time: Long?): RecordListItem = RecordListItem(
        record = RunRecordDto(recordId = id.toString(), date = time?.let(::Date)),
        uploadState = RecordUploadState.UPLOADED_VALID
    )
}
