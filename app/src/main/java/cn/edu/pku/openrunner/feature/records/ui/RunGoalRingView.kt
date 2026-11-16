package cn.edu.pku.openrunner.feature.records.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import cn.edu.pku.openrunner.R

class RunGoalRingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val trackColor = ContextCompat.getColor(context, R.color.or_surface_container_high)
    private val primaryColor = ContextCompat.getColor(context, R.color.or_primary)
    private val overflowColor = ContextCompat.getColor(context, R.color.or_goal_overflow)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.or_ring_thickness)
    }
    private val arcBounds = RectF()

    var percent: Double = 0.0
        internal set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val radius = (minOf(w, h) - paint.strokeWidth) / 2f
        val cx = w / 2f
        val cy = h / 2f
        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val completedLaps = (percent / 100).toLong()
        val sweep = (percent % 100 * 3.6).toFloat()

        // Only the last complete lap is visible beneath the current arc.
        paint.color = if (completedLaps == 0L) trackColor else lapColor(completedLaps - 1)
        canvas.drawOval(arcBounds, paint)
        if (sweep > 0f) {
            paint.color = lapColor(completedLaps)
            canvas.drawArc(arcBounds, -90f, sweep, false, paint)
        }
    }

    private fun lapColor(index: Long): Int = if (index % 2 == 0L) primaryColor else overflowColor
}
