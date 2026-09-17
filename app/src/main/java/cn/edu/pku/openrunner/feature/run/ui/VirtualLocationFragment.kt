package cn.edu.pku.openrunner.feature.run.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import kotlinx.coroutines.launch

/** Manual application-local samples. This does not register a system mock-location provider. */
class VirtualLocationFragment : Fragment() {
    private val viewModel: RunViewModel by activityViewModels {
        RunViewModel.Factory.from(requireContext())
    }
    private var mapView: MapView? = null
    private var map: AMap? = null
    private var candidateMarker: Marker? = null
    private var confirmedMarker: Marker? = null
    private var route: Polyline? = null
    private var candidate: TrackPoint? = null
    private var selectingPoint = false
    private var lastRenderedTrack: List<TrackPoint>? = null
    private var lastRenderedVirtual = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_virtual_location, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selectingPoint = savedInstanceState?.getBoolean(KEY_SELECTING) ?: false
        if (savedInstanceState?.containsKey(KEY_LATITUDE) == true) {
            candidate = TrackPoint(
                savedInstanceState.getDouble(KEY_LONGITUDE),
                savedInstanceState.getDouble(KEY_LATITUDE)
            )
        }
        val mapWidget = view.findViewById<MapView>(R.id.virtual_map)
        mapView = mapWidget
        mapWidget.onCreate(savedInstanceState)
        map = mapWidget.map.apply {
            mapType = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            ) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isScaleControlsEnabled = true
            val initialPoint = viewModel.uiState.value.virtualPoint
            moveCamera(CameraUpdateFactory.newLatLngZoom(
                initialPoint?.toLatLng() ?: CAMPUS_CENTER,
                if (initialPoint == null) 15.5f else 17f
            ))
        }
        val toggle = view.findViewById<Button>(R.id.virtual_toggle)
        val addPoint = view.findViewById<Button>(R.id.virtual_add_point)
        val confirm = view.findViewById<Button>(R.id.virtual_confirm_point)
        val hint = view.findViewById<TextView>(R.id.virtual_selection_hint)
        val status = view.findViewById<TextView>(R.id.virtual_status)

        fun refreshSelection() {
            val enabled = viewModel.uiState.value.virtualLocationEnabled
            addPoint.isEnabled = enabled
            confirm.isEnabled = enabled && candidate != null && selectingPoint
            hint.setText(when {
                !enabled -> R.string.virtual_hint_off
                !selectingPoint -> R.string.virtual_hint_ready
                candidate == null -> R.string.virtual_hint_selecting
                else -> R.string.virtual_hint_confirm
            })
        }

        toggle.setOnClickListener {
            viewModel.setVirtualLocationEnabled(!viewModel.uiState.value.virtualLocationEnabled)
        }
        addPoint.setOnClickListener {
            selectingPoint = true
            candidate = null
            renderCandidate()
            refreshSelection()
        }
        map?.setOnMapClickListener { position ->
            if (selectingPoint && viewModel.uiState.value.virtualLocationEnabled) {
                candidate = TrackPoint(position.longitude, position.latitude)
                renderCandidate()
                refreshSelection()
            }
        }
        confirm.setOnClickListener {
            val point = candidate ?: return@setOnClickListener
            if (!viewModel.uiState.value.virtualLocationEnabled) return@setOnClickListener
            viewModel.confirmVirtualPoint(point)
            selectingPoint = false
            candidate = null
            renderCandidate()
            refreshSelection()
            Toast.makeText(requireContext(), R.string.virtual_point_added, Toast.LENGTH_SHORT).show()
        }
        renderCandidate()
        viewModel.startLocating()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        toggle.setText(if (state.virtualLocationEnabled) {
                            R.string.virtual_toggle_on
                        } else {
                            R.string.virtual_toggle_off
                        })
                        if (!state.virtualLocationEnabled) {
                            candidate = null
                            selectingPoint = false
                            renderCandidate()
                        }
                        val pointLabel = state.virtualPoint?.let {
                            getString(R.string.virtual_point_coordinates, it.longitude, it.latitude)
                        } ?: getString(R.string.virtual_no_point)
                        status.text = getString(
                            R.string.virtual_run_status,
                            pointLabel,
                            getString(when (state.status) {
                                RunStatus.RUNNING -> R.string.virtual_run_active
                                RunStatus.FINISHED -> R.string.virtual_run_finished
                                else -> R.string.virtual_run_idle
                            }),
                            RunMetrics.formatDuration(state.durationSeconds),
                            state.distanceMeters,
                            state.pointCount
                        )
                        state.virtualPoint?.let(::renderConfirmedPoint)
                        renderTrack(state.points, state.usedVirtualLocation)
                        refreshSelection()
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        if (event is RunUiEvent.Message) {
                            Toast.makeText(requireContext(), event.text, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderCandidate() {
        candidateMarker?.remove()
        candidateMarker = candidate?.let {
            map?.addMarker(MarkerOptions().position(it.toLatLng())
                .title(getString(R.string.virtual_candidate_point))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
        }
    }

    private fun renderConfirmedPoint(point: TrackPoint) {
        val marker = confirmedMarker
        if (marker == null) {
            confirmedMarker = map?.addMarker(MarkerOptions().position(point.toLatLng())
                .title(getString(R.string.virtual_confirmed_point))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)))
        } else {
            marker.position = point.toLatLng()
        }
    }

    private fun renderTrack(points: List<TrackPoint>, virtual: Boolean) {
        if (points == lastRenderedTrack && virtual == lastRenderedVirtual) return
        lastRenderedTrack = points
        lastRenderedVirtual = virtual
        route?.remove()
        route = if (points.size < 2) null else map?.addPolyline(
            PolylineOptions().addAll(points.map { it.toLatLng() })
                .color(ContextCompat.getColor(requireContext(),
                    if (virtual) R.color.or_status_virtual else R.color.map_route))
                .width(8f)
        )
    }

    private fun TrackPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SELECTING, selectingPoint)
        candidate?.let {
            outState.putDouble(KEY_LATITUDE, it.latitude)
            outState.putDouble(KEY_LONGITUDE, it.longitude)
        }
    }

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
        candidateMarker?.remove()
        confirmedMarker?.remove()
        route?.remove()
        candidateMarker = null
        confirmedMarker = null
        route = null
        lastRenderedTrack = null
        mapView?.onDestroy()
        mapView = null
        map = null
        super.onDestroyView()
    }

    companion object {
        private const val KEY_SELECTING = "virtual_selecting"
        private const val KEY_LATITUDE = "virtual_candidate_latitude"
        private const val KEY_LONGITUDE = "virtual_candidate_longitude"
        private val CAMPUS_CENTER = LatLng(39.99281, 116.31088)
    }
}
