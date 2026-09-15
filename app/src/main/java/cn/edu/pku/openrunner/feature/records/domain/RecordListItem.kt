package cn.edu.pku.openrunner.feature.records.domain

import cn.edu.pku.openrunner.core.network.RunRecordDto

enum class RecordUploadState {
    PENDING,
    UPLOADING,
    UPLOADED_VALID,
    UPLOADED_INVALID,
    FAILED
}

enum class RecordIssue {
    DISTANCE,
    SPEED,
    LOCATION,
    TIME,
    PHOTO,
    DIGEST,
    SESSION,
    ALREADY_UPLOADED,
    UNKNOWN;

    companion object {
        fun fromServerCode(code: Int?): RecordIssue = when (code) {
            3 -> SESSION
            4 -> ALREADY_UPLOADED
            7 -> DISTANCE
            8 -> SPEED
            9 -> LOCATION
            18 -> PHOTO
            23 -> TIME
            24 -> DIGEST
            else -> UNKNOWN
        }
    }
}

data class RecordListItem(
    val record: RunRecordDto,
    val localId: String? = null,
    val detailLocalId: String? = null,
    val hasLocalDetails: Boolean = false,
    val uploadState: RecordUploadState,
    val hasPhoto: Boolean = false,
    val isPhotoProcessing: Boolean = false,
    val failureCode: Int? = null,
    val failureMessage: String? = null
) {
    val itemKey: String
        get() = localId?.let { "local:$it" }
            ?: "server:${record.recordId}:${record.id}:${record.date?.time ?: 0L}"
}

object RecordOrdering {
    fun newestFirst(items: List<RecordListItem>): List<RecordListItem> =
        items.sortedByDescending { it.record.date?.time ?: Long.MIN_VALUE }
}
