package cn.edu.pku.openrunner.feature.records.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.PathInterpolator
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

    /**
     * 曲线生长进度，0 = 一根都没画，1 = 画满。
     *
     * 初值给 1 而不是 0：数据为空、或者动画被跳过时按「画满」处理，
     * 绝不会出现「有数据却什么都没画」这种最难查的状态。
     */
    private var growth = 1f

    /** onDraw 里用的裁剪框。预建一次，逐帧绘制里不新建对象。 */
    private val growthClip = RectF()

    private val growthAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = GROWTH_DURATION_MILLIS
        interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
        addUpdateListener {
            growth = it.animatedValue as Float
            invalidate()
        }
    }

    // Axis maxima, recomputed only when the data changes.
    // They used to be recomputed on demand from inside the per-point draw loop, so a
    // distance chart did one full scan of the series for every point it drew: O(n^2)
    // per frame, and a marathon-length record carries tens of thousands of samples.
    private var maximumElapsedMillis = 1f
    private var maximumDistanceKilometres = 0.1

    fun setData(points: List<RunChartPoint>, type: RunChartType) {
        val ordered = points.sortedBy(RunChartPoint::elapsedMillis)
        // 同一份数据重复下发时不重播生长动画 —— 状态流会把同一个列表再发一次，
        // 每发一次就重长一遍会变成闪。换图（距离 ↔ 配速）算换了内容，重播一次。
        val dataChanged = ordered != this.points || type != this.type
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
        if (dataChanged) startGrowth()
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
        // 生长动画只裁曲线这一层：网格与刻度是背景，不该跟着长。
        //
        // 用 clipRect 而不是 PathMeasure —— 折线与单点圆是两种图元，
        // 裁一条边能让它们一起被「揭」出来，也不必重建 Path。
        // 裁边比线宽与圆点半径各多留一点余量，否则右端会被削成平口。
        val slack = dp(GROWTH_CLIP_SLACK_DP)
        growthClip.set(
            left - slack,
            top - slack,
            left + (right - left) * growth + slack,
            bottom + slack
        )
        canvas.save()
        canvas.clipRect(growthClip)
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
        canvas.restore()
    }

    /**
     * 从左边把曲线「揭」出来。
     *
     * 详情页一屏只有两张曲线，生长就是它们的入场方式，所以数据一到位就播。
     * 不判断 View 是否已挂到窗口上：动画本身不依赖附着，期间多画几帧空白没有代价；
     * 万一动画播完才上屏，growth 已经是 1，画面也是对的。
     */
    private fun startGrowth() {
        growthAnimator.cancel()
        if (points.isEmpty()) {
            growth = 1f
            invalidate()
            return
        }
        growth = 0f
        growthAnimator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 动画每帧都要 invalidate 这个 View，别让它在视图消失之后继续持有引用。
        growthAnimator.cancel()
        growth = 1f
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

        /** 生长动画时长。比页面转场长得多是有意的：曲线要读得出来是「长出来」的。 */
        private const val GROWTH_DURATION_MILLIS = 700L

        /** 生长裁剪时右端多留的余量，够盖住线宽的一半与单点圆的半径。 */
        private const val GROWTH_CLIP_SLACK_DP = 5f
    }
}
