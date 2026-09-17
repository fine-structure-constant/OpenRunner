package cn.edu.pku.openrunner.feature.records.data

import cn.edu.pku.openrunner.core.network.ApiClient
import cn.edu.pku.openrunner.core.network.PkuNewYouthApi
import cn.edu.pku.openrunner.core.network.RunRecordDto
import cn.edu.pku.openrunner.core.network.UserStatusDto
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.records.domain.RecordListItem
import cn.edu.pku.openrunner.feature.records.domain.RecordOrdering
import cn.edu.pku.openrunner.feature.records.domain.RecordReconciliation
import cn.edu.pku.openrunner.feature.records.domain.RecordUploadState
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecord
import cn.edu.pku.openrunner.feature.run.data.RunRecordRepository
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs
import kotlin.math.max

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
        val claimedServerIds = mutableSetOf<String>()
        val reconciledLocal = local.map { record ->
            if (record.uploaded ||
                !RecordReconciliation.isResponseParsingFailure(record.lastUploadError)
            ) {
                record
            } else {
                val matchingRemote = remote.firstOrNull { serverRecord ->
                    serverRecord.serverId !in claimedServerIds &&
                        RecordReconciliation.matchesSubmittedRun(
                            record.completedAtMillis,
                            record.distanceMeters,
                            record.durationSeconds,
                            serverRecord
                        )
                }
                if (matchingRemote == null) {
                    record
                } else {
                    matchingRemote.serverId?.let { claimedServerIds += it }
                    runRecordRepository.confirmUploaded(record.localId, matchingRemote) ?: record
                }
            }
        }
        return merge(reconciledLocal, remote)
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
        val localByServerId = local
            .filter { it.uploaded && it.serverRecordId != null }
            .associateBy { checkNotNull(it.serverRecordId) }
        val matchedLocalIds = mutableSetOf<String>()
        val remoteItems = remote.map { record ->
            val localDetails = serverId(record)?.let(localByServerId::get)
                ?: findLocalMatch(record, local, matchedLocalIds)
            localDetails?.let { matchedLocalIds += it.localId }
            RecordListItem(
                record = record,
                detailLocalId = localDetails
                    ?.takeIf(::hasLocalDetails)
                    ?.localId,
                hasLocalDetails = localDetails?.let(::hasLocalDetails) == true,
                uploadState = if (record.verified) {
                    RecordUploadState.UPLOADED_VALID
                } else {
                    RecordUploadState.UPLOADED_INVALID
                },
                hasPhoto = !record.photoPath.isNullOrBlank()
            )
        }
        val localItems = local
            .filter { record ->
                record.localId !in matchedLocalIds &&
                    (!record.uploaded ||
                        record.serverRecordId == null ||
                        record.serverRecordId !in remoteIds)
            }
            .map(::localItem)
        return RecordOrdering.newestFirst(localItems + remoteItems)
    }

    private fun localItem(record: LocalRunRecord): RecordListItem = RecordListItem(
        record = record.asDto(),
        localId = record.localId,
        detailLocalId = record.localId.takeIf { hasLocalDetails(record) },
        hasLocalDetails = hasLocalDetails(record),
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

    private fun serverId(record: RunRecordDto): String? = record.serverId

    private fun hasLocalDetails(record: LocalRunRecord): Boolean =
        record.metricSamples.orEmpty().size >= 2

    private fun findLocalMatch(
        remote: RunRecordDto,
        local: List<LocalRunRecord>,
        alreadyMatched: Set<String>
    ): LocalRunRecord? {
        val remoteTime = remote.date?.time ?: return null
        return local
            .asSequence()
            .filter {
                it.uploaded &&
                    it.serverRecordId == null &&
                    it.localId !in alreadyMatched &&
                    abs(it.completedAtMillis - remoteTime) <= MATCH_TIME_TOLERANCE_MILLIS &&
                    abs(it.distanceMeters - remote.distance) <=
                    max(MATCH_DISTANCE_TOLERANCE_METERS, remote.distance * 0.05) &&
                    abs(it.durationSeconds - remote.duration) <= MATCH_DURATION_TOLERANCE_SECONDS
            }
            .minByOrNull { record ->
                abs(record.completedAtMillis - remoteTime) +
                    abs(record.distanceMeters - remote.distance).toLong() * 1_000L
            }
    }

    companion object {
        private const val MATCH_TIME_TOLERANCE_MILLIS = 120_000L
        private const val MATCH_DISTANCE_TOLERANCE_METERS = 30.0
        private const val MATCH_DURATION_TOLERANCE_SECONDS = 15.0
    }
}
