package cn.edu.pku.openrunner.feature.run.ui

import android.os.SystemClock
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.pku.openrunner.feature.run.data.AndroidLocationTracker
import cn.edu.pku.openrunner.feature.run.data.AccelerometerStepCounter
import cn.edu.pku.openrunner.feature.run.data.LocationSample
import cn.edu.pku.openrunner.feature.run.data.RunRecordRepository
import cn.edu.pku.openrunner.feature.run.data.RecordAlreadyUploadedException
import cn.edu.pku.openrunner.feature.run.domain.RunMetrics
import cn.edu.pku.openrunner.feature.run.domain.RunRecordDraft
import cn.edu.pku.openrunner.feature.run.domain.TrackDistance
import cn.edu.pku.openrunner.feature.run.domain.TrackPoint
import cn.edu.pku.openrunner.core.network.ApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RunStatus {
    IDLE,
    RUNNING,
    FINISHED,
    PERMISSION_REQUIRED
}

enum class RecordSaveStatus {
    NONE,
    SAVING,
    SAVED,
    ATTACHING_PHOTO,
    UPLOADING,
    UPLOADED,
    UPLOADED_INVALID,
    ERROR
}

sealed interface RunUiEvent {
    data class UploadCompleted(val verified: Boolean, val invalidReason: Int) : RunUiEvent
    data object AlreadyUploaded : RunUiEvent
}

data class RunUiState(
    val status: RunStatus = RunStatus.IDLE,
    val durationSeconds: Int = 0,
    val distanceMeters: Int = 0,
    val paceSecondsPerKm: Int? = null,
    val stepCount: Int = 0,
    val pointCount: Int = 0,
    val points: List<TrackPoint> = emptyList(),
    val currentPoint: TrackPoint? = null,
    val accuracyMeters: Int? = null,
    val locating: Boolean = false,
    val backgroundTrackingActive: Boolean = false,
    val recordSaveStatus: RecordSaveStatus = RecordSaveStatus.NONE,
    val savedRecordId: String? = null,
    val photoAttached: Boolean = false,
    val recordErrorCode: Int? = null,
    val recordError: String? = null
)

