package cn.edu.pku.openrunner.feature.records.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.run.domain.RunChartData
import cn.edu.pku.openrunner.feature.run.domain.RunChartPoint
import cn.edu.pku.openrunner.feature.run.domain.RunMetrics
import java.util.Locale
import kotlin.math.max

enum class RunChartType {
    DISTANCE,
    PACE
}

class RunLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(11f)
    }
    private val linePath = Path()
    private var points: List<RunChartPoint> = emptyList()
    private var type: RunChartType = RunChartType.DISTANCE

    // Axis maxima, recomputed only when the data changes.
    // They used to be recomputed on demand from inside the per-point draw loop, so a
    // distance chart did one full scan of the series for every point it drew: O(n^2)
    // per frame, and a marathon-length record carries tens of thousands of samples.
    private var maximumElapsedMillis = 1f
    private var maximumDistanceKilometres = 0.1

    fun setData(points: List<RunChartPoint>, type: RunChartType) {
        val ordered = points.sortedBy(RunChartPoint::elapsedMillis)
        this.points = ordered
        this.type = type
        maximumElapsedMillis = max(ordered.lastOrNull()?.elapsedMillis?.toFloat() ?: 0f, 1f)
        maximumDistanceKilometres = max(ordered.maxOfOrNull(RunChartPoint::value) ?: 0.0, 0.1)
        contentDescription = context.getString(
            if (type == RunChartType.DISTANCE) {
                R.string.record_chart_distance_description
            } else {
                R.string.record_chart_pace_description
            },
            points.size
        )
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = dp(320f).toInt() + paddingLeft + paddingRight
        val desiredHeight = dp(210f).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        gridPaint.color = ContextCompat.getColor(context, R.color.or_outline)
        labelPaint.color = ContextCompat.getColor(context, R.color.or_muted)
        linePaint.color = ContextCompat.getColor(
            context,
            if (type == RunChartType.DISTANCE) R.color.map_route else R.color.or_accent
        )

        val left = paddingLeft + dp(48f)
        val top = paddingTop + dp(14f)
        val right = width - paddingRight - dp(12f)
        val bottom = height - paddingBottom - dp(30f)
        if (right <= left || bottom <= top) return

        repeat(GRID_DIVISIONS + 1) { index ->
            val ratio = index.toFloat() / GRID_DIVISIONS
            val y = top + (bottom - top) * ratio
            canvas.drawLine(left, y, right, y, gridPaint)
            labelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(yLabel(ratio), left - dp(7f), y - labelPaint.fontMetrics.ascent / 3f, labelPaint)
        }
        repeat(TIME_DIVISIONS + 1) { index ->
            val ratio = index.toFloat() / TIME_DIVISIONS
            val x = left + (right - left) * ratio
            canvas.drawLine(x, top, x, bottom, gridPaint)
            labelPaint.textAlign = when (index) {
                0 -> Paint.Align.LEFT
                TIME_DIVISIONS -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(
                RunMetrics.formatDuration((maximumElapsedMillis * ratio / 1_000f).toInt()),
                x,
                bottom + dp(20f),
                labelPaint
            )
        }

        if (points.isEmpty()) {
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                context.getString(R.string.record_chart_no_data),
                (left + right) / 2f,
                (top + bottom) / 2f,
                labelPaint
            )
            return
        }

        linePath.reset()
        points.forEachIndexed { index, point ->
            val x = left + (right - left) *
                (point.elapsedMillis.toFloat() / maximumElapsedMillis)
            val y = valueToY(point.value, top, bottom)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        canvas.drawPath(linePath, linePaint)
        if (points.size == 1) {
            canvas.drawCircle(
                left + (right - left) *
                    (points.first().elapsedMillis.toFloat() / maximumElapsedMillis),
                valueToY(points.first().value, top, bottom),
                dp(3f),
                linePaint
            )
        }
    }

    private fun valueToY(value: Double, top: Float, bottom: Float): Float {
        return when (type) {
            RunChartType.DISTANCE -> {
                bottom - ((value / maximumDistanceKilometres)
                    .coerceIn(0.0, 1.0) * (bottom - top)).toFloat()
            }
            RunChartType.PACE -> {
                val ratio = (value - RunChartData.MIN_PACE_MINUTES_PER_KM) /
                    (RunChartData.MAX_PACE_MINUTES_PER_KM -
                        RunChartData.MIN_PACE_MINUTES_PER_KM)
                top + (ratio.coerceIn(0.0, 1.0) * (bottom - top)).toFloat()
            }
        }
    }

    private fun yLabel(ratio: Float): String = when (type) {
        RunChartType.DISTANCE -> String.format(
            Locale.getDefault(),
            "%.1f",
            maximumDistanceKilometres * (1f - ratio)
        )
        RunChartType.PACE -> {
            val pace = RunChartData.MIN_PACE_MINUTES_PER_KM + ratio *
                (RunChartData.MAX_PACE_MINUTES_PER_KM -
                    RunChartData.MIN_PACE_MINUTES_PER_KM)
            RunMetrics.formatPace((pace * 60).toInt())
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        resources.displayMetrics
    )

    companion object {
        private const val GRID_DIVISIONS = 4
        private const val TIME_DIVISIONS = 2
    }
}
