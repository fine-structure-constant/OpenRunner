package cn.edu.pku.openrunner.feature.records.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.core.AmapPrivacyStore
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecord
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecordStore
import cn.edu.pku.openrunner.feature.run.domain.RunChartData
import cn.edu.pku.openrunner.feature.run.domain.RunMetrics
import com.google.android.material.appbar.MaterialToolbar
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import java.text.DateFormat
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordDetailActivity : AppCompatActivity(R.layout.activity_record_detail) {
    private var routeMap: TextureMapView? = null
    private var savedMapState: Bundle? = null
    private var mapResumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedMapState = savedInstanceState?.getBundle(KEY_MAP_STATE)
        findViewById<MaterialToolbar>(R.id.record_detail_toolbar)
            .setNavigationOnClickListener { finish() }
        val localId = intent.getStringExtra(EXTRA_LOCAL_ID)
        if (localId.isNullOrBlank()) {
            finishMissingRecord()
            return
        }
        lifecycleScope.launch {
            val record = withContext(Dispatchers.IO) {
                LocalRunRecordStore(this@RecordDetailActivity).find(localId)
            }
            if (record == null || !record.hasLocalDetails) {
                finishMissingRecord()
            } else {
                render(record)
            }
        }
    }

    private fun render(record: LocalRunRecord) {
        findViewById<View>(R.id.record_detail_progress).visibility = View.GONE
        findViewById<View>(R.id.record_detail_content).visibility = View.VISIBLE
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
        val samples = record.metricSamples.orEmpty()
        // One normalisation pass feeds both charts and the headline pace. Deriving the
        // three of them separately re-sorted the samples three times and rebuilt the
        // pace series twice, and this runs on the main thread.
        val series = RunChartData.series(samples)
        val fastestPace = series.fastestPaceMinutesPerKm
        findViewById<TextView>(R.id.record_detail_source).visibility =
            if (record.usedVirtualLocation) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.record_detail_start).text = getString(
            R.string.record_detail_start,
            dateFormat.format(record.startedAtMillis)
        )
        findViewById<TextView>(R.id.record_detail_end).text = getString(
            R.string.record_detail_end,
            dateFormat.format(record.completedAtMillis)
        )
        findViewById<TextView>(R.id.record_detail_distance).text = getString(
            R.string.record_detail_distance,
            record.distanceMeters / 1_000.0
        )
        findViewById<TextView>(R.id.record_detail_duration).text = getString(
            R.string.record_detail_duration,
            RunMetrics.formatDuration(record.durationSeconds)
        )
        findViewById<TextView>(R.id.record_detail_steps).text = getString(
            R.string.record_detail_steps,
            record.steps
        )
        findViewById<TextView>(R.id.record_detail_fastest_pace).text = getString(
            R.string.record_detail_fastest_pace,
            RunMetrics.formatPace(fastestPace?.times(60)?.roundToInt())
        )
        findViewById<TextView>(R.id.record_detail_samples).text = getString(
            R.string.record_detail_samples,
            samples.size
        )
        findViewById<RunLineChartView>(R.id.record_distance_chart).setData(
            series.distanceKilometres,
            RunChartType.DISTANCE
        )
        findViewById<RunLineChartView>(R.id.record_pace_chart).setData(
            series.paceMinutesPerKm,
            RunChartType.PACE
        )
        val chartsVisible = if (samples.size >= 2) View.VISIBLE else View.GONE
        findViewById<View>(R.id.record_distance_chart_card).visibility = chartsVisible
        findViewById<View>(R.id.record_pace_chart_card).visibility = chartsVisible
        renderRoute(record)
    }

    private fun renderRoute(record: LocalRunRecord) {
        val placeholder = findViewById<TextView>(R.id.record_route_placeholder)
        val positions = record.track.orEmpty().filter { it.isValidCoordinate }
            .map { LatLng(it.latitude, it.longitude) }
        if (positions.isEmpty()) {
            placeholder.setText(R.string.record_route_empty)
            return
        }
        // Opening historical local records must not bypass the map privacy decision.
        if (!AmapPrivacyStore(this).isAgreed) {
            placeholder.setText(R.string.record_route_privacy)
            return
        }
        val container = findViewById<FrameLayout>(R.id.record_route_container)
        // Texture rendering follows the scroll view's transforms and clipping in the same frame.
        // A Surface-based MapView uses a separate surface that can lag behind the scrolling card.
        val widget = TextureMapView(this)
        routeMap = widget
        container.addView(widget, 0, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        widget.onCreate(savedMapState)
        if (mapResumed) widget.onResume()
        placeholder.setText(R.string.record_route_loading)
        val map = widget.map
        map.mapType = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        ) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isScaleControlsEnabled = true
        map.setOnMapTouchListener { event ->
            container.parent.requestDisallowInterceptTouchEvent(
                event.action != MotionEvent.ACTION_UP && event.action != MotionEvent.ACTION_CANCEL
            )
        }
        if (positions.size >= 2) {
            map.addPolyline(PolylineOptions().addAll(positions).width(8f).color(
                ContextCompat.getColor(this,
                    if (record.usedVirtualLocation) R.color.or_status_virtual else R.color.map_route)
            ))
        }
        map.addMarker(MarkerOptions().position(positions.first())
            .title(getString(R.string.record_route_start))
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        if (positions.size >= 2) {
            map.addMarker(MarkerOptions().position(positions.last())
                .title(getString(R.string.record_route_end))
                .anchor(0.5f, if (positions.last() == positions.first()) 0f else 1f)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        }
        map.setOnMapLoadedListener {
            if (positions.distinct().size == 1) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(positions.first(), 17f))
            } else {
                val bounds = LatLngBounds.Builder()
                positions.forEach { bounds.include(it) }
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(
                    bounds.build(), (40 * resources.displayMetrics.density).roundToInt()
                ))
            }
            placeholder.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        mapResumed = true
        routeMap?.onResume()
    }

    override fun onPause() {
        mapResumed = false
        routeMap?.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val mapState = Bundle()
        routeMap?.onSaveInstanceState(mapState)
        outState.putBundle(KEY_MAP_STATE, mapState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        routeMap?.onDestroy()
        routeMap = null
        super.onDestroy()
    }

    private fun finishMissingRecord() {
        Toast.makeText(this, R.string.record_detail_missing, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private const val EXTRA_LOCAL_ID = "record_local_id"
        private const val KEY_MAP_STATE = "record_route_map_state"

        fun intent(context: Context, localId: String): Intent =
            Intent(context, RecordDetailActivity::class.java)
                .putExtra(EXTRA_LOCAL_ID, localId)
    }
}