class RunViewModel(
    private val locationTracker: AndroidLocationTracker,
    private val stepCounter: AccelerometerStepCounter,
    private val recordRepository: RunRecordRepository
) : ViewModel() {
    private val points = mutableListOf<TrackPoint>()
    private val _uiState = MutableStateFlow(RunUiState())
    val uiState: StateFlow<RunUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<RunUiEvent>(extraBufferCapacity = 2)
    val events = _events
    private var startedAtMillis = 0L
    private var startedAtElapsedMillis = 0L
    private var timerJob: Job? = null

    fun startLocating(): Boolean {
        if (_uiState.value.locating) return true
        val started = locationTracker.start(::onLocation)
        _uiState.value = if (started) {
            _uiState.value.copy(
                locating = true,
                status = if (_uiState.value.status == RunStatus.PERMISSION_REQUIRED) {
                    RunStatus.IDLE
                } else {
                    _uiState.value.status
                }
            )
        } else {
            _uiState.value.copy(status = RunStatus.PERMISSION_REQUIRED, locating = false)
        }
        return started
    }

    fun stopLocatingIfIdle() {
        if (_uiState.value.status == RunStatus.RUNNING) return
        locationTracker.stop()
        _uiState.value = _uiState.value.copy(locating = false)
    }

    fun start() {
        if (_uiState.value.recordSaveStatus in setOf(
                RecordSaveStatus.SAVING,
                RecordSaveStatus.ATTACHING_PHOTO,
                RecordSaveStatus.UPLOADING
            )
        ) return
        if (!startLocating()) {
            _uiState.value = _uiState.value.copy(status = RunStatus.PERMISSION_REQUIRED)
            return
        }
        points.clear()
        startedAtMillis = System.currentTimeMillis()
        startedAtElapsedMillis = SystemClock.elapsedRealtime()
        val backgroundTrackingActive = locationTracker.enableBackgroundTracking()
        _uiState.value = _uiState.value.copy(
            status = RunStatus.RUNNING,
            durationSeconds = 0,
            distanceMeters = 0,
            paceSecondsPerKm = null,
            stepCount = 0,
            pointCount = 0,
            points = emptyList(),
            backgroundTrackingActive = backgroundTrackingActive,
            recordSaveStatus = RecordSaveStatus.NONE,
            savedRecordId = null,
            photoAttached = false,
            recordErrorCode = null,
            recordError = null
        )
        stepCounter.start(::onStep)
        startTimer()
    }

    fun stop() {
        if (_uiState.value.status != RunStatus.RUNNING) return
        updateElapsedTime()
        timerJob?.cancel()
        timerJob = null
        stepCounter.stop()
        locationTracker.disableBackgroundTracking()
        val finishedState = _uiState.value.copy(
            status = RunStatus.FINISHED,
            backgroundTrackingActive = false,
            recordSaveStatus = RecordSaveStatus.SAVING,
            recordError = null
        )
        _uiState.value = finishedState
        val draft = RunRecordDraft(
            startedAtMillis = startedAtMillis,
            durationSeconds = finishedState.durationSeconds,
            track = points.toList(),
            steps = finishedState.stepCount
        )
        viewModelScope.launch {
            runCatching { recordRepository.save(draft) }
                .onSuccess { record ->
                    _uiState.value = _uiState.value.copy(
                        recordSaveStatus = RecordSaveStatus.SAVED,
                        savedRecordId = record.localId,
                        photoAttached = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        recordSaveStatus = RecordSaveStatus.ERROR,
                        recordError = error.message ?: "记录保存失败"
                    )
                }
        }
    }

    fun attachPhoto(source: Uri) {
        val localId = _uiState.value.savedRecordId ?: return
        if (_uiState.value.recordSaveStatus in setOf(
                RecordSaveStatus.ATTACHING_PHOTO,
                RecordSaveStatus.UPLOADING,
                RecordSaveStatus.UPLOADED,
                RecordSaveStatus.UPLOADED_INVALID
            )
        ) return
        _uiState.value = _uiState.value.copy(
            recordSaveStatus = RecordSaveStatus.ATTACHING_PHOTO,
            recordErrorCode = null,
            recordError = null
        )
        viewModelScope.launch {
            runCatching { recordRepository.attachPhoto(localId, source) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        recordSaveStatus = RecordSaveStatus.SAVED,
                        photoAttached = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        recordSaveStatus = RecordSaveStatus.ERROR,
                        recordError = error.message ?: "图片处理失败"
                    )
                }
        }
    }

    fun uploadSavedRecord() {
        val localId = _uiState.value.savedRecordId ?: return
        if (_uiState.value.recordSaveStatus == RecordSaveStatus.UPLOADING) return
        _uiState.value = _uiState.value.copy(
            recordSaveStatus = RecordSaveStatus.UPLOADING,
            recordError = null
        )
        viewModelScope.launch {
            runCatching { recordRepository.upload(localId) }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        recordSaveStatus = if (result.verified) {
                            RecordSaveStatus.UPLOADED
                        } else {
                            RecordSaveStatus.UPLOADED_INVALID
                        },
                        recordErrorCode = result.invalidReason,
                        recordError = null
                    )
                    _events.tryEmit(
                        RunUiEvent.UploadCompleted(result.verified, result.invalidReason)
                    )
                }
                .onFailure { error ->
                    if (error is RecordAlreadyUploadedException) {
                        _uiState.value = _uiState.value.copy(
                            recordSaveStatus = RecordSaveStatus.UPLOADED,
                            savedRecordId = null,
                            recordErrorCode = null,
                            recordError = null
                        )
                        _events.tryEmit(RunUiEvent.AlreadyUploaded)
                        return@onFailure
                    }
                    _uiState.value = _uiState.value.copy(
                        recordSaveStatus = RecordSaveStatus.ERROR,
                        recordErrorCode = (error as? ApiException)?.code,
                        recordError = error.message ?: "记录上传失败"
                    )
                }
        }
    }

    private fun onLocation(sample: LocationSample) {
        val current = _uiState.value
        if (current.status == RunStatus.RUNNING && shouldAddToTrack(sample)) {
            points += sample.point
        }
        _uiState.value = current.copy(
            currentPoint = sample.point,
            accuracyMeters = sample.accuracyMeters.toInt(),
            distanceMeters = TrackDistance.polylineMeters(points).toInt(),
            pointCount = points.size,
            points = points.toList(),
            locating = true
        ).withUpdatedPace()
    }

    private fun onStep() {
        if (_uiState.value.status != RunStatus.RUNNING) return
        _uiState.value = _uiState.value.copy(stepCount = _uiState.value.stepCount + 1)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.status == RunStatus.RUNNING) {
                updateElapsedTime()
                delay(TIMER_INTERVAL_MILLIS)
            }
        }
    }

    private fun updateElapsedTime() {
        if (startedAtElapsedMillis == 0L) return
        val seconds = ((SystemClock.elapsedRealtime() - startedAtElapsedMillis) / 1_000L).toInt()
        _uiState.value = _uiState.value.copy(durationSeconds = seconds).withUpdatedPace()
    }

    private fun RunUiState.withUpdatedPace(): RunUiState = copy(
        paceSecondsPerKm = RunMetrics.paceSecondsPerKm(durationSeconds, distanceMeters)
    )

    private fun shouldAddToTrack(sample: LocationSample): Boolean {
        if (points.isEmpty()) return true
        if (sample.accuracyMeters > 100f) return false
        val distance = TrackDistance.haversineMeters(points.last(), sample.point)
        return distance in 0.5..200.0
    }

    override fun onCleared() {
        timerJob?.cancel()
        stepCounter.stop()
        locationTracker.destroy()
        super.onCleared()
    }

    class Factory(
        private val tracker: AndroidLocationTracker,
        private val stepCounter: AccelerometerStepCounter,
        private val recordRepository: RunRecordRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RunViewModel(tracker, stepCounter, recordRepository) as T
        }
    }

    companion object {
        private const val TIMER_INTERVAL_MILLIS = 250L
    }
}
