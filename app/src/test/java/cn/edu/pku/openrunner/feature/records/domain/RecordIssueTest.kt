package cn.edu.pku.openrunner.feature.records.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordIssueTest {
    @Test
    fun `legacy validation codes keep their official meanings`() {
        assertEquals(RecordIssue.DISTANCE, RecordIssue.fromServerCode(7))
        assertEquals(RecordIssue.SPEED, RecordIssue.fromServerCode(8))
        assertEquals(RecordIssue.LOCATION, RecordIssue.fromServerCode(9))
        assertEquals(RecordIssue.TIME, RecordIssue.fromServerCode(23))
        assertEquals(RecordIssue.DIGEST, RecordIssue.fromServerCode(24))
    }

    @Test
    fun `unknown server code remains explicit`() {
        assertEquals(RecordIssue.UNKNOWN, RecordIssue.fromServerCode(999))
        assertEquals(RecordIssue.UNKNOWN, RecordIssue.fromServerCode(null))
    }
}
