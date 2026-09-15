package cn.edu.pku.openrunner.feature.records.data

import cn.edu.pku.openrunner.core.network.ApiClient
import cn.edu.pku.openrunner.core.network.PkuNewYouthApi
import cn.edu.pku.openrunner.core.network.RunRecordDto
import cn.edu.pku.openrunner.core.network.UserStatusDto
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.records.domain.RecordListItem
import cn.edu.pku.openrunner.feature.records.domain.RecordOrdering
import cn.edu.pku.openrunner.feature.records.domain.RecordUploadState
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecord
import cn.edu.pku.openrunner.feature.run.data.RunRecordRepository
import android.net.Uri
import kotlinx.coroutines.flow.Flow

class RecordRepository(
    private val sessionStore: SessionStore,
    private val runRecordRepository: RunRecordRepository,
    private val api: PkuNewYouthApi = ApiClient.api
) {
    private fun requireUserId(): String = sessionStore.userId ?: error("未登录")

    suspend fun list(): List<RecordListItem> {
        val local = runRecordRepository.localRecords()
        val userId = sessionStore.userId
        val remote = if (userId == null) {
            emptyList()
        } else {
            runCatching { api.getRecords(userId).requireData() }
                .getOrElse { error ->
                    if (local.isEmpty()) throw error else emptyList()
                }
        }
        return merge(local, remote)
    }

    suspend fun upload(localId: String): RunRecordDto =
        runRecordRepository.upload(localId)

    suspend fun attachPhoto(localId: String, source: Uri) =
        runRecordRepository.attachPhoto(localId, source)

    suspend fun deleteLocal(localId: String): Boolean =
        runRecordRepository.deleteLocal(localId)

    fun observeLocalChanges(): Flow<Unit> = runRecordRepository.observeLocalChanges()

    suspend fun status(): UserStatusDto =
        api.getUserStatus(requireUserId()).requireData()

    private fun merge(
        local: List<LocalRunRecord>,
        remote: List<RunRecordDto>
    ): List<RecordListItem> {
        val remoteIds = remote.mapNotNull(::serverId).toSet()
        val localItems = local
            .filter { record ->
                !record.uploaded || record.serverRecordId == null || record.serverRecordId !in remoteIds
            }
            .map(::localItem)
        val remoteItems = remote.map { record ->
            RecordListItem(
                record = record,
                uploadState = if (record.verified) {
                    RecordUploadState.UPLOADED_VALID
                } else {
                    RecordUploadState.UPLOADED_INVALID
                },
                hasPhoto = !record.photoPath.isNullOrBlank()
            )
        }
        return RecordOrdering.newestFirst(localItems + remoteItems)
    }

    private fun localItem(record: LocalRunRecord): RecordListItem = RecordListItem(
        record = record.asDto(),
        localId = record.localId,
        uploadState = when {
            record.uploaded && record.verified -> RecordUploadState.UPLOADED_VALID
            record.uploaded -> RecordUploadState.UPLOADED_INVALID
            !record.lastUploadError.isNullOrBlank() -> RecordUploadState.FAILED
            else -> RecordUploadState.PENDING
        },
        hasPhoto = !record.photoFilePath.isNullOrBlank() ||
            record.photoUploaded ||
            !record.photoRemotePath.isNullOrBlank(),
        failureCode = record.lastUploadErrorCode,
        failureMessage = record.lastUploadError
    )

    private fun serverId(record: RunRecordDto): Int? =
        record.recordId.takeIf { it >= 0 } ?: record.id.takeIf { it >= 0 }
}
