package cn.edu.pku.openrunner.feature.run.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

/**
 * Lightweight replacement for the legacy MapPresenter accelerometer step detector.
 * It deliberately uses the accelerometer so it does not require activity-recognition permission.
 */
class AccelerometerStepCounter(context: Context) : SensorEventListener {
    private val sensorManager = context.applicationContext
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravity = FloatArray(3)
    private var initialized = false
    private var smoothedLinearAcceleration = 0f
    private var aboveThreshold = false
    private var lastStepAtMillis = 0L
    private var onStep: (() -> Unit)? = null

    fun start(onStep: () -> Unit): Boolean {
        stop()
        val sensor = accelerometer ?: return false
        resetFilter()
        this.onStep = onStep
        return sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        onStep = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || onStep == null) return
        if (!initialized) {
            for (axis in 0..2) gravity[axis] = event.values[axis]
            initialized = true
            return
        }

        var squaredMagnitude = 0f
        for (axis in 0..2) {
            gravity[axis] = GRAVITY_ALPHA * gravity[axis] +
                (1f - GRAVITY_ALPHA) * event.values[axis]
            val linearAcceleration = event.values[axis] - gravity[axis]
            squaredMagnitude += linearAcceleration * linearAcceleration
        }
        val magnitude = sqrt(squaredMagnitude)
        smoothedLinearAcceleration = SMOOTHING_ALPHA * smoothedLinearAcceleration +
            (1f - SMOOTHING_ALPHA) * magnitude
        val isAboveThreshold = smoothedLinearAcceleration >= STEP_THRESHOLD
        val now = SystemClock.elapsedRealtime()
        if (
            isAboveThreshold &&
            !aboveThreshold &&
            now - lastStepAtMillis >= MIN_STEP_INTERVAL_MILLIS
        ) {
            lastStepAtMillis = now
            onStep?.invoke()
        }
        aboveThreshold = isAboveThreshold
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun resetFilter() {
        gravity.fill(0f)
        initialized = false
        smoothedLinearAcceleration = 0f
        aboveThreshold = false
        lastStepAtMillis = 0L
    }

    companion object {
        private const val GRAVITY_ALPHA = 0.8f
        private const val SMOOTHING_ALPHA = 0.72f
        private const val STEP_THRESHOLD = 1.15f
        private const val MIN_STEP_INTERVAL_MILLIS = 250L
    }
}
