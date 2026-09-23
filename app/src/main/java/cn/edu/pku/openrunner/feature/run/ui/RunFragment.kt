package cn.edu.pku.openrunner.feature.run.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
                        pauseResume.visibility = if (
                            state.status == RunStatus.RUNNING || state.status == RunStatus.PAUSED
                        ) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                        pauseResume.setText(
                            if (state.status == RunStatus.PAUSED) {
                                R.string.run_resume
                            } else {
                                R.string.run_pause
                            }
                        )
                        pauseResume.isEnabled = !state.isSavingRecord
                        delete.visibility = if (state.hasPendingRecord) View.VISIBLE else View.GONE
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
                        recordStatusText.visibility = if (recordStatusText.text.isEmpty()) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
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
    }
}
