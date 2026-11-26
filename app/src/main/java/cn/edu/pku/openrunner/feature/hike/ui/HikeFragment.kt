package cn.edu.pku.openrunner.feature.hike.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.core.AmapPrivacyController
import cn.edu.pku.openrunner.core.AmapPrivacyStore
import cn.edu.pku.openrunner.feature.hike.data.HikeLocation
import cn.edu.pku.openrunner.feature.hike.data.HikeRecording
import cn.edu.pku.openrunner.feature.hike.data.HikeRecordingService
import cn.edu.pku.openrunner.feature.hike.data.RecordedFix
import cn.edu.pku.openrunner.feature.hike.domain.GeoPoint
import cn.edu.pku.openrunner.feature.hike.domain.HikeSample
import cn.edu.pku.openrunner.feature.hike.domain.MapArea
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

class HikeFragment : Fragment(R.layout.fragment_hike) {
    private val model: HikeViewModel by activityViewModels()
    private lateinit var map: HikeMapView
    private val amapPrivacy by lazy { AmapPrivacyStore(requireContext()) }
    private var amapLayer: HikeAmapLayer? = null
    private var amapUnavailable = false
    private var preview: HikeLocation? = null
    private var previewFix: RecordedFix? = null
    private var previewGps = true
    private var recording = HikeRecording()
    private var pendingAction: LocationAction? = null
    private var starting = false
    private var recenterOnFix = false

