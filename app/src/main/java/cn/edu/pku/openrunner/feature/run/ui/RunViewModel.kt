package cn.edu.pku.openrunner.feature.run.ui

import android.os.SystemClock
import android.content.Context
import cn.edu.pku.openrunner.core.session.SessionStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.pku.openrunner.feature.run.data.AndroidLocationTracker
import cn.edu.pku.openrunner.feature.run.data.AccelerometerStepCounter
import cn.edu.pku.openrunner.feature.run.data.LocationSample
import cn.edu.pku.openrunner.feature.run.data.RunRecordRepository
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecordStore
import cn.edu.pku.openrunner.feature.run.data.RunPhotoStore
import cn.edu.pku.openrunner.feature.run.domain.RunMetrics
import cn.edu.pku.openrunner.feature.run.domain.RunMetricSample
import cn.edu.pku.openrunner.feature.run.domain.RunRecordDraft
import cn.edu.pku.openrunner.feature.run.domain.TrackDistance
import cn.edu.pku.openrunner.feature.run.domain.TrackPoint
import kotlinx.coroutines.CancellationException
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
    ERROR
}

enum class RunPrimaryAction {
    START,
    STOP,
    SAVE
}

sealed interface RunUiEvent {
    data class Message(val text: String) : RunUiEvent
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
    val recordError: String? = null,
    val virtualLocationEnabled: Boolean = false,
    val virtualPoint: TrackPoint? = null,
    val usedVirtualLocation: Boolean = false
) {
    val hasPendingRecord: Boolean
        get() = status == RunStatus.FINISHED && recordSaveStatus != RecordSaveStatus.SAVED

    val isSavingRecord: Boolean
        get() = recordSaveStatus == RecordSaveStatus.SAVING

    val primaryAction: RunPrimaryAction
        get() = when {
            status == RunStatus.RUNNING -> RunPrimaryAction.STOP
            hasPendingRecord -> RunPrimaryAction.SAVE
            else -> RunPrimaryAction.START
        }

    fun withVirtualLocationMode(enabled: Boolean): RunUiState = copy(
        virtualLocationEnabled = enabled,
        usedVirtualLocation = usedVirtualLocation || (enabled && status == RunStatus.RUNNING),
        locating = false,
        backgroundTrackingActive = false,
        currentPoint = null,
        accuracyMeters = null
    )
}

