package cn.edu.pku.openrunner.feature.hike.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import cn.edu.pku.openrunner.feature.hike.data.RecordedFix
import cn.edu.pku.openrunner.ui.MapLocationMarker
import cn.edu.pku.openrunner.feature.hike.domain.GeoPoint
import cn.edu.pku.openrunner.feature.hike.domain.HikeRoute
import cn.edu.pku.openrunner.feature.hike.domain.HikeSample
import cn.edu.pku.openrunner.feature.hike.domain.latitudeAt
import cn.edu.pku.openrunner.feature.hike.domain.longitudeAt
import cn.edu.pku.openrunner.feature.hike.domain.worldX
import cn.edu.pku.openrunner.feature.hike.domain.worldY
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.CoordinateConverter
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MultiPointItem
import com.amap.api.maps.model.MultiPointOverlay
import com.amap.api.maps.model.MultiPointOverlayOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot

internal class HikeAmapLayer(
    private val container: FrameLayout,
    private val scope: CoroutineScope,
    private val cameraChanged: (HikeCamera) -> Unit,
    segmentSelected: (HikeSample) -> Unit,
    waypointSelected: (String) -> Unit
) {
    private val context = container.context.applicationContext
    private val converter = converter(context)
    private val density = container.resources.displayMetrics.density
    private val view = TextureMapView(container.context)
    private val map: AMap
    private var resumed = false
    private var gestureMoved = false
    private var route: HikeRoute? = null
    private var routeJob: Job? = null
    private var trackJob: Job? = null
    private var waypoints: MultiPointOverlay? = null
    private val routeLines = mutableListOf<Polyline>()
    private val trackLines = mutableListOf<Polyline>()
    private var samples: List<HikeSample> = emptyList()
    private var displayedTrack: List<HikeSample> = emptyList()
    private var trackPoints = emptyList<LatLng>()
    private var fix: RecordedFix? = null
    private val locationMarker: MapLocationMarker
    private var accuracy: Circle? = null
    private val routeIcon by lazy { dotIcon(HikeMapView.REFERENCE, 11f) }

    init {
        try {
            view.onCreate(null)
            map = requireNotNull(view.map)
            locationMarker = MapLocationMarker(container.context, map)
            map.mapType = if (container.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL
            map.uiSettings.apply {
                isCompassEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isZoomControlsEnabled = false
                isScaleControlsEnabled = true
            }
            map.setOnMapTouchListener { if (it.actionMasked == MotionEvent.ACTION_MOVE ||
                it.actionMasked == MotionEvent.ACTION_DOWN) gestureMoved = true }
            map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                override fun onCameraChange(position: CameraPosition) = Unit

                override fun onCameraChangeFinish(position: CameraPosition) {
                    if (!gestureMoved) return
                    gestureMoved = false
                    val center = toWgs84(position.target)
                    cameraChanged(HikeCamera(worldX(center.longitude), worldY(center.latitude), position.zoom.toDouble()))
                }
            })
            map.setOnMapClickListener { point -> nearestSegment(point)?.let(segmentSelected) }
            map.setOnMultiPointClickListener { item ->
                item.title?.takeIf(String::isNotBlank)?.let(waypointSelected)
                true
            }
            container.addView(view, 0, FrameLayout.LayoutParams(-1, -1))
        } catch (error: Exception) {
            runCatching { view.onDestroy() }
            throw error
        } catch (error: LinkageError) {
            runCatching { view.onDestroy() }
            throw error
        }
    }

    fun resume() {
        if (!resumed) view.onResume()
        resumed = true
    }

    fun pause() {
        if (resumed) view.onPause()
        resumed = false
    }

    fun destroy() {
        routeJob?.cancel()
        trackJob?.cancel()
        try {
            pause()
            view.onDestroy()
        } finally {
            container.removeView(view)
        }
    }

    fun move(camera: HikeCamera) {
        gestureMoved = false
        val point = GeoPoint(latitudeAt(camera.y), longitudeAt(camera.x))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(toMapPoint(converter, point), camera.zoom.toFloat()))
    }

    fun zoomBy(amount: Float) {
        gestureMoved = true
        map.moveCamera(CameraUpdateFactory.zoomBy(amount))
    }

    fun showRoute(value: HikeRoute?) {
        if (route === value) return
        route = value
        routeJob?.cancel()
        routeLines.forEach(Polyline::remove)
        routeLines.clear()
        waypoints?.remove()
        waypoints = null
        if (value == null) return

        routeJob = scope.launch {
            try {
                val (lines, items) = withContext(Dispatchers.Default) {
                    val projector = converter(context)
                    val lines = value.lines.map { line -> line.map {
                        ensureActive()
                        toMapPoint(projector, it)
                    } }
                    val items = value.waypoints.map {
                        ensureActive()
                        MultiPointItem(toMapPoint(projector, it.point)).apply { title = it.name }
                    }
                    lines to items
                }
                lines.filter { it.size > 1 }.forEach { line ->
                    routeLines.add(addLine(line, Color.WHITE, 5f, 1f))
                    routeLines.add(addLine(line, HikeMapView.REFERENCE, 2.7f, 2f))
                }
                if (items.isNotEmpty()) {
                    waypoints = map.addMultiPointOverlay(MultiPointOverlayOptions().icon(routeIcon).anchor(0.5f, 0.5f))
                        .apply { setItems(items) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: RuntimeException) {
                Log.w(TAG, "Unable to draw reference route", error)
            }
        }
    }

    fun showTrack(value: List<HikeSample>) {
        if (samples === value) return
        samples = value
        trackJob?.cancel()
        trackJob = scope.launch {
            try {
                // Recorded samples are append-only. Reuse the transformed prefix as new fixes arrive.
                val oldSamples = displayedTrack
                val prefix = trackPoints.takeIf { value.size >= oldSamples.size &&
                    value.take(oldSamples.size) == oldSamples }.orEmpty()
                val points = withContext(Dispatchers.Default) {
                    val projector = converter(context)
                    prefix + value.drop(prefix.size).map {
                        ensureActive()
                        toMapPoint(projector, it.point)
                    }
                }
                trackLines.forEach(Polyline::remove)
                trackLines.clear()
                displayedTrack = value
                trackPoints = points
                var start = 0
                while (start < value.size) {
                    var end = start + 1
                    while (end < value.size && value[end].section == value[start].section) end++
                    if (end - start > 1) {
                        val line = points.subList(start, end)
                        val colors = List(end - start - 1) { index ->
                            HikeMapView.segmentColor(value[start + index], value[start + index + 1])
                        }
                        trackLines.add(addLine(line, Color.WHITE, 6f, 3f))
                        trackLines.add(map.addPolyline(PolylineOptions().addAll(line).width(3.6f * density)
                            .colorValues(colors + colors.last()).zIndex(4f)))
                    }
                    start = end
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: RuntimeException) {
                Log.w(TAG, "Unable to draw recorded track", error)
            }
        }
    }

    fun showFix(value: RecordedFix?) {
        if (fix == value) return
        fix = value
        if (value == null) {
            locationMarker.clear()
            accuracy?.remove()
            accuracy = null
            return
        }
        val position = toMapPoint(converter, value.point)
        val radius = value.horizontalAccuracy.toDouble()
        locationMarker.moveTo(position)
        if (accuracy == null) {
            accuracy = map.addCircle(CircleOptions().center(position).radius(radius)
                .strokeWidth(0f).fillColor(0x303b72c6).zIndex(0f))
        } else {
            accuracy?.center = position
            accuracy?.radius = radius
        }
    }

    fun showHeading(heading: Int?) = locationMarker.updateHeading(heading)

    private fun nearestSegment(point: LatLng): HikeSample? {
        val projection = map.projection
        val tap = projection.toScreenLocation(point)
        var distance = 18f * density
        var result: HikeSample? = null
        for (index in 1 until trackPoints.size) {
            val sample = displayedTrack[index]
            if (sample.section != displayedTrack[index - 1].section) continue
            val start = projection.toScreenLocation(trackPoints[index - 1])
            val end = projection.toScreenLocation(trackPoints[index])
            val dx = (end.x - start.x).toFloat()
            val dy = (end.y - start.y).toFloat()
            val squared = dx * dx + dy * dy
            if (squared == 0f) continue
            val offsetX = tap.x - start.x
            val offsetY = tap.y - start.y
            val position = ((offsetX * dx + offsetY * dy) / squared).coerceIn(0f, 1f)
            val candidate = hypot(offsetX - position * dx, offsetY - position * dy)
            if (candidate < distance) {
                result = sample
                distance = candidate
            }
        }
        return result
    }

    private fun addLine(points: List<LatLng>, color: Int, width: Float, z: Float): Polyline =
        map.addPolyline(PolylineOptions().addAll(points).width(width * density).color(color).zIndex(z))

    private fun dotIcon(color: Int, diameter: Float) = BitmapDescriptorFactory.fromBitmap(
        Bitmap.createBitmap((diameter * density).toInt(), (diameter * density).toInt(), Bitmap.Config.ARGB_8888).apply {
            val center = width / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE }
            val canvas = Canvas(this)
            canvas.drawCircle(center, center, center, paint)
            paint.color = color
            canvas.drawCircle(center, center, center - density * 1.5f, paint)
        }
    )

    private fun toWgs84(target: LatLng): GeoPoint {
        var point = GeoPoint(target.latitude, target.longitude)
        // Invert the SDK's WGS84-to-map offset at the camera center.
        repeat(3) {
            val mapped = toMapPoint(converter, point)
            point = GeoPoint((point.latitude + target.latitude - mapped.latitude).coerceIn(-90.0, 90.0),
                (point.longitude + target.longitude - mapped.longitude).coerceIn(-180.0, 180.0))
        }
        return point
    }

    private companion object {
        const val TAG = "HikeAmapLayer"

        fun converter(context: Context) = CoordinateConverter(context).from(CoordinateConverter.CoordType.GPS)

        fun toMapPoint(converter: CoordinateConverter, point: GeoPoint): LatLng {
            val position = LatLng(point.latitude, point.longitude)
            return converter.coord(position).convert() ?: position
        }
    }
}
