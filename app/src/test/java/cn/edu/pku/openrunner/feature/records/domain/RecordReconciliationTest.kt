package cn.edu.pku.openrunner.feature.records.domain

import cn.edu.pku.openrunner.core.network.RunRecordDto
import java.util.Date
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordReconciliationTest {
    private val remote = RunRecordDto(
        id = "6aa5b123456789abcdef01234",
        date = Date(100_000L),
        distance = 2_000,
        duration = 600.0
    )

    @Test
    fun responseParsingFailures_areDistinguishedFromRealUploadErrors() {
        assertTrue(RecordReconciliation.isResponseParsingFailure(
            "java.lang.NumberFormatException: For input string: \"6aa5b123\""
        ))
        assertFalse(RecordReconciliation.isResponseParsingFailure("连接服务器失败"))
        assertFalse(RecordReconciliation.isResponseParsingFailure(null))
    }

    @Test
    fun sameSubmittedMetrics_allowRepairWithSubsecondDateRounding() {
        assertTrue(RecordReconciliation.matchesSubmittedRun(100_500L, 2_000, 600, remote))
    }

    @Test
    fun differentRuns_orMissingServerIdentity_areNotReconciled() {
        assertFalse(RecordReconciliation.matchesSubmittedRun(103_000L, 2_000, 600, remote))
        assertFalse(RecordReconciliation.matchesSubmittedRun(100_000L, 2_020, 600, remote))
        assertFalse(RecordReconciliation.matchesSubmittedRun(100_000L, 2_000, 610, remote))
        assertFalse(RecordReconciliation.matchesSubmittedRun(
            100_000L, 2_000, 600, remote.copy(id = null)
        ))
    }
}
