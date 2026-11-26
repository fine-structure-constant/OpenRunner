package cn.edu.pku.openrunner.core.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.abs
import kotlin.math.atan2

internal enum class CompassStatus { WAITING, UNAVAILABLE, CALIBRATE, TILTED }

internal class DeviceCompass(
    context: Context,
    private val displayRotation: () -> Int,
    private val allowUpright: Boolean = false,
    private val onReading: (Float?, CompassStatus?) -> Unit
) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gravity: FloatArray? = null
    private var field: FloatArray? = null
    private var heading: Float? = null
    private var status: CompassStatus? = null
    private val matrix = FloatArray(9)
    private val screenMatrix = FloatArray(9)

    fun start(): Boolean {
        stop()
        val rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val magnetic = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val supported = (rotation != null && manager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI)) ||
            (magnetic != null && accelerometer != null &&
                manager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_UI) &&
                manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI))
        if (!supported) manager.unregisterListener(this)
        report(null, if (supported) CompassStatus.WAITING else CompassStatus.UNAVAILABLE)
        return supported
    }

    fun stop() {
        manager.unregisterListener(this)
        gravity = null
        field = null
        heading = null
        status = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                if (event.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                    report(null, CompassStatus.CALIBRATE)
                    return
                }
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
            }
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD -> {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values.copyOf(3)
                else {
                    field = event.values.copyOf(3).takeIf { event.accuracy > SensorManager.SENSOR_STATUS_ACCURACY_LOW }
                    if (field == null) {
                        report(null, CompassStatus.CALIBRATE)
                        return
                    }
                }
                val acceleration = gravity ?: return
                val magnetism = field ?: return
                if (!SensorManager.getRotationMatrix(matrix, null, acceleration, magnetism)) {
                    report(null, CompassStatus.CALIBRATE)
                    return
                }
            }
            else -> return
        }

        val rotation = displayRotation()
        val x = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y
            else -> SensorManager.AXIS_X
        }
        val y = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_X
            else -> SensorManager.AXIS_Y
        }
        if (!SensorManager.remapCoordinateSystem(matrix, x, y, screenMatrix)) return
        val raw = magneticAzimuth(screenMatrix, allowUpright)
        if (raw == null) {
            report(null, CompassStatus.TILTED)
            return
        }
        report(smoothHeading(heading, raw), null)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type != Sensor.TYPE_ACCELEROMETER && accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) field = null
            report(null, CompassStatus.CALIBRATE)
        }
    }

    private fun report(value: Float?, status: CompassStatus?) {
        if (heading == value && this.status == status) return
        heading = value
        this.status = status
        onReading(value, status)
    }
}

internal fun magneticAzimuth(screenMatrix: FloatArray, allowUpright: Boolean): Float? {
    val upright = abs(screenMatrix[7]) > 0.9063f // The top edge is more than 65° above or below horizontal.
    if (upright && !allowUpright) return null
    // When held upright, the top edge has little horizontal projection. Use the back of the phone.
    val east = if (upright) -screenMatrix[2] else screenMatrix[1]
    val north = if (upright) -screenMatrix[5] else screenMatrix[4]
    val degrees = Math.toDegrees(atan2(east.toDouble(), north.toDouble())).toFloat()
    return degrees.takeIf(Float::isFinite)?.let(::wrapHeading)
}

internal fun smoothHeading(previous: Float?, target: Float): Float {
    if (previous == null) return wrapHeading(target)
    val delta = wrapHeading(target - previous + 180f) - 180f
    return wrapHeading(previous + delta * 0.22f)
}
