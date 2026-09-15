package cn.edu.pku.openrunner.feature.run.data

import cn.edu.pku.openrunner.core.network.ApiClient
import cn.edu.pku.openrunner.core.network.ApiException
import cn.edu.pku.openrunner.core.network.PkuNewYouthApi
import cn.edu.pku.openrunner.core.network.RunRecordDto
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.run.domain.RunRecordDraft
import cn.edu.pku.openrunner.feature.run.domain.TrackPoint
import com.google.gson.Gson
import android.net.Uri
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody

class RecordAlreadyUploadedException : Exception("服务器已存在该记录，本地重复项已删除")

class RunRecordRepository(
    private val sessionStore: SessionStore,
    private val localStore: LocalRunRecordStore,
    private val photoStore: RunPhotoStore,
    private val api: PkuNewYouthApi = ApiClient.api
) {
    suspend fun save(draft: RunRecordDraft): LocalRunRecord = withContext(Dispatchers.IO) {
        val completedAt = draft.startedAtMillis + draft.durationSeconds * 1_000L
        val userId = sessionStore.userId
        val track = markTrackBoundaries(draft.track)
        localStore.create(
            userId = userId,
            startedAtMillis = draft.startedAtMillis,
            completedAtMillis = completedAt,
            durationSeconds = draft.durationSeconds,
            distanceMeters = draft.distanceMeters,
            steps = draft.steps,
            track = track,
            checkField = userId?.let {
                RunRecordSecurity.generateCheckField(it, completedAt)
            }
        )
    }

    suspend fun attachPhoto(localId: String, source: Uri): LocalRunRecord =
        withContext(Dispatchers.IO) {
            val record = localStore.find(localId) ?: error("本地记录不存在")
            check(!record.uploaded) { "已上传的记录不能更换图片" }
            val path = photoStore.save(source, localId)
            localStore.setPhoto(localId, path) ?: error("图片信息保存失败")
        }

    suspend fun upload(localId: String): RunRecordDto = withContext(Dispatchers.IO) {
        val record = localStore.find(localId) ?: error("本地记录不存在")
        check(!record.uploaded) { "记录已经上传" }
        try {
            val userId = sessionStore.userId ?: error("请先登录再上传记录")
            check(record.userId == userId) { "记录所属账号与当前账号不一致" }
            check(
                RunRecordSecurity.verifyCheckField(
                    userId,
                    record.completedAtMillis,
                    record.checkField
                )
            ) { "本地记录完整性校验失败" }

            val detail = Gson().toJson(
                record.track.map { point ->
                    listOf(point.longitude, point.latitude, point.status)
                }
            )
            val roundedSteps = (record.steps / STEP_BUCKET) * STEP_BUCKET
            val uploaded = api.uploadRecord(
                userId = userId,
                duration = record.durationSeconds.asBody(),
                distance = record.distanceMeters.asBody(),
                date = record.completedAtMillis.asBody(),
                detail = detail.asBody(),
                misc = MISC.asBody(),
                step = roundedSteps.asBody(),
                abstract = RunRecordSecurity.uploadAbstract(
                    userId,
                    record.completedAtMillis
                ).asBody(),
                photo = record.photoFilePath.toPhotoPart()
            ).requireData()
            localStore.markUploaded(localId, uploaded)
            photoStore.delete(record.photoFilePath)
            uploaded
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            if (error is ApiException && error.code == ALREADY_UPLOADED_CODE) {
                localStore.delete(localId)
                photoStore.delete(record.photoFilePath)
                throw RecordAlreadyUploadedException()
            }
            localStore.markUploadFailed(
                localId,
                (error as? ApiException)?.code,
                error.message ?: "记录上传失败"
            )
            throw error
        }
    }

    suspend fun localRecords(): List<LocalRunRecord> = withContext(Dispatchers.IO) {
        localStore.all(sessionStore.userId)
    }

    suspend fun deleteLocal(localId: String): Boolean = withContext(Dispatchers.IO) {
        val removed = localStore.delete(localId) ?: return@withContext false
        photoStore.delete(removed.photoFilePath)
        true
    }

    fun observeLocalChanges(): Flow<Unit> = localStore.observeChanges()

    private fun markTrackBoundaries(points: List<TrackPoint>): List<TrackPoint> {
        return points.mapIndexed { index, point ->
            point.copy(
                status = when (index) {
                    0 -> STATUS_BEGIN
                    points.lastIndex -> STATUS_END
                    else -> point.status
                }
            )
        }
    }

    private fun Any.asBody(): RequestBody = RequestBody.create(TEXT_MEDIA_TYPE, toString())

    private fun String?.toPhotoPart(): MultipartBody.Part? {
        if (isNullOrBlank()) return null
        val file = File(this)
        check(file.isFile) { "已选择的图片不存在，请重新选择" }
        return MultipartBody.Part.createFormData(
            "photo",
            "image.jpg",
            RequestBody.create(IMAGE_MEDIA_TYPE, file)
        )
    }

    companion object {
        private const val STATUS_BEGIN = 1
        private const val STATUS_END = 2
        private const val STEP_BUCKET = 17
        private const val ALREADY_UPLOADED_CODE = 4
        private const val MISC = "{\"agent\":\"Android v1.2+\"}"
        private val TEXT_MEDIA_TYPE = MediaType.parse("text/plain")
        private val IMAGE_MEDIA_TYPE = MediaType.parse("image/jpeg")
    }
}
