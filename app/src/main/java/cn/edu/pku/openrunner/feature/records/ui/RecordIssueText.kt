package cn.edu.pku.openrunner.feature.records.ui

import android.content.Context
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.records.domain.RecordIssue

fun Context.recordIssueText(code: Int?, fallback: String? = null): String {
    val resource = when (RecordIssue.fromServerCode(code)) {
        RecordIssue.DISTANCE -> R.string.record_issue_distance
        RecordIssue.SPEED -> R.string.record_issue_speed
        RecordIssue.LOCATION -> R.string.record_issue_location
        RecordIssue.TIME -> R.string.record_issue_time
        RecordIssue.PHOTO -> R.string.record_issue_photo
        RecordIssue.DIGEST -> R.string.record_issue_digest
        RecordIssue.SESSION -> R.string.record_issue_session
        RecordIssue.ALREADY_UPLOADED -> R.string.record_issue_already_uploaded
        RecordIssue.UNKNOWN -> null
    }
    return resource?.let(::getString)
        ?: fallback?.takeIf(String::isNotBlank)
        ?: getString(R.string.record_issue_unknown)
}
