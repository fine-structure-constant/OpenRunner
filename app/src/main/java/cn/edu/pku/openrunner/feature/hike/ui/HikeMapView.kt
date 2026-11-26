package cn.edu.pku.openrunner.feature.hike.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.hike.data.HikeMapData
import cn.edu.pku.openrunner.feature.hike.data.RecordedFix
import cn.edu.pku.openrunner.feature.hike.domain.*
import kotlin.math.*

data class HikeCamera(val x: Double, val y: Double, val zoom: Double)

class HikeMapView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null
) : View(context, attributes) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val areaRect = RectF()
    private val tileRect = RectF()
    private val labels = mutableListOf<RectF>()
    var camera = HikeCamera(0.5, 0.5, 3.0)
        private set
    var onCameraChanged: (() -> Unit)? = null
    var onSegmentSelected: ((HikeSample) -> Unit)? = null
    var hasCamera = false
        private set
    private val scale: Double get() = 256 * density * 2.0.pow(camera.zoom)
    var map: HikeMapData? = null
        set(value) {
            field = value
            features = value?.features?.map(::ProjectedFeature).orEmpty()
            invalidate()
        }
    private var features = emptyList<ProjectedFeature>()
    var reference: HikeRoute? = null
        set(value) {
            field = value
            referenceLines = value?.lines?.map { line -> line.map(::WorldPoint) }.orEmpty()
            invalidate()
        }
    private var referenceLines = emptyList<List<WorldPoint>>()
    var recording: List<HikeSample> = emptyList()
        set(value) {
            if (field === value) return
            field = value
            logged = value.map { WorldPoint(it.point) }
            trackColors = value.zipWithNext(::segmentColor)
            invalidate()
        }
    private var logged = emptyList<WorldPoint>()
    private var trackColors = emptyList<Int>()
    var fix: RecordedFix? = null
        set(value) {
            field = value
            invalidate()
        }

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(first: MotionEvent?, last: MotionEvent, dx: Float, dy: Float): Boolean {
            if (!pinch.isInProgress && last.pointerCount == 1) {
                setCamera(HikeCamera(camera.x + dx / scale, camera.y + dy / scale, camera.zoom), notify = false)
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            zoomBy(1.0, e.x, e.y)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            performClick()
            var nearest: HikeSample? = null
            var bestDistance = 18f * density
            for (index in 1 until logged.size) {
                val sample = recording[index]
                if (sample.section != recording[index - 1].section) continue
                val start = logged[index - 1]
                val end = logged[index]
                val ax = x(start.x)
                val ay = y(start.y)
                val dx = x(end.x) - ax
                val dy = y(end.y) - ay
                val lengthSquared = dx * dx + dy * dy
                if (lengthSquared <= 0f) continue
                val t = (((e.x - ax) * dx + (e.y - ay) * dy) / lengthSquared).coerceIn(0f, 1f)
                val distance = hypot(e.x - ax - t * dx, e.y - ay - t * dy)
                if (distance < bestDistance) {
                    bestDistance = distance
                    nearest = sample
                }
            }
            nearest?.let { onSegmentSelected?.invoke(it) }
            return true
        }
    })

    private val pinch = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomBy(log2(detector.scaleFactor.toDouble()), detector.focusX, detector.focusY, notify = false)
            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(event.actionMasked != MotionEvent.ACTION_UP &&
            event.actionMasked != MotionEvent.ACTION_CANCEL)
        pinch.onTouchEvent(event)
        gestures.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            onCameraChanged?.invoke()
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    fun setCamera(value: HikeCamera, notify: Boolean = true) {
        camera = HikeCamera(value.x - floor(value.x), value.y.coerceIn(0.0, 1.0), value.zoom.coerceIn(2.0, 19.0))
        hasCamera = true
        invalidate()
        if (notify) onCameraChanged?.invoke()
    }

    fun center(point: GeoPoint, zoom: Double = 15.0) {
        setCamera(HikeCamera(worldX(point.longitude), worldY(point.latitude), zoom))
    }

    fun fit(area: MapArea) {
        if (width == 0 || height == 0) {
            post { if (isAttachedToWindow) fit(area) }
            return
        }
        val horizontal = max((area.east - area.west) / 360, 0.000015)
        val vertical = max(worldY(area.south) - worldY(area.north), 0.000015)
        val pixels = min(width * 0.8 / horizontal, height * 0.78 / vertical)
        setCamera(HikeCamera(area.x, area.y, log2(pixels / (256 * density))))
    }

    fun zoomBy(amount: Double, x: Float = width / 2f, y: Float = height / 2f, notify: Boolean = true) {
        if (!amount.isFinite()) return
        val zoom = (camera.zoom + amount).coerceIn(2.0, 19.0)
        val deltaScale = (1 - 2.0.pow(camera.zoom - zoom)) / scale
        setCamera(HikeCamera(camera.x + (x - width / 2f) * deltaScale, camera.y + (y - height / 2f) * deltaScale, zoom),
            notify = notify)
    }

    private fun x(value: Double): Float = ((nearestWorldX(value, camera.x) - camera.x) * scale + width / 2).toFloat()
    private fun y(value: Double): Float = ((value - camera.y) * scale + height / 2).toFloat()

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(233, 230, 220))
        labels.clear()
        map?.let { drawMap(canvas, it) }
        drawRoutes(canvas)
        drawCurrentFix(canvas)
        drawScale(canvas)
    }

    private fun drawMap(canvas: Canvas, data: HikeMapData) {
        val area = data.info.area
        areaRect.set(x(worldX(area.west)), y(worldY(area.north)), x(worldX(area.east)), y(worldY(area.south)))
        if (areaRect.right < 0 || areaRect.left > width || areaRect.top > height || areaRect.bottom < 0 ||
            areaRect.right < areaRect.left) return
        paint.color = Color.rgb(244, 239, 220)
        paint.style = Paint.Style.FILL
        canvas.drawRect(areaRect, paint)
        canvas.save()
        canvas.clipRect(areaRect)
        for (tile in data.terrain) {
            val tileSize = (scale / tile.id.scale).toFloat()
            val left = x(tile.id.x.toDouble() / tile.id.scale)
            val top = y(tile.id.y.toDouble() / tile.id.scale)
            if (left > width || top > height || left + tileSize < 0 || top + tileSize < 0) continue
            tileRect.set(left, top, left + tileSize, top + tileSize)
            paint.color = Color.WHITE
            canvas.drawBitmap(tile.relief, null, tileRect, paint)
            val ratio = tileSize / 256
            canvas.save()
            canvas.translate(left, top)
            canvas.scale(ratio, ratio)
            paint.style = Paint.Style.STROKE
            if (camera.zoom >= 13) {
                paint.color = 0x886c573d.toInt()
                paint.strokeWidth = 0.55f * density / ratio
                canvas.drawPath(tile.minor, paint)
            }
            paint.color = 0xba624a34.toInt()
            paint.strokeWidth = 0.9f * density / ratio
            canvas.drawPath(tile.major, paint)
            canvas.restore()
            if (camera.zoom >= 14) for (label in tile.labels) {
                drawLabel(canvas, label.meters.toString(), left + label.x * ratio, top + label.y * ratio, 9f, 0xff775b32.toInt())
            }
        }
        for (type in DRAW_ORDER) for (feature in features) {
            if (feature.source.type == type) drawFeature(canvas, feature)
        }
        canvas.restore()
    }

    private fun drawFeature(canvas: Canvas, feature: ProjectedFeature) {
        val type = feature.source.type
        if ((type == MapFeatureType.LOCAL_ROAD || type == MapFeatureType.PATH) && camera.zoom < 11.5) return
        if (x(feature.east) < 0 || x(feature.west) > width || y(feature.south) < 0 || y(feature.north) > height) return
        val first = feature.points.first()
        if (type == MapFeatureType.PEAK) {
            val px = x(first.x)
            val py = y(first.y)
            path.reset()
            path.moveTo(px, py - 5 * density)
            path.lineTo(px - 5 * density, py + 4 * density)
            path.lineTo(px + 5 * density, py + 4 * density)
            path.close()
            paint.style = Paint.Style.FILL
            paint.color = PEAK
            canvas.drawPath(path, paint)
            val peakLabel = listOfNotNull(feature.source.name, feature.source.elevation?.let {
                if (it.toDoubleOrNull() != null) "$it m" else it
            }).joinToString(" ")
            if (camera.zoom >= 11) drawLabel(canvas, peakLabel, px, py - 11 * density, 12f, PEAK)
            return
        }

        path.reset()
        path.moveTo(x(first.x), y(first.y))
        for (index in 1 until feature.points.size) {
            val point = feature.points[index]
            path.lineTo(x(point.x), y(point.y))
        }
        val closedWater = type == MapFeatureType.WATER && first == feature.points.last()
        if (closedWater) path.close()
        val color = when (type) {
            MapFeatureType.WATER, MapFeatureType.WATERWAY -> WATER
            MapFeatureType.ROAD -> 0xffae652c.toInt()
            MapFeatureType.LOCAL_ROAD -> 0xff82796d.toInt()
            MapFeatureType.PATH -> 0xff776039.toInt()
            MapFeatureType.PEAK -> PEAK
        }
        val stroke = when (type) {
            MapFeatureType.ROAD -> 2.3f
            MapFeatureType.LOCAL_ROAD -> 1.5f
            else -> 1.1f
        }
        paint.style = if (closedWater) Paint.Style.FILL else Paint.Style.STROKE
        paint.strokeWidth = stroke * density
        paint.color = color
        if (type == MapFeatureType.PATH) paint.pathEffect = trailDash
        canvas.drawPath(path, paint)
        paint.pathEffect = null
        val middle = feature.points[feature.points.size / 2]
        val showName = camera.zoom >= 12 && type in NAMED_FEATURES || camera.zoom >= 15
        if (showName) feature.source.name?.let { drawLabel(canvas, it, x(middle.x), y(middle.y) - 4 * density, 10f, color) }
    }

    private val trailDash = android.graphics.DashPathEffect(floatArrayOf(4 * density, 3 * density), 0f)

    private fun drawRoutes(canvas: Canvas) {
        for (line in referenceLines) {
            path.reset()
            path.moveTo(x(line[0].x), y(line[0].y))
            for (index in 1 until line.size) path.lineTo(x(line[index].x), y(line[index].y))
            paint.style = Paint.Style.STROKE
            paint.color = Color.WHITE
            paint.strokeWidth = 5f * density
            canvas.drawPath(path, paint)
            paint.color = REFERENCE
            paint.strokeWidth = 2.7f * density
            canvas.drawPath(path, paint)
        }
        reference?.waypoints?.forEach { waypoint ->
            val px = x(worldX(waypoint.point.longitude))
            val py = y(worldY(waypoint.point.latitude))
            if (px !in 0f..width.toFloat() || py !in 0f..height.toFloat()) return@forEach
            dot(canvas, px, py, 4f, REFERENCE)
            drawLabel(canvas, waypoint.name, px, py - 9 * density, 11f, REFERENCE)
        }
        for (index in 1 until logged.size) {
            if (recording[index].section != recording[index - 1].section) continue
            val a = logged[index - 1]
            val b = logged[index]
            val ax = x(a.x)
            val ay = y(a.y)
            val bx = x(b.x)
            val by = y(b.y)
            if (max(ax, bx) < 0 || min(ax, bx) > width || max(ay, by) < 0 || min(ay, by) > height) continue
            paint.strokeWidth = 6f * density
            paint.color = Color.WHITE
            canvas.drawLine(ax, ay, bx, by, paint)
            paint.strokeWidth = 3.6f * density
            paint.color = trackColors[index - 1]
            canvas.drawLine(ax, ay, bx, by, paint)
        }
        logged.lastOrNull()?.let { dot(canvas, x(it.x), y(it.y), 4f, LEVEL) }
    }

    private fun drawCurrentFix(canvas: Canvas) {
        val fix = fix ?: return
        val point = WorldPoint(fix.point)
        val px = x(point.x)
        val py = y(point.y)
        val radius = (fix.horizontalAccuracy * scale / metersPerWorldUnit(point.y)).toFloat()
        paint.style = Paint.Style.FILL
        paint.color = 0x303b72c6
        canvas.drawCircle(px, py, radius, paint)
        dot(canvas, px, py, 6f, 0xff285fb0.toInt())
    }

    private fun drawScale(canvas: Canvas) {
        if (!hasCamera || width <= 0) return
        val metersPerPx = metersPerWorldUnit(camera.y) / scale
        val target = metersPerPx * min(width / 3f, 100 * density)
        val unit = 10.0.pow(floor(log10(target)))
        val meters = when {
            target >= 5 * unit -> 5 * unit
            target >= 2 * unit -> 2 * unit
            else -> unit
        }
        val length = (meters / metersPerPx).toFloat()
        val start = 14f * density
        val base = height - 11f * density
        paint.style = Paint.Style.FILL
        paint.color = 0xeefaf7ec.toInt()
        canvas.drawRoundRect(start - 5 * density, base - 26 * density, start + max(length, 44 * density) + 5 * density,
            base + 5 * density, 3 * density, 3 * density, paint)
        paint.color = 0xff403b31.toInt()
        paint.strokeWidth = 2 * density
        canvas.drawLine(start, base, start + length, base, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 11 * density
        val label = if (meters >= 1000) context.getString(R.string.hike_scale_km, (meters / 1000).toInt())
            else context.getString(R.string.hike_scale_m, meters.roundToInt())
        canvas.drawText(label, start, base - 7 * density, paint)
    }

    private fun drawLabel(canvas: Canvas, name: String, px: Float, py: Float, size: Float, color: Int) {
        if (name.isBlank() || px !in 0f..width.toFloat() || py !in 0f..height.toFloat() || labels.size >= 100) return
        paint.textSize = size * density
        paint.textAlign = Paint.Align.CENTER
        val half = paint.measureText(name) / 2 + 3 * density
        val box = RectF(px - half, py - paint.textSize, px + half, py + 3 * density)
        if (labels.any { RectF.intersects(it, box) }) return
        labels.add(box)
        paint.style = Paint.Style.STROKE
        paint.color = 0xfffaf5e7.toInt()
        paint.strokeWidth = 2.6f * density
        canvas.drawText(name, px, py, paint)
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawText(name, px, py, paint)
    }

    private fun dot(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(x, y, (radius + 1.5f) * density, paint)
        paint.color = color
        canvas.drawCircle(x, y, radius * density, paint)
    }

    private fun metersPerWorldUnit(y: Double): Double = 40_075_017 * cos(Math.toRadians(latitudeAt(y)))

    private data class WorldPoint(val x: Double, val y: Double) {
        constructor(point: GeoPoint) : this(worldX(point.longitude), worldY(point.latitude))
    }

    private class ProjectedFeature(val source: MapFeature) {
        val points = source.points.map(::WorldPoint)
        val west = points.minOf { it.x }
        val east = points.maxOf { it.x }
        val north = points.minOf { it.y }
        val south = points.maxOf { it.y }
    }

    companion object {
        const val UPHILL = 0xffa73f17.toInt()
        const val DOWNHILL = 0xff176098.toInt()
        const val LEVEL = 0xff286144.toInt()
        const val UNKNOWN = 0xff625e58.toInt()
        const val REFERENCE = 0xff6a3d8c.toInt()

        fun segmentColor(a: HikeSample, b: HikeSample): Int {
            val from = a.point.altitude ?: return UNKNOWN
            val to = b.point.altitude ?: return UNKNOWN
            val accuracy = max(a.verticalAccuracy ?: 0f, b.verticalAccuracy ?: 0f)
            if (accuracy > 25f) return UNKNOWN
            val threshold = max(2.5, accuracy * 0.35)
            return when {
                to - from > threshold -> UPHILL
                from - to > threshold -> DOWNHILL
                else -> LEVEL
            }
        }

        private const val WATER = 0xff397b9a.toInt()
        private const val PEAK = 0xff5d3824.toInt()
        private val DRAW_ORDER = listOf(MapFeatureType.WATER, MapFeatureType.WATERWAY, MapFeatureType.PATH,
            MapFeatureType.LOCAL_ROAD, MapFeatureType.ROAD, MapFeatureType.PEAK)
        private val NAMED_FEATURES = setOf(MapFeatureType.ROAD, MapFeatureType.WATER, MapFeatureType.WATERWAY)
    }
}
