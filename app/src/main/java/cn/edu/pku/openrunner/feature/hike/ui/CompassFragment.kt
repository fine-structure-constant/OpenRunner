package cn.edu.pku.openrunner.feature.hike.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.hike.data.HikeLocation
import cn.edu.pku.openrunner.feature.hike.data.RecordedFix
import cn.edu.pku.openrunner.feature.hike.domain.trueNorthHeading
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.math.abs
import kotlin.math.roundToInt

class CompassFragment : Fragment(R.layout.fragment_compass), SensorEventListener {
    private lateinit var manager: SensorManager
    private var rotation: Sensor? = null
    private var sensorSupported = false
    private var magnetic: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gravity: FloatArray? = null
    private var field: FloatArray? = null
    private var heading: Float? = null
    private var sensorStatus: Int? = R.string.compass_waiting
    private var trueNorthRequested = false
    private var gpsEnabled = false
    private var gps: HikeLocation? = null
    private var declination: Float? = null
    private val handler = Handler(Looper.getMainLooper())
    private val expireCorrection = Runnable {
        declination = null
        render()
    }
    private val matrix = FloatArray(9)
    private val screenMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (isResumed) updateGps()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        manager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetic = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        trueNorthRequested = savedInstanceState?.getBoolean(TRUE_NORTH_REQUESTED) ?: false
        view.findViewById<SwitchMaterial>(R.id.compass_true_north).apply {
            isChecked = trueNorthRequested
            setOnCheckedChangeListener { _, checked ->
                trueNorthRequested = checked
                if (checked && !preciseLocationAllowed()) {
                    locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                updateGps()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        heading = null
        field = null
        gravity = null
        val primary = rotation
        val mag = magnetic
        val accel = accelerometer
        sensorSupported = if (primary != null &&
            manager.registerListener(this, primary, SensorManager.SENSOR_DELAY_UI)) {
            true
        } else if (mag != null && accel != null) {
            manager.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI) &&
                manager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        } else false
        if (!sensorSupported) manager.unregisterListener(this)
        sensorStatus = if (sensorSupported) R.string.compass_waiting else R.string.compass_unavailable
        view?.findViewById<SwitchMaterial>(R.id.compass_true_north)?.isEnabled = sensorSupported
        updateGps()
    }

    override fun onPause() {
        manager.unregisterListener(this)
        stopGps()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(TRUE_NORTH_REQUESTED, trueNorthRequested)
    }

    private fun preciseLocationAllowed(): Boolean = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun updateGps() {
        if (!trueNorthRequested || !sensorSupported || !preciseLocationAllowed()) {
            stopGps()
        } else if (gps == null) {
            val tracker = HikeLocation(requireContext(), ::onGpsFix) { enabled ->
                gpsEnabled = enabled
                if (!enabled) clearCorrection()
                render()
            }
            gps = tracker.takeIf { it.start(5_000L, Looper.getMainLooper()) }
        }
        render()
    }

    private fun onGpsFix(fix: RecordedFix) {
        if (!trueNorthRequested || !gpsEnabled || fix.horizontalAccuracy > 100f) return
        val remaining = FIX_VALIDITY_MS - (SystemClock.elapsedRealtime() - fix.monotonicMs)
        if (remaining <= 0) return
        val correction = GeomagneticField(
            fix.point.latitude.toFloat(), fix.point.longitude.toFloat(),
            fix.point.altitude?.toFloat() ?: 0f, fix.time
        ).declination
        if (!correction.isFinite()) return
        declination = correction
        handler.removeCallbacks(expireCorrection)
        handler.postDelayed(expireCorrection, remaining)
        render()
    }

    private fun stopGps() {
        gps?.stop()
        gps = null
        gpsEnabled = false
        clearCorrection()
    }

    private fun clearCorrection() {
        handler.removeCallbacks(expireCorrection)
        declination = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                if (event.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                    showSensorStatus(R.string.compass_calibrate)
                    return
                }
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
            }
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD -> {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values.copyOf(3)
                else {
                    field = event.values.copyOf(3).takeIf { event.accuracy > SensorManager.SENSOR_STATUS_ACCURACY_LOW }
                    if (field == null) {
                        showSensorStatus(R.string.compass_calibrate)
                        return
                    }
                }
                val g = gravity ?: return
                val f = field ?: return
                if (!SensorManager.getRotationMatrix(matrix, null, g, f)) {
                    showSensorStatus(R.string.compass_calibrate)
                    return
                }
            }
            else -> return
        }
        val screenRotation = view?.display?.rotation ?: Surface.ROTATION_0
        val x = when (screenRotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y
            else -> SensorManager.AXIS_X
        }
        val y = when (screenRotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_X
            else -> SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matrix, x, y, screenMatrix)
        SensorManager.getOrientation(screenMatrix, orientation)
        val pitch = Math.toDegrees(orientation[1].toDouble())
        if (abs(pitch) > 65) {
            showSensorStatus(R.string.compass_flat)
            return
        }
        val raw = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (!raw.isFinite()) return
        val previous = heading ?: raw
        val difference = (raw - previous + 540f) % 360f - 180f
        heading = (previous + difference * 0.22f + 360f) % 360f
        showSensorStatus(null)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    private fun showSensorStatus(status: Int?) {
        sensorStatus = status
        render()
    }

