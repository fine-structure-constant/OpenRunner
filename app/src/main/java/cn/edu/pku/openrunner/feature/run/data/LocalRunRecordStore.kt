package cn.edu.pku.openrunner.feature.run.data

import android.content.Context
import cn.edu.pku.openrunner.core.network.RunRecordDto
import cn.edu.pku.openrunner.feature.run.domain.TrackPoint
import cn.edu.pku.openrunner.feature.run.domain.RunMetricSample
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Date
import java.util.UUID

data class LocalRunRecord(
    val localId: String,
    val userId: String?,
    val startedAtMillis: Long,
    val completedAtMillis: Long,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val steps: Int,
    val track: List<TrackPoint>,
    val checkField: String?,
    val uploaded: Boolean = false,
    // String also reads legacy numeric JSON IDs through Gson without losing existing local details.
    val serverRecordId: String? = null,
    val verified: Boolean = false,
    val invalidReason: Int = 0,
    val morningBonus: Boolean = false,
    val photoFilePath: String? = null,
    val photoUploaded: Boolean = false,
    val photoRemotePath: String? = null,
    val lastUploadErrorCode: Int? = null,
    val lastUploadError: String? = null,
    // Nullable for Gson compatibility with records saved before local chart data was introduced.
    val metricSamples: List<RunMetricSample>? = null,
    // Missing in legacy JSON means false; once saved this provenance must never be cleared.
    val usedVirtualLocation: Boolean = false
) {
    val canUpload: Boolean get() = !usedVirtualLocation

    fun asDto(): RunRecordDto = RunRecordDto(
        id = null,
        recordId = serverRecordId,
        distance = distanceMeters,
        duration = durationSeconds.toDouble(),
        date = Date(completedAtMillis),
        step = steps,
        track = track.map { listOf(it.longitude, it.latitude, it.status.toDouble()) },
        uploaded = uploaded,
        verified = verified,
        invalidReason = invalidReason,
        morningBonus = morningBonus,
        photoPath = photoRemotePath,
        localId = localId
    )
}

class LocalRunRecordStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<LocalRunRecord>>() {}.type

    fun create(
        userId: String?,
        startedAtMillis: Long,
        completedAtMillis: Long,
        durationSeconds: Int,
        distanceMeters: Int,
        steps: Int,
        track: List<TrackPoint>,
        checkField: String?,
        metricSamples: List<RunMetricSample>,
        usedVirtualLocation: Boolean
    ): LocalRunRecord = synchronized(STORE_LOCK) {
        val record = LocalRunRecord(
            localId = UUID.randomUUID().toString(),
            userId = userId,
            startedAtMillis = startedAtMillis,
            completedAtMillis = completedAtMillis,
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters,
            steps = steps,
            track = track,
            checkField = checkField,
            metricSamples = metricSamples,
            usedVirtualLocation = usedVirtualLocation
        )
        val records = readRecords()
        records += record
        writeRecords(records)
        record
    }

    fun find(localId: String): LocalRunRecord? =
        synchronized(STORE_LOCK) { readRecords().firstOrNull { it.localId == localId } }

    fun all(userId: String?): List<LocalRunRecord> = synchronized(STORE_LOCK) {
        readRecords()
            .filter { it.userId == userId }
            .sortedByDescending(LocalRunRecord::completedAtMillis)
    }

    fun delete(localId: String): LocalRunRecord? = synchronized(STORE_LOCK) {
        val records = readRecords()
        val index = records.indexOfFirst { it.localId == localId }
        if (index < 0) return null
        val removed = records.removeAt(index)
        writeRecords(records)
        removed
    }

    fun observeChanges(): Flow<Unit> = CHANGE_EVENTS.asSharedFlow()

    fun setPhoto(localId: String, photoFilePath: String): LocalRunRecord? =
        update(localId) {
            it.copy(
                photoFilePath = photoFilePath,
                photoUploaded = false,
                lastUploadErrorCode = null,
                lastUploadError = null
            )
        }

    fun markUploaded(localId: String, result: RunRecordDto): LocalRunRecord? =
        update(localId) { record ->
            check(record.canUpload) { "虚拟定位测试记录仅保存在本机，不能上传官方服务器" }
            record.copy(
                uploaded = true,
                serverRecordId = result.serverId,
                verified = result.verified,
                invalidReason = result.invalidReason,
                morningBonus = result.morningBonus,
                photoUploaded = !record.photoFilePath.isNullOrBlank(),
                photoRemotePath = result.photoPath,
                photoFilePath = null,
                lastUploadErrorCode = null,
                lastUploadError = null
            )
        }

    fun markUploadFailed(localId: String, code: Int?, message: String): LocalRunRecord? =
        update(localId) {
            it.copy(lastUploadErrorCode = code, lastUploadError = message)
        }

    private fun update(
        localId: String,
        transform: (LocalRunRecord) -> LocalRunRecord
    ): LocalRunRecord? = synchronized(STORE_LOCK) {
        val records = readRecords()
        val index = records.indexOfFirst { it.localId == localId }
        if (index < 0) return null
        val updated = transform(records[index])
        records[index] = updated
        writeRecords(records)
        updated
    }

    private fun readRecords(): MutableList<LocalRunRecord> {
        val payload = preferences.getString(KEY_RECORDS, null) ?: return mutableListOf()
        return runCatching {
            gson.fromJson<MutableList<LocalRunRecord>>(payload, listType) ?: mutableListOf()
        }.getOrDefault(mutableListOf())
    }

    private fun writeRecords(records: List<LocalRunRecord>) {
        check(preferences.edit().putString(KEY_RECORDS, gson.toJson(records)).commit()) {
            "本地记录写入失败"
        }
        CHANGE_EVENTS.tryEmit(Unit)
    }

    companion object {
        private const val FILE_NAME = "openrunner_local_runs"
        private const val KEY_RECORDS = "records"
        private val STORE_LOCK = Any()
        private val CHANGE_EVENTS = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }
}
