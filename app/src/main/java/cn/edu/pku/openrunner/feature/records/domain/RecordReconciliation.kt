package cn.edu.pku.openrunner.feature.records.domain

import cn.edu.pku.openrunner.core.network.RunRecordDto
import kotlin.math.abs

object RecordReconciliation {
    fun isResponseParsingFailure(message: String?): Boolean =
        message?.let {
            it.contains("NumberFormatException") || it.contains("For input string:")
        } == true

    /** Strict matching is used only to repair a response that failed after a successful POST. */
    fun matchesSubmittedRun(
        completedAtMillis: Long,
        distanceMeters: Int,
        durationSeconds: Int,
        remote: RunRecordDto
    ): Boolean {
        val remoteTime = remote.date?.time ?: return false
        return remote.serverId != null &&
            abs(completedAtMillis - remoteTime) <= 1_000L &&
            abs(distanceMeters - remote.distance) <= 1 &&
            abs(durationSeconds - remote.duration) <= 1.0
    }
}
