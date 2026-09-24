package cn.edu.pku.openrunner.feature.run.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.run.domain.RunMetrics
import cn.edu.pku.openrunner.feature.run.domain.TrackPoint
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import kotlinx.coroutines.launch
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RunFragment : Fragment() {
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasLocationPermission()) viewModel.startLocating()
    }

    private val viewModel: RunViewModel by activityViewModels {
        RunViewModel.Factory.from(requireContext())
    }

    private var mapView: MapView? = null
    private var aMap: AMap? = null
    private var routePolyline: Polyline? = null
    private var currentMarker: Marker? = null
    private var accuracyCircle: Circle? = null
    private var followLocation = true
    private var stopConfirmation: AlertDialog? = null
    private var pauseConfirmation: AlertDialog? = null

    /** 控件显隐用的缓动，只建一次，别在每帧的状态回调里新建。 */
    private val controlFadeInterpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_run, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val map = view.findViewById<MapView>(R.id.run_map)
        mapView = map
        map.onCreate(savedInstanceState)
        aMap = map.map.apply {
            mapType = if (isNightMode()) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isCompassEnabled = false
            uiSettings.isScaleControlsEnabled = true
            moveCamera(CameraUpdateFactory.newLatLngZoom(CAMPUS_CENTER, 15.5f))
            setOnMapTouchListener { followLocation = false }
        }

        val statusText = view.findViewById<TextView>(R.id.run_status)
        val distanceText = view.findViewById<TextView>(R.id.run_distance)
        val durationText = view.findViewById<TextView>(R.id.run_duration)
        val paceText = view.findViewById<TextView>(R.id.run_pace)
        val recordStatusText = view.findViewById<TextView>(R.id.run_record_status)
        val pauseResume = view.findViewById<Button>(R.id.run_pause_resume)
        val action = view.findViewById<Button>(R.id.run_action)
        val delete = view.findViewById<Button>(R.id.run_delete)
        view.findViewById<View>(R.id.run_recenter).setOnClickListener {
            followLocation = true
            viewModel.uiState.value.currentPoint?.let { renderCurrentLocation(it, null, true) }
        }
        pauseResume.setOnClickListener {
            when (viewModel.uiState.value.status) {
                RunStatus.RUNNING -> confirmPause()
                RunStatus.PAUSED -> viewModel.resume()
                else -> Unit
            }
        }
        action.setOnClickListener {
            when (viewModel.uiState.value.primaryAction) {
                RunPrimaryAction.STOP -> confirmStop()
                RunPrimaryAction.SAVE -> viewModel.saveFinishedRecord()
                RunPrimaryAction.START -> if (
                    viewModel.uiState.value.virtualLocationEnabled || hasLocationPermission()
                ) {
                    followLocation = true
                    viewModel.start()
                } else {
                    requestLocationPermission()
                }
            }
        }
        delete.setOnClickListener { viewModel.discardFinishedRecord() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        statusText.text = when (state.status) {
                            RunStatus.IDLE -> if (state.currentPoint == null) {
                                getString(if (state.virtualLocationEnabled) {
                                    R.string.run_virtual_waiting
                                } else {
                                    R.string.run_locating
                                })
                            } else {
                                getString(R.string.run_location_ready, state.accuracyMeters ?: 0)
                            }
                            RunStatus.RUNNING -> getString(
                                if (state.virtualLocationEnabled) {
                                    R.string.run_running_virtual
                                } else if (state.backgroundTrackingActive) {
                                    R.string.run_running_background
                                } else {
                                    R.string.run_running_foreground_only
                                },
                                state.stepCount
                            )
                            RunStatus.PAUSED -> getString(
                                R.string.run_paused,
                                state.stepCount
                            )
                            RunStatus.FINISHED -> getString(
                                R.string.run_finished,
                                state.stepCount
                            )
                            RunStatus.PERMISSION_REQUIRED -> getString(R.string.run_permission_required)
                        }
                        if (state.usedVirtualLocation && state.status != RunStatus.IDLE) {
                            statusText.text = getString(R.string.run_virtual_status, statusText.text)
                        }
                        action.setText(
                            when (state.primaryAction) {
                                RunPrimaryAction.START -> R.string.run_start
                                RunPrimaryAction.STOP -> R.string.run_stop
                                RunPrimaryAction.SAVE -> R.string.run_save
                            }
                        )
                        action.isEnabled = !state.isSavingRecord
                        crossFade(
                            pauseResume,
                            state.status == RunStatus.RUNNING || state.status == RunStatus.PAUSED
                        )
                        pauseResume.setText(
                            if (state.status == RunStatus.PAUSED) {
                                R.string.run_resume
                            } else {
                                R.string.run_pause
                            }
                        )
                        pauseResume.isEnabled = !state.isSavingRecord
                        crossFade(delete, state.hasPendingRecord)
                        delete.isEnabled = !state.isSavingRecord
                        distanceText.text = getString(R.string.run_metric_distance_value, state.distanceMeters)
                        durationText.text = RunMetrics.formatDuration(state.durationSeconds)
                        paceText.text = RunMetrics.formatPace(state.paceSecondsPerKm)
                        recordStatusText.text = when (state.recordSaveStatus) {
                            RecordSaveStatus.NONE -> if (state.hasPendingRecord) {
                                getString(R.string.run_record_waiting)
                            } else {
                                ""
                            }
                            RecordSaveStatus.SAVING -> getString(R.string.run_record_saving)
                            RecordSaveStatus.SAVED -> getString(
                                if (state.usedVirtualLocation) {
                                    R.string.run_record_saved_virtual
                                } else {
                                    R.string.run_record_saved
                                }
                            )
                            RecordSaveStatus.ERROR -> getString(
                                R.string.run_record_error,
                                state.recordError ?: getString(R.string.unknown_error)
                            )
                        }
                        crossFade(recordStatusText, recordStatusText.text.isNotEmpty())
                        // 该文本位于跑步页浮层上。浮层底色随亮暗主题切换
                        // （浅色为 #FFFDF8，深色为墨色 #221C16），
                        // 所以直接用随主题解析的状态色即可，两边都有足够对比度。
                        recordStatusText.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                when (state.recordSaveStatus) {
                                    RecordSaveStatus.SAVED -> if (state.usedVirtualLocation) {
                                        R.color.or_status_virtual
                                    } else {
                                        R.color.or_status_success
                                    }
                                    RecordSaveStatus.ERROR -> R.color.or_status_error
                                    RecordSaveStatus.SAVING -> R.color.or_status_uploading
                                    else -> R.color.or_status_pending
                                }
                            )
                        )
                        state.currentPoint?.let {
                            renderCurrentLocation(it, state.accuracyMeters, false)
                        }
                        renderTrack(state.points, state.usedVirtualLocation)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is RunUiEvent.Message -> Toast.makeText(
                                requireContext(),
                                event.text,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        if (viewModel.uiState.value.virtualLocationEnabled || hasLocationPermission()) {
            viewModel.startLocating()
        } else {
            requestLocationPermission()
        }
    }

    private fun confirmStop() {
        if (stopConfirmation != null || pauseConfirmation != null ||
            viewModel.uiState.value.status !in setOf(RunStatus.RUNNING, RunStatus.PAUSED)
        ) return
        val isPaused = viewModel.uiState.value.status == RunStatus.PAUSED
        stopConfirmation = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.run_stop_confirm_title)
            .setMessage(R.string.run_stop_confirm_message)
            .setPositiveButton(R.string.run_stop_confirm_yes) { _, _ -> viewModel.stop() }
            .setNegativeButton(
                if (isPaused) R.string.run_stop_confirm_keep_paused else R.string.run_stop_confirm_no,
                null
            )
            .setOnDismissListener { stopConfirmation = null }
            .show()
    }

    private fun confirmPause() {
        if (pauseConfirmation != null || stopConfirmation != null ||
            viewModel.uiState.value.status != RunStatus.RUNNING
        ) return
        pauseConfirmation = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.run_pause_confirm_title)
            .setMessage(R.string.run_pause_confirm_message)
            .setPositiveButton(R.string.run_pause_confirm_yes) { _, _ -> viewModel.pause() }
            .setNegativeButton(R.string.run_pause_confirm_no, null)
            .setOnDismissListener { pauseConfirmation = null }
            .show()
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
        )
    }

    private fun renderCurrentLocation(
        point: TrackPoint,
        accuracyMeters: Int?,
        forceCenter: Boolean
    ) {
        val map = aMap ?: return
        val position = point.toMapLatLng()
        val marker = currentMarker
        if (marker == null) {
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_location_point))
            )
        } else {
            marker.position = position
        }
        accuracyCircle?.remove()
        accuracyCircle = accuracyMeters?.takeIf { it > 0 }?.let { accuracy ->
            val routeColor = ContextCompat.getColor(requireContext(), R.color.map_route)
            map.addCircle(
                CircleOptions()
                    .center(position)
                    .radius(accuracy.toDouble())
                    .strokeWidth(2f)
                    .strokeColor(ColorUtils.setAlphaComponent(routeColor, 150))
                    .fillColor(ColorUtils.setAlphaComponent(routeColor, 35))
            )
        }
        if (forceCenter || followLocation) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 17.5f))
        }
    }

    private fun renderTrack(points: List<TrackPoint>, virtual: Boolean) {
        val map = aMap ?: return
        val latLngs = points.map { it.toMapLatLng() }
        routePolyline?.remove()
        routePolyline = if (latLngs.size >= 2) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .color(ContextCompat.getColor(requireContext(),
                        if (virtual) R.color.or_status_virtual else R.color.map_route))
                    .width(10f)
            )
        } else {
            null
        }
    }

    /**
     * 控件的显隐交叉淡入。
     *
     * 直接切 visibility 会让按钮在同一位置「啪」地出现／消失；跑步页上暂停、
     * 保存、丢弃三个按钮彼此牵连，硬切读起来像布局跳了一下。这里只动 alpha：
     * 显示时先置 VISIBLE 再淡入，隐藏时淡完才置 GONE，中间不参与测量，
     * 不会额外引起重排（GONE 本身带来的回流是本来就有的）。
     *
     * 用 view.animate() 而不是自建 Animator：它每个 View 一份，重复调用会打断
     * 上一条，状态反复横跳时不会留下两条动画互相打架。
     *
     * 「应该是什么可见性」记在 view.tag 上。淡出被打断时，end action 仍可能
     * 已经排上队并被执行，那时若不判断就会把一个正在淡入的控件又置成 GONE。
     * tag 在布局与代码里都没被用过，借它存这个意图；tag 为空说明从未调用过本方法，
     * 按「该显示」处理，别误置 GONE。
     */
    private fun crossFade(view: View, visible: Boolean) {
        view.tag = visible
        val target = if (visible) 1f else 0f
        val showing = view.visibility == View.VISIBLE
        if (showing && view.alpha == target) return
        if (visible) {
            if (!showing) {
                view.alpha = 0f
                view.visibility = View.VISIBLE
            }
            view.animate()
                .alpha(1f)
                .setDuration(CONTROL_FADE_MILLIS)
                .setInterpolator(controlFadeInterpolator)
                .start()
        } else {
            if (!showing) return
            view.animate()
                .alpha(0f)
                .setDuration(CONTROL_FADE_MILLIS)
                .setInterpolator(controlFadeInterpolator)
                .withEndAction {
                    if (view.tag as? Boolean != true) view.visibility = View.GONE
                }
                .start()
        }
    }

    private fun TrackPoint.toMapLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isNightMode(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        stopConfirmation?.dismiss()
        stopConfirmation = null
        pauseConfirmation?.dismiss()
        pauseConfirmation = null
        viewModel.stopLocatingIfIdle()
        routePolyline?.remove()
        currentMarker?.remove()
        accuracyCircle?.remove()
        routePolyline = null
        currentMarker = null
        accuracyCircle = null
        mapView?.onDestroy()
        mapView = null
        aMap = null
        super.onDestroyView()
    }

    companion object {
        private val CAMPUS_CENTER = LatLng(39.99281, 116.31088)

        /** 控件显隐的淡入淡出时长。够短，不会被读成「页面在加载」。 */
        private const val CONTROL_FADE_MILLIS = 150L
    }
}