class RunViewModel(
    private val locationTracker: AndroidLocationTracker,
    private val stepCounter: AccelerometerStepCounter,
    private val recordRepository: RunRecordRepository
) : ViewModel() {
    private val points = mutableListOf<TrackPoint>()
    private val metricSamples = mutableListOf<RunMetricSample>()
    private val _uiState = MutableStateFlow(RunUiState())
    val uiState: StateFlow<RunUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<RunUiEvent>(extraBufferCapacity = 2)
    val events = _events
    private var startedAtMillis = 0L
    private var startedAtElapsedMillis = 0L
    private var timerJob: Job? = null
    private var pendingDraft: RunRecordDraft? = null
    private var pendingLocationSourceChange = false

    fun startLocating(): Boolean {
        if (_uiState.value.virtualLocationEnabled) {
            _uiState.value = _uiState.value.copy(
                locating = true,
                status = if (_uiState.value.status == RunStatus.PERMISSION_REQUIRED) {
                    RunStatus.IDLE
                } else {
                    _uiState.value.status
                }
            )
            return true
        }
        if (_uiState.value.locating) return true
        val started = locationTracker.start { sample -> onLocation(sample, virtual = false) }
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
            _uiState.value.copy(
                status = when {
                    _uiState.value.status == RunStatus.RUNNING -> RunStatus.RUNNING
                    _uiState.value.hasPendingRecord -> RunStatus.FINISHED
                    else -> RunStatus.PERMISSION_REQUIRED
                },
                locating = false
            )
        }
        if (started && _uiState.value.status == RunStatus.RUNNING) {
            _uiState.value = _uiState.value.copy(
                backgroundTrackingActive = locationTracker.enableBackgroundTracking()
            )
        }
        return started
    }

    fun setVirtualLocationEnabled(enabled: Boolean) {
        val current = _uiState.value
        if (current.virtualLocationEnabled == enabled) return
        locationTracker.stop()
        pendingLocationSourceChange = current.status == RunStatus.RUNNING
        _uiState.value = current.withVirtualLocationMode(enabled)
        if (enabled) {
            startLocating()
            current.virtualPoint?.let(::confirmVirtualPoint)
        } else {
            if (!startLocating()) {
                _events.tryEmit(RunUiEvent.Message("真实定位未就绪，请检查定位权限；跑步计时不会重置"))
            }
        }
    }

    fun confirmVirtualPoint(point: TrackPoint) {
        if (!_uiState.value.virtualLocationEnabled ||
            !point.longitude.isFinite() || !point.latitude.isFinite() ||
            point.longitude !in -180.0..180.0 || point.latitude !in -90.0..90.0
        ) return
        _uiState.value = _uiState.value.copy(virtualPoint = point)
        onLocation(
            LocationSample(point, accuracyMeters = 0f, timestampMillis = System.currentTimeMillis()),
            virtual = true
        )
    }

    fun stopLocatingIfIdle() {
        if (_uiState.value.status == RunStatus.RUNNING) return
        locationTracker.stop()
        _uiState.value = _uiState.value.copy(locating = false)
    }

    fun start() {
        if (_uiState.value.primaryAction != RunPrimaryAction.START ||
            _uiState.value.isSavingRecord
        ) return
        if (!startLocating()) {
            _uiState.value = _uiState.value.copy(status = RunStatus.PERMISSION_REQUIRED)
            return
        }
        points.clear()
        metricSamples.clear()
        pendingDraft = null
        pendingLocationSourceChange = false
        startedAtMillis = System.currentTimeMillis()
        startedAtElapsedMillis = SystemClock.elapsedRealtime()
        metricSamples += RunMetricSample(elapsedMillis = 0L, distanceMeters = 0.0)
        val isVirtual = _uiState.value.virtualLocationEnabled
        val backgroundTrackingActive = !isVirtual && locationTracker.enableBackgroundTracking()
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
            recordError = null,
            usedVirtualLocation = isVirtual
        )
        if (isVirtual) _uiState.value.virtualPoint?.let(::confirmVirtualPoint)
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
        val completedElapsedMillis = SystemClock.elapsedRealtime() - startedAtElapsedMillis
        addMetricSample(
            elapsedMillis = completedElapsedMillis,
            distanceMeters = TrackDistance.polylineMeters(points)
        )
        val finishedState = _uiState.value.copy(
            status = RunStatus.FINISHED,
            backgroundTrackingActive = false,
            recordSaveStatus = RecordSaveStatus.NONE,
            recordError = null
        )
        pendingDraft = RunRecordDraft(
            startedAtMillis = startedAtMillis,
            completedAtMillis = startedAtMillis + completedElapsedMillis,
            durationSeconds = finishedState.durationSeconds,
            track = points.toList(),
            steps = finishedState.stepCount,
            metricSamples = metricSamples.toList(),
            usedVirtualLocation = finishedState.usedVirtualLocation
        )
        _uiState.value = finishedState
    }

    fun saveFinishedRecord() {
        if (!_uiState.value.hasPendingRecord || _uiState.value.isSavingRecord) return
        val draft = pendingDraft ?: return
        _uiState.value = _uiState.value.copy(
            recordSaveStatus = RecordSaveStatus.SAVING,
            recordError = null
        )
        viewModelScope.launch {
            try {
                val record = recordRepository.save(draft)
                pendingDraft = null
                _uiState.value = _uiState.value.copy(
                    recordSaveStatus = RecordSaveStatus.SAVED,
                    savedRecordId = record.localId
                )
                _events.tryEmit(RunUiEvent.Message(
                    if (record.usedVirtualLocation) {
                        "虚拟定位测试记录已保存到本机，不上传官方服务器"
                    } else {
                        "记录已保存到本机，请在“记录”页添加图片或上传"
                    }
                ))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    recordSaveStatus = RecordSaveStatus.ERROR,
                    recordError = error.message ?: "记录保存失败"
                )
            }
        }
    }

    fun discardFinishedRecord() {
        if (!_uiState.value.hasPendingRecord || _uiState.value.isSavingRecord) return
        pendingDraft = null
        pendingLocationSourceChange = false
        points.clear()
        metricSamples.clear()
        startedAtMillis = 0L
        startedAtElapsedMillis = 0L
        val current = _uiState.value
        _uiState.value = RunUiState(
            currentPoint = current.currentPoint,
            accuracyMeters = current.accuracyMeters,
            locating = current.locating,
            virtualLocationEnabled = current.virtualLocationEnabled,
            virtualPoint = current.virtualPoint
        )
        _events.tryEmit(RunUiEvent.Message("本次未保存记录已删除"))
    }

    private fun onLocation(sample: LocationSample, virtual: Boolean) {
        val current = _uiState.value
        if (virtual != current.virtualLocationEnabled) return
        if (current.status == RunStatus.RUNNING && shouldAddToTrack(sample, virtual)) {
            points += sample.point
            pendingLocationSourceChange = false
            addMetricSample(
                elapsedMillis = SystemClock.elapsedRealtime() - startedAtElapsedMillis,
                distanceMeters = TrackDistance.polylineMeters(points)
            )
        }
        _uiState.value = current.copy(
            currentPoint = sample.point,
            accuracyMeters = sample.accuracyMeters.toInt(),
            distanceMeters = TrackDistance.polylineMeters(points).toInt(),
            pointCount = points.size,
            points = points.toList(),
            locating = true,
            usedVirtualLocation = current.usedVirtualLocation ||
                (virtual && current.status == RunStatus.RUNNING)
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

    private fun shouldAddToTrack(sample: LocationSample, virtual: Boolean): Boolean {
        if (points.isEmpty()) return true
        if (sample.accuracyMeters > 100f) return false
        if (pendingLocationSourceChange) return true
        val distance = TrackDistance.haversineMeters(points.last(), sample.point)
        // Explicit map picks are test checkpoints, not noisy GPS jumps. Real GPS keeps its filter.
        return if (virtual) distance >= 0.5 else distance in 0.5..200.0
    }

    private fun addMetricSample(elapsedMillis: Long, distanceMeters: Double) {
        val sample = RunMetricSample(
            elapsedMillis = elapsedMillis.coerceAtLeast(0L),
            distanceMeters = distanceMeters.coerceAtLeast(0.0)
        )
        val last = metricSamples.lastOrNull()
        when {
            last == null -> metricSamples += sample
            sample.elapsedMillis > last.elapsedMillis -> metricSamples += sample
            sample.elapsedMillis == last.elapsedMillis -> metricSamples[metricSamples.lastIndex] = sample
        }
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

        companion object {
            fun from(context: Context): Factory = Factory(
                AndroidLocationTracker(context),
                AccelerometerStepCounter(context),
                RunRecordRepository(
                    SessionStore(context),
                    LocalRunRecordStore(context),
                    RunPhotoStore(context)
                )
            )
        }
    }

    companion object {
        private const val TIMER_INTERVAL_MILLIS = 250L
    }
}
