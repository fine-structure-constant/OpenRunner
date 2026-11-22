package cn.edu.pku.openrunner.feature.hike.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cn.edu.pku.openrunner.MainActivity
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.hike.domain.HikeSample
import cn.edu.pku.openrunner.feature.hike.domain.HikeTrack
import cn.edu.pku.openrunner.feature.hike.domain.distanceMeters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

data class HikeRecording(
    val id: Long? = null,
    val active: Boolean = false,
    val gpsEnabled: Boolean = true,
    val fix: RecordedFix? = null,
    val samples: List<HikeSample> = emptyList(),
    val distance: Double = 0.0,
    val ascent: Double = 0.0,
    val error: Boolean = false
)

class HikeRecordingService : Service() {
    private val worker = HandlerThread("Hike GPS")
    private lateinit var handler: Handler
    private lateinit var store: HikeStore
    private lateinit var location: HikeLocation
    private var id: Long? = null
    private var track = HikeTrack()
    private var previousFix: RecordedFix? = null
    private var previousPoint: RecordedFix? = null
    private var sequence = 0
    private var breakBeforeNext = false

    override fun onCreate() {
        super.onCreate()
        worker.start()
        handler = Handler(worker.looper)
        store = HikeStore(applicationContext)
        location = HikeLocation(this, ::recordFix) { available ->
            if (!available) breakBeforeNext = true
            state.value = state.value.copy(gpsEnabled = available, fix = if (available) state.value.fix else null)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            handler.post { finish(startId) }
            return START_NOT_STICKY
        }
        try {
            startForeground(NOTIFICATION_ID, notification())
        } catch (_: RuntimeException) {
            state.value = HikeRecording(error = true)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        handler.post { start(startId) }
        return START_NOT_STICKY
    }

    private fun start(startId: Int) {
        if (id != null) return
        try {
            val session = store.start(System.currentTimeMillis())
            id = session
            track = HikeTrack()
            previousFix = null
            previousPoint = null
            sequence = 0
            breakBeforeNext = false
            state.value = HikeRecording(id = session, active = true)
            if (!location.start(3_000L, worker.looper)) finish(startId, error = true)
        } catch (_: RuntimeException) {
            finish(startId, error = true)
        }
    }

    private fun recordFix(fix: RecordedFix) {
        val session = id ?: return
        if (fix.monotonicMs <= (state.value.fix?.monotonicMs ?: -1)) return
        state.value = state.value.copy(fix = fix)
        if (fix.horizontalAccuracy > 50f) return
        val previous = previousFix
        if (previous != null && fix.monotonicMs <= previous.monotonicMs) return
        if (previous != null && fix.monotonicMs - previous.monotonicMs > 60_000) breakBeforeNext = true
        previousFix = fix
        val last = previousPoint
        if (last != null && !breakBeforeNext) {
            val distance = distanceMeters(last.point, fix.point)
            val minimum = max(5.0, max(last.horizontalAccuracy, fix.horizontalAccuracy) * 0.45)
            val seconds = (fix.monotonicMs - last.monotonicMs) / 1000.0
            if (seconds < 2.5 || distance < minimum || distance / seconds > 15) return
        }
        val sample = track.append(fix.point, fix.time, fix.verticalAccuracy, breakBeforeNext)
        try {
            store.append(session, sequence, sample)
        } catch (_: RuntimeException) {
            finish(error = true)
            return
        }
        sequence++
        previousPoint = fix
        breakBeforeNext = false
        state.value = state.value.copy(samples = track.snapshot(), distance = track.distance, ascent = track.ascent)
    }

    private fun finish(startId: Int? = null, error: Boolean = false) {
        location.stop()
        val saved = runCatching { id?.let { store.finish(it, System.currentTimeMillis()) } }.isSuccess
        id = null
        state.value = state.value.copy(active = false, fix = null, error = error || !saved)
        if (startId == null) stopSelf() else stopSelf(startId)
    }

    override fun onDestroy() {
        handler.post {
            location.stop()
            if (id != null) {
                runCatching { id?.let { store.finish(it, System.currentTimeMillis()) } }
                state.value = state.value.copy(active = false, fix = null)
            }
            store.close()
            worker.quitSafely()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.nav_hike), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java).setAction(ACTION_OPEN)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 2, Intent(this, HikeRecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_or_hike)
            .setContentTitle(getString(R.string.hike_recording))
            .setContentIntent(open)
            .addAction(R.drawable.ic_or_hike, getString(R.string.hike_stop), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        const val ACTION_OPEN = "cn.edu.pku.openrunner.hike.OPEN"
        const val ACTION_STOP = "cn.edu.pku.openrunner.hike.STOP"
        private const val CHANNEL_ID = "hike_recording"
        private const val NOTIFICATION_ID = 820
        private val state = MutableStateFlow(HikeRecording())
        val recording = state.asStateFlow()
    }
}