    private fun render() {
        val root = view ?: return
        val correction = declination
        val value = heading?.takeIf { sensorStatus == null }?.let { magneticHeading ->
            if (correction == null) magneticHeading else trueNorthHeading(magneticHeading, correction)
        }
        root.findViewById<CompassDial>(R.id.compass_dial).heading = value
        root.findViewById<TextView>(R.id.compass_heading).text = if (value == null) {
            getString(R.string.compass_no_heading)
        } else {
            val directions = resources.getStringArray(R.array.compass_directions)
            getString(R.string.compass_heading, directions[((value + 22.5f) / 45f).toInt() % 8], value.roundToInt() % 360)
        }
        val status = sensorStatus ?: when {
            correction != null -> R.string.compass_true_north_active
            !trueNorthRequested -> R.string.compass_magnetic
            !preciseLocationAllowed() -> R.string.compass_true_north_permission
            !gpsEnabled -> R.string.compass_true_north_gps_off
            else -> R.string.compass_true_north_waiting
        }
        root.findViewById<TextView>(R.id.compass_status).setText(status)
    }

    private companion object {
        const val TRUE_NORTH_REQUESTED = "true_north_requested"
        const val FIX_VALIDITY_MS = 20_000L
    }
}

class CompassDial @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null
) : View(context, attributes) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val needle = Path()
    private val foreground = ContextCompat.getColor(context, R.color.or_on_surface)
    private val muted = ContextCompat.getColor(context, R.color.or_muted)
    private val red = ContextCompat.getColor(context, R.color.or_primary)
    private val ring = ContextCompat.getColor(context, R.color.or_surface_container)
    var heading: Float? = null
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val radius = minOf(width, height) * 0.43f
        if (radius <= 0) return
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        paint.color = ring
        paint.style = Paint.Style.FILL
        canvas.drawCircle(0f, 0f, radius, paint)
        paint.strokeWidth = radius * 0.008f
        paint.color = muted
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(0f, 0f, radius, paint)
        paint.style = Paint.Style.FILL
        canvas.save()
        canvas.rotate(-(heading ?: 0f))
        for (tick in 0 until 72) {
            paint.color = if (tick % 18 == 0) foreground else muted
            paint.strokeWidth = radius * if (tick % 6 == 0) 0.014f else 0.007f
            canvas.drawLine(0f, -radius * 0.94f, 0f, -radius * if (tick % 6 == 0) 0.82f else 0.87f, paint)
            canvas.rotate(5f)
        }
        paint.textSize = radius * 0.17f
        paint.textAlign = Paint.Align.CENTER
        val cardinals = listOf("N", "E", "S", "W")
        for ((index, label) in cardinals.withIndex()) {
            paint.color = if (index == 0) red else foreground
            canvas.drawText(label, 0f, -radius * 0.66f - (paint.ascent() + paint.descent()) / 2, paint)
            canvas.rotate(90f)
        }
        if (heading != null) {
            needle.reset()
            needle.moveTo(0f, -radius * 0.52f)
            needle.lineTo(-radius * 0.11f, 0f)
            needle.lineTo(radius * 0.11f, 0f)
            needle.close()
            paint.color = red
            canvas.drawPath(needle, paint)
            canvas.rotate(180f)
            paint.color = muted
            canvas.drawPath(needle, paint)
            paint.color = foreground
            canvas.drawCircle(0f, 0f, radius * 0.035f, paint)
        }
        canvas.restore()
        paint.color = foreground
        paint.strokeWidth = radius * 0.018f
        canvas.drawLine(0f, -radius * 1.08f, 0f, -radius * 0.97f, paint)
        canvas.restore()
    }
}