    private val kmlPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.importRoute(uri)
    }

    private val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val action = pendingAction
        pendingAction = null
        if (preciseLocationAllowed()) {
            syncPreview()
            when (action) {
                LocationAction.RECORD -> requestRecording()
                LocationAction.CENTER -> recenter()
                else -> Unit
            }
        } else {
            toast(R.string.hike_precise_permission)
            renderGps()
        }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (pendingAction == LocationAction.NOTIFICATION) {
            pendingAction = null
            startRecording()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pendingAction = savedInstanceState?.getString(ACTION_KEY)?.let(LocationAction::valueOf)
        val savedCamera = savedInstanceState?.getDoubleArray(CAMERA_KEY)?.let { HikeCamera(it[0], it[1], it[2]) }
        recording = HikeRecordingService.recording.value
        map = view.findViewById(R.id.hike_map)
        (model.camera ?: savedCamera)?.let { map.setCamera(it, notify = false) }
        map.onCameraChanged = {
            model.camera = map.camera
            val amap = amapLayer
            if (amap == null) model.chooseMap(map.camera) else amap.move(map.camera)
            renderMapStatus()
        }
        map.onSegmentSelected = ::showSegment

        view.findViewById<View>(R.id.hike_zoom_in).setOnClickListener { zoomMap(1) }
        view.findViewById<View>(R.id.hike_zoom_out).setOnClickListener { zoomMap(-1) }
        view.findViewById<View>(R.id.hike_recenter).setOnClickListener {
            withLocationPermission(LocationAction.CENTER, ::recenter)
        }
        view.findViewById<View>(R.id.hike_basemap).setOnClickListener { switchBasemap() }
        view.findViewById<View>(R.id.hike_import).setOnClickListener { kmlPicker.launch(arrayOf("*/*")) }
        view.findViewById<View>(R.id.hike_route_label).setOnClickListener {
            model.page.value.route?.let { route -> MapArea.around(route.points(), 0)?.let(map::fit) }
        }
        view.findViewById<View>(R.id.hike_cache).setOnClickListener {
            if (model.page.value.progress != null) model.cancelDownload() else chooseCacheTarget()
        }
        view.findViewById<View>(R.id.hike_files).setOnClickListener { showFiles() }
        view.findViewById<View>(R.id.hike_record).setOnClickListener {
            if (recording.active) confirmStop() else withLocationPermission(LocationAction.RECORD, ::requestRecording)
        }
        view.findViewById<View>(R.id.hike_history).setOnClickListener { showHistory() }
        view.findViewById<View>(R.id.hike_attribution).setOnClickListener { showAttribution() }
        if (!recording.active && model.page.value.hike == null) model.loadHike()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.page.collect(::renderPage) }
                launch {
                    HikeRecordingService.recording.collect { update ->
                        val previous = recording
                        recording = update
                        if (update.active || update.error) starting = false
                        if (previous.active != update.active) {
                            if (!update.active) model.loadHike()
                            syncPreview()
                        }
                        if (update.error && !previous.error) toast(R.string.hike_record_error)
                        renderRecording()
                        renderGps()
                    }
                }
                launch {
                    while (true) {
                        renderGps()
                        delay(1_500)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        syncPreview()
    }

    override fun onStop() {
        stopPreview()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        amapLayer?.resume()
    }

    override fun onPause() {
        amapLayer?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        amapLayer?.destroy()
        amapLayer = null
        amapUnavailable = false
        map.onCameraChanged = null
        map.onSegmentSelected = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingAction?.let { outState.putString(ACTION_KEY, it.name) }
        if (::map.isInitialized && map.hasCamera) {
            val camera = map.camera
            outState.putDoubleArray(CAMERA_KEY, doubleArrayOf(camera.x, camera.y, camera.zoom))
        }
    }

    private fun syncPreview() {
        if (recording.active || !preciseLocationAllowed()) {
            stopPreview()
            return
        }
        if (preview != null || view == null) return
        preview = HikeLocation(requireContext(), { fix ->
            previewFix = fix
            renderGps()
        }, { enabled ->
            previewGps = enabled
            if (!enabled) previewFix = null
            renderGps()
        }).also { tracker ->
            if (!tracker.start(4_000, Looper.getMainLooper())) {
                previewGps = false
                renderGps()
            }
        }
    }

    private fun stopPreview() {
        preview?.stop()
        preview = null
        previewFix = null
    }

    private fun preciseLocationAllowed(): Boolean = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun withLocationPermission(action: LocationAction, granted: () -> Unit) {
        if (preciseLocationAllowed()) granted() else {
            pendingAction = action
            locationPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun requestRecording() {
        val preferences = requireContext().getSharedPreferences("hike-permissions", 0)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !preferences.getBoolean("notification-asked", false)) {
            preferences.edit().putBoolean("notification-asked", true).apply()
            pendingAction = LocationAction.NOTIFICATION
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (recording.active || starting || !preciseLocationAllowed()) return
        val context = requireContext()
        starting = true
        renderRecording()
        try {
            ContextCompat.startForegroundService(context, Intent(context, HikeRecordingService::class.java))
        } catch (_: RuntimeException) {
            starting = false
            renderRecording()
            toast(R.string.hike_record_error)
        }
    }

    private fun confirmStop() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.hike_stop_confirm)
            .setNegativeButton(R.string.hike_cancel, null)
            .setPositiveButton(R.string.hike_stop) { _, _ ->
                val context = requireContext()
                context.startService(Intent(context, HikeRecordingService::class.java).setAction(HikeRecordingService.ACTION_STOP))
            }.show()
    }

    private fun recenter() {
        val enabled = if (recording.active) recording.gpsEnabled else previewGps
        if (!enabled) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        val fix = currentFix()
        if (fix != null) map.center(fix.point) else {
            recenterOnFix = true
            toast(R.string.hike_waiting)
        }
    }

    private fun currentFix(): RecordedFix? = (if (recording.active) recording.fix else previewFix)
        ?.takeIf { SystemClock.elapsedRealtime() - it.monotonicMs <= 20_000 }

    private fun renderGps() {
        val view = view ?: return
        val fix = currentFix()
        val enabled = if (recording.active) recording.gpsEnabled else previewGps
        if (map.fix != fix) map.fix = fix
        amapLayer?.showFix(fix)
        if (fix != null && (!map.hasCamera || recenterOnFix)) {
            recenterOnFix = false
            map.center(fix.point)
        }
        val status = when {
            !preciseLocationAllowed() -> getString(R.string.hike_precise_permission)
            !enabled -> getString(R.string.hike_gps_off)
            fix == null -> getString(if (recording.active) R.string.hike_recording_waiting else R.string.hike_waiting)
            fix.horizontalAccuracy > (if (recording.active) 50 else 100) -> getString(R.string.hike_waiting_accuracy)
            else -> getString(
                if (recording.active) R.string.hike_gps_fix_recording else R.string.hike_gps_fix,
                fix.horizontalAccuracy.roundToInt(), meters(fix.point.altitude)
            )
        }
        view.findViewById<TextView>(R.id.hike_gps_status).text = status
        view.findViewById<TextView>(R.id.hike_coordinates).apply {
            isVisible = fix != null
            if (fix != null) text = getString(R.string.hike_gps_coordinates, fix.point.latitude, fix.point.longitude)
        }
    }

    private fun renderRecording() {
        val view = view ?: return
        val log = model.page.value.hike
        val active = recording.active
        val samples = if (active) recording.samples else log?.samples.orEmpty()
        map.recording = samples
        amapLayer?.showTrack(samples)
        view.findViewById<Button>(R.id.hike_record).apply {
            isEnabled = !starting
            setText(if (active) R.string.hike_stop else R.string.hike_start)
        }
        view.findViewById<View>(R.id.hike_history).isEnabled = !active
        view.findViewById<TextView>(R.id.hike_stats).text = if (samples.isEmpty()) {
            getString(R.string.hike_track_empty)
        } else {
            val distance = if (active) recording.distance else log?.distance ?: 0.0
            val ascent = if (active) recording.ascent else log?.ascent ?: 0.0
            val measuredAscent = ascent.takeIf { samples.any { it.segmentAscent != null } }
            getString(R.string.hike_track_stats, distance / 1_000, meters(measuredAscent))
        }
        val legend = view.findViewById<TextView>(R.id.hike_legend)
        legend.isVisible = samples.size > 1
        if (legend.isVisible) legend.text = legendText
    }

    private fun renderPage(page: HikePage) {
        val view = view ?: return
        renderBasemap(page.basemap)
        if (map.reference !== page.route) map.reference = page.route
        amapLayer?.showRoute(page.route)
        val offlineMap = if (amapLayer == null) page.map else null
        if (map.map !== offlineMap) map.map = offlineMap
        if (page.fit != null) {
            model.consumeFit(page.fit)
            map.fit(page.fit)
        } else if (!map.hasCamera) {
            val area = page.route?.let { MapArea.around(it.points(), 0) }
                ?: page.hike?.let { MapArea.around(it.samples.asSequence().map { sample -> sample.point }, 0) }
            when {
                area != null -> map.fit(area)
                currentFix() != null -> currentFix()?.let { map.center(it.point) }
                page.maps.isNotEmpty() -> map.fit(page.maps.first().area)
            }
        } else if (amapLayer == null) {
            model.chooseMap(map.camera)
        }
        view.findViewById<TextView>(R.id.hike_route_label).apply {
            isVisible = page.route != null
            text = page.route?.let { getString(R.string.hike_reference_label, it.name) }
        }
        view.findViewById<View>(R.id.hike_attribution).isVisible = offlineMap != null
        view.findViewById<View>(R.id.hike_import).isEnabled = !page.importing
        view.findViewById<Button>(R.id.hike_cache).setText(
            if (page.progress == null) R.string.hike_cache else R.string.hike_cancel_download
        )
        view.findViewById<ProgressBar>(R.id.hike_progress).apply {
            isVisible = page.progress != null
            page.progress?.let { (complete, total) ->
                max = total
                progress = complete
            }
        }
        renderRecording()
        renderGps()
        renderMapStatus()
        page.message?.let {
            toast(it)
            model.consumeMessage(it)
        }
    }

    private fun renderMapStatus() {
        val status = view?.findViewById<TextView>(R.id.hike_map_status) ?: return
        val page = model.page.value
        val progress = page.progress
        val area = page.map?.info?.area
        status.text = when {
            progress != null -> getString(R.string.hike_cache_progress, progress.first, progress.second)
            amapLayer != null -> getString(R.string.hike_basemap_amap)
            area?.contains(map.camera.x, map.camera.y) == true -> getString(R.string.hike_map_available)
            amapUnavailable -> getString(R.string.hike_amap_unavailable)
            else -> getString(R.string.hike_map_missing)
        }
    }

    private fun renderBasemap(mode: HikeBasemap) {
        val root = view ?: return
        if (mode == HikeBasemap.AMAP && amapPrivacy.isAgreed && !amapUnavailable) {
            if (amapLayer == null) {
                val result = runCatching {
                    HikeAmapLayer(
                        root.findViewById<FrameLayout>(R.id.hike_map_container), viewLifecycleOwner.lifecycleScope,
                        { camera ->
                            map.setCamera(camera, notify = false)
                            model.camera = map.camera
                        }, ::showSegment,
                        { name -> context?.let { Toast.makeText(it, name, Toast.LENGTH_SHORT).show() } }
                    )
                }
                amapLayer = result.getOrNull()
                amapUnavailable = amapLayer == null
                amapLayer?.let { layer ->
                    if (isResumed) layer.resume()
                    if (map.hasCamera) layer.move(map.camera)
                }
            }
        } else {
            amapLayer?.destroy()
            amapLayer = null
            if (mode == HikeBasemap.CONTOURS) amapUnavailable = false
        }
        val next = if (amapLayer == null) R.string.hike_basemap_amap else R.string.hike_basemap_contours
        // Keep the offline view measured so the shared viewport can still fit a route.
        map.visibility = if (amapLayer == null) View.VISIBLE else View.INVISIBLE
        root.findViewById<Button>(R.id.hike_basemap).apply {
            setText(next)
            contentDescription = getString(R.string.hike_switch_basemap, getString(next))
        }
    }

    private fun switchBasemap() {
        if (amapLayer != null) {
            model.setBasemap(HikeBasemap.CONTOURS)
        } else if (amapPrivacy.isAgreed) {
            amapUnavailable = false
            model.setBasemap(HikeBasemap.AMAP)
            renderPage(model.page.value)
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.amap_privacy_title)
                .setMessage(R.string.amap_privacy_message)
                .setNegativeButton(R.string.amap_privacy_decline, null)
                .setPositiveButton(R.string.amap_privacy_accept) { _, _ ->
                    AmapPrivacyController.recordDecision(requireContext(), agreed = true)
                    model.setBasemap(HikeBasemap.AMAP)
                    renderPage(model.page.value)
                }
                .show()
        }
    }

    private fun zoomMap(amount: Int) {
        val amap = amapLayer
        if (amap == null) map.zoomBy(amount.toDouble()) else amap.zoomBy(amount.toFloat())
    }

    private fun showSegment(sample: HikeSample) {
        val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(sample.timestamp))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.hike_segment_title)
            .setMessage(getString(R.string.hike_segment_details, time, meters(sample.point.altitude), meters(sample.segmentAscent)))
            .setPositiveButton(R.string.hike_close, null)
            .show()
    }

    private fun chooseCacheTarget() {
        val route = model.page.value.route
        val location = currentFix()?.takeIf { it.horizontalAccuracy <= 100 }?.point
        when {
            route == null && location == null -> toast(R.string.hike_cache_location_needed)
            route != null && location != null -> MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hike_cache)
                .setItems(arrayOf(getString(R.string.hike_cache_route), getString(R.string.hike_cache_here))) { _, index ->
                    if (index == 0) chooseDistance(route.name, route::points)
                    else chooseDistance(getString(R.string.hike_cache_here)) { sequenceOf(location) }
                }.show()
            route != null -> chooseDistance(route.name, route::points)
            location != null -> chooseDistance(getString(R.string.hike_cache_here)) { sequenceOf(location) }
        }
    }

    private fun chooseDistance(name: String, points: () -> Sequence<GeoPoint>) {
        val options = listOf(2, 5, 10)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.hike_cache_radius)
            .setItems(options.map { getString(R.string.hike_cache_distance, it) }.toTypedArray()) { _, index ->
                val radius = options[index]
                val area = MapArea.around(points(), radius)
                if (area == null) toast(R.string.hike_area_too_large)
                else model.downloadMap(area, getString(R.string.hike_cache_label, name, radius))
            }.show()
    }

    private fun showFiles() {
        val page = model.page.value
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val files = buildList<Pair<String, () -> Unit>> {
            for (route in page.routes.entries) {
                val active = route.id == page.routes.activeId && page.route != null
                val title = if (active) getString(R.string.hike_file_active, route.title) else route.title
                val label = getString(R.string.hike_file_route, title, route.fileName, format.format(Date(route.importedAt)))
                add(label to {
                    showFileActions(route.title, hide = active, { model.selectRoute(if (active) null else route.id) }) {
                        model.deleteRoute(route.id)
                    }
                })
            }
            for (item in page.maps) {
                val label = getString(R.string.hike_file_map, item.label).let {
                    if (page.basemap == HikeBasemap.CONTOURS && page.map?.info?.key == item.key) {
                        getString(R.string.hike_file_active, it)
                    } else it
                }
                add(label to {
                    showFileActions(item.label, hide = false, { model.openOfflineMap(item) }) { model.deleteMap(item) }
                })
            }
        }
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(R.string.hike_files)
            if (files.isEmpty()) setMessage(R.string.hike_files_empty)
            else setItems(files.map { it.first }.toTypedArray()) { _, index -> files[index].second() }
            setNeutralButton(R.string.hike_import) { _, _ -> kmlPicker.launch(arrayOf("*/*")) }
            setNegativeButton(R.string.hike_close, null)
        }.show()
    }

    private fun showFileActions(title: String, hide: Boolean, display: () -> Unit, delete: () -> Unit) {
        val actions = arrayOf(getString(if (hide) R.string.hike_file_hide else R.string.hike_file_show),
            getString(R.string.hike_file_delete))
        MaterialAlertDialogBuilder(requireContext()).setTitle(title)
            .setItems(actions) { _, selected ->
                if (selected == 0) display()
                else MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.hike_file_delete_confirm, title))
                    .setNegativeButton(R.string.hike_cancel, null)
                    .setPositiveButton(R.string.hike_file_delete) { _, _ -> delete() }
                    .show()
            }
            .setNegativeButton(R.string.hike_close, null)
            .show()
    }

    private fun showHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            val hikes = runCatching { model.hikes() }.getOrElse {
                toast(R.string.hike_record_error)
                return@launch
            }
            if (hikes.isEmpty()) {
                toast(R.string.hike_history_empty)
                return@launch
            }
            val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            val names = hikes.map { format.format(Date(it.started)) }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hike_history)
                .setItems(names) { _, index ->
                    val hike = hikes[index]
                    model.loadHike(hike, fit = true)
                    if (hike.finished == null) toast(R.string.hike_track_interrupted)
                }.show()
        }
    }

    private fun showAttribution() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.hike_attribution)
            .setNeutralButton(R.string.hike_close, null)
            .setPositiveButton(R.string.hike_attribution_osm) { _, _ -> openLink("https://www.openstreetmap.org/copyright") }
            .setNegativeButton(R.string.hike_attribution_terrain) { _, _ ->
                openLink("https://github.com/tilezen/joerd/blob/master/docs/attribution.md")
            }.show()
    }

    private fun openLink(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private val legendText by lazy {
        SpannableStringBuilder().apply {
            val items = listOf(
                R.string.hike_direction_up to HikeMapView.UPHILL,
                R.string.hike_direction_down to HikeMapView.DOWNHILL,
                R.string.hike_direction_level to HikeMapView.LEVEL,
                R.string.hike_direction_unknown to HikeMapView.UNKNOWN
            )
            for ((label, color) in items) {
                if (length > 0) append("    ")
                val start = length
                append(getString(label))
                setSpan(ForegroundColorSpan(color), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun meters(value: Double?): String = value?.let { getString(R.string.hike_scale_m, it.roundToInt()) }
        ?: getString(R.string.hike_value_missing)

    private fun toast(message: Int) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    private enum class LocationAction { CENTER, RECORD, NOTIFICATION }

    companion object {
        private const val ACTION_KEY = "hike_permission_action"
        private const val CAMERA_KEY = "hike_camera"
    }
}
