package cn.edu.pku.openrunner.feature.hike.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import cn.edu.pku.openrunner.R
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
    private val matrix = FloatArray(9)
    private val screenMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        manager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetic = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
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
        render(null, if (sensorSupported) R.string.compass_waiting else R.string.compass_unavailable)
    }

    override fun onPause() {
        manager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                if (event.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                    render(null, R.string.compass_calibrate)
                    return
                }
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
            }
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD -> {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values.copyOf(3)
                else {
                    field = event.values.copyOf(3).takeIf { event.accuracy > SensorManager.SENSOR_STATUS_ACCURACY_LOW }
                    if (field == null) {
                        render(null, R.string.compass_calibrate)
                        return
                    }
                }
                val g = gravity ?: return
                val f = field ?: return
                if (!SensorManager.getRotationMatrix(matrix, null, g, f)) {
                    render(null, R.string.compass_calibrate)
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
            render(null, R.string.compass_flat)
            return
        }
        val raw = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (!raw.isFinite()) return
        val previous = heading ?: raw
        val difference = (raw - previous + 540f) % 360f - 180f
        heading = (previous + difference * 0.22f + 360f) % 360f
        render(heading, R.string.compass_magnetic)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    private fun render(value: Float?, status: Int) {
        val root = view ?: return
        root.findViewById<CompassDial>(R.id.compass_dial).heading = value
        root.findViewById<TextView>(R.id.compass_heading).text = if (value == null) {
            getString(R.string.compass_no_heading)
        } else {
            val directions = resources.getStringArray(R.array.compass_directions)
            getString(R.string.compass_heading, directions[((value + 22.5f) / 45f).toInt() % 8], value.roundToInt() % 360)
        }
        root.findViewById<TextView>(R.id.compass_status).setText(status)
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
