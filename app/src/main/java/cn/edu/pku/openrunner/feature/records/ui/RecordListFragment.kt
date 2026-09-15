package cn.edu.pku.openrunner.feature.records.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.core.network.ApiClient
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.records.data.RecordRepository
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecordStore
import cn.edu.pku.openrunner.feature.run.data.RunRecordRepository
import cn.edu.pku.openrunner.feature.run.data.RunPhotoStore
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class RecordListFragment : Fragment() {
    private var photoTargetLocalId: String? = null
    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val localId = photoTargetLocalId
        photoTargetLocalId = null
        if (uri != null && localId != null) viewModel.attachPhoto(localId, uri)
    }

    private val viewModel: RecordListViewModel by viewModels {
        val sessionStore = SessionStore(requireContext())
        RecordListViewModel.Factory(
            RecordRepository(
                sessionStore,
                RunRecordRepository(
                    sessionStore,
                    LocalRunRecordStore(requireContext()),
                    RunPhotoStore(requireContext()),
                    ApiClient.api
                ),
                ApiClient.api
            )
        )
    }
    private val adapter = RecordAdapter(
        onUpload = { localId -> viewModel.upload(localId) },
        onChoosePhoto = { localId ->
            photoTargetLocalId = localId
            photoPicker.launch("image/*")
        },
        onDelete = { item -> viewModel.delete(item) },
        onOpenDetails = { item ->
            item.detailLocalId?.let { localId ->
                startActivity(RecordDetailActivity.intent(requireContext(), localId))
            }
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_records, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<RecyclerView>(R.id.records_list)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.records_refresh)
        val progress = view.findViewById<ProgressBar>(R.id.records_progress)
        val empty = view.findViewById<TextView>(R.id.records_empty)
        val goalProgress = view.findViewById<CircularProgressIndicator>(
            R.id.records_goal_progress
        )
        val goalPercent = view.findViewById<TextView>(R.id.records_goal_percent)
        val goalDistance = view.findViewById<TextView>(R.id.records_goal_distance)
        val goalDetail = view.findViewById<TextView>(R.id.records_goal_detail)
        var lastShownError: String? = null
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        refresh.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        adapter.submitList(state.records)
                        refresh.isRefreshing = state.loading
                        progress.visibility = if (state.loading && state.records.isEmpty()) View.VISIBLE else View.GONE
                        empty.text = state.errorMessage ?: getString(R.string.records_empty)
                        empty.visibility = if (!state.loading && state.records.isEmpty()) View.VISIBLE else View.GONE
                        val userStatus = state.userStatus
                        if (userStatus == null) {
                            goalProgress.setProgressCompat(0, true)
                            goalPercent.setText(R.string.record_goal_percent_unavailable)
                            goalDistance.setText(
                                if (state.loading) {
                                    R.string.record_goal_loading
                                } else {
                                    R.string.record_goal_unavailable
                                }
                            )
                            goalDetail.text = state.statusErrorMessage
                                ?: getString(R.string.record_goal_detail_loading)
                        } else {
                            val percent = if (userStatus.target > 0) {
                                (userStatus.current.toDouble() * 100.0 / userStatus.target)
                                    .roundToInt()
                                    .coerceIn(0, 100)
                            } else {
                                0
                            }
                            goalProgress.setProgressCompat(percent, true)
                            goalPercent.text = if (userStatus.target > 0) {
                                getString(R.string.record_goal_percent, percent)
                            } else {
                                getString(R.string.record_goal_percent_unavailable)
                            }
                            goalDistance.text = if (userStatus.target > 0) {
                                getString(
                                    R.string.record_goal_distance,
                                    userStatus.current / 1_000.0,
                                    userStatus.target / 1_000.0
                                )
                            } else {
                                getString(
                                    R.string.record_goal_no_target,
                                    userStatus.current / 1_000.0
                                )
                            }
                            goalDetail.text = getString(
                                R.string.record_goal_detail,
                                userStatus.bonus / 1_000.0,
                                userStatus.validCount,
                                getString(
                                    if (userStatus.isPassed) {
                                        R.string.account_passed
                                    } else {
                                        R.string.account_in_progress
                                    }
                                )
                            )
                        }
                        if (state.errorMessage == null) {
                            lastShownError = null
                        } else if (state.records.isNotEmpty() && state.errorMessage != lastShownError) {
                            lastShownError = state.errorMessage
                            Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is RecordListEvent.Message -> Toast.makeText(
                                requireContext(),
                                event.text,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
        viewModel.refresh()
    }
}
