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
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.run.data.AndroidLocationTracker
import cn.edu.pku.openrunner.feature.run.data.AccelerometerStepCounter
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecordStore
import cn.edu.pku.openrunner.feature.run.data.RunRecordRepository
import cn.edu.pku.openrunner.feature.run.data.RunPhotoStore
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
import cn.edu.pku.openrunner.feature.records.ui.recordIssueText

class RunFragment : Fragment() {
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasLocationPermission()) viewModel.startLocating()
    }
    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::attachPhoto)
    }

    private val viewModel: RunViewModel by activityViewModels {
        RunViewModel.Factory(
            AndroidLocationTracker(requireContext()),
            AccelerometerStepCounter(requireContext()),
            RunRecordRepository(
                SessionStore(requireContext()),
                LocalRunRecordStore(requireContext()),
                RunPhotoStore(requireContext())
            )
        )
    }

    private var mapView: MapView? = null
    private var aMap: AMap? = null
    private var routePolyline: Polyline? = null
    private var currentMarker: Marker? = null
    private var accuracyCircle: Circle? = null
    private var followLocation = true

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
        val action = view.findViewById<Button>(R.id.run_action)
        val upload = view.findViewById<Button>(R.id.run_upload)
        val photo = view.findViewById<Button>(R.id.run_photo)
        view.findViewById<View>(R.id.run_recenter).setOnClickListener {
            followLocation = true
            viewModel.uiState.value.currentPoint?.let { renderCurrentLocation(it, null, true) }
        }
        action.setOnClickListener {
            when {
                viewModel.uiState.value.status == RunStatus.RUNNING -> viewModel.stop()
                hasLocationPermission() -> {
                    followLocation = true
                    viewModel.start()
                }
                else -> requestLocationPermission()
            }
        }
        upload.setOnClickListener { viewModel.uploadSavedRecord() }
        photo.setOnClickListener { photoPicker.launch("image/*") }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                    statusText.text = when (state.status) {
                        RunStatus.IDLE -> if (state.currentPoint == null) {
                            getString(R.string.run_locating)
                        } else {
                            getString(R.string.run_location_ready, state.accuracyMeters ?: 0)
                        }
                        RunStatus.RUNNING -> getString(
                            if (state.backgroundTrackingActive) {
                                R.string.run_running_background
                            } else {
                                R.string.run_running_foreground_only
                            },
                            state.pointCount,
                            state.stepCount
                        )
                        RunStatus.FINISHED -> getString(
                            R.string.run_finished,
                            state.pointCount,
                            state.stepCount
                        )
                        RunStatus.PERMISSION_REQUIRED -> getString(R.string.run_permission_required)
                    }
                    action.setText(
                        if (state.status == RunStatus.RUNNING) R.string.run_stop else R.string.run_start
                    )
                    action.isEnabled = state.recordSaveStatus !in setOf(
                        RecordSaveStatus.SAVING,
                        RecordSaveStatus.ATTACHING_PHOTO,
                        RecordSaveStatus.UPLOADING
                    )
                    distanceText.text = getString(R.string.run_metric_distance_value, state.distanceMeters)
                    durationText.text = RunMetrics.formatDuration(state.durationSeconds)
                    paceText.text = RunMetrics.formatPace(state.paceSecondsPerKm)
                    recordStatusText.text = when (state.recordSaveStatus) {
                        RecordSaveStatus.NONE -> ""
                        RecordSaveStatus.SAVING -> getString(R.string.run_record_saving)
                        RecordSaveStatus.SAVED -> getString(
                            if (state.photoAttached) {
                                R.string.run_record_saved_with_photo
                            } else {
                                R.string.run_record_saved
                            }
                        )
                        RecordSaveStatus.ATTACHING_PHOTO -> getString(R.string.run_photo_processing)
                        RecordSaveStatus.UPLOADING -> getString(R.string.run_record_uploading)
                        RecordSaveStatus.UPLOADED -> getString(R.string.run_record_uploaded)
                        RecordSaveStatus.UPLOADED_INVALID -> getString(
                            R.string.run_record_uploaded_invalid,
                            requireContext().recordIssueText(state.recordErrorCode)
                        )
                        RecordSaveStatus.ERROR -> getString(
                            R.string.run_record_error,
                            requireContext().recordIssueText(
                                state.recordErrorCode,
                                state.recordError ?: getString(R.string.unknown_error)
                            )
                        )
                    }
                    recordStatusText.visibility = if (
                        state.recordSaveStatus == RecordSaveStatus.NONE
                    ) View.GONE else View.VISIBLE
                    recordStatusText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            when (state.recordSaveStatus) {
                                RecordSaveStatus.UPLOADED -> R.color.or_status_success
                                RecordSaveStatus.UPLOADED_INVALID,
                                RecordSaveStatus.ERROR -> R.color.or_status_error
                                RecordSaveStatus.UPLOADING,
                                RecordSaveStatus.ATTACHING_PHOTO -> R.color.or_status_uploading
                                else -> R.color.or_status_pending
                            }
                        )
                    )
                    upload.isEnabled = state.recordSaveStatus in setOf(
                        RecordSaveStatus.SAVED,
                        RecordSaveStatus.ERROR
                    )
                    upload.visibility = if (
                        state.savedRecordId != null && state.recordSaveStatus !in setOf(
                            RecordSaveStatus.UPLOADED,
                            RecordSaveStatus.UPLOADED_INVALID
                        )
                    ) View.VISIBLE else View.GONE
                    photo.visibility = upload.visibility
                    photo.isEnabled = state.recordSaveStatus in setOf(
                        RecordSaveStatus.SAVED,
                        RecordSaveStatus.ERROR
                    )
                    photo.setText(
                        if (state.photoAttached) R.string.record_photo_replace else R.string.record_photo_add
                    )
                    state.currentPoint?.let {
                        renderCurrentLocation(it, state.accuracyMeters, false)
                    }
                    renderTrack(state.points)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is RunUiEvent.UploadCompleted -> Toast.makeText(
                                requireContext(),
                                if (event.verified) {
                                    getString(R.string.run_record_uploaded)
                                } else {
                                    getString(
                                        R.string.run_record_uploaded_invalid,
                                        requireContext().recordIssueText(event.invalidReason)
                                    )
                                },
                                Toast.LENGTH_LONG
                            ).show()
                            RunUiEvent.AlreadyUploaded -> Toast.makeText(
                                requireContext(),
                                getString(R.string.record_issue_already_uploaded),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        if (hasLocationPermission()) {
            viewModel.startLocating()
        } else {
            requestLocationPermission()
        }
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

    private fun renderTrack(points: List<TrackPoint>) {
        val map = aMap ?: return
        val latLngs = points.map { it.toMapLatLng() }
        routePolyline?.remove()
        routePolyline = if (latLngs.size >= 2) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .color(ContextCompat.getColor(requireContext(), R.color.map_route))
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
