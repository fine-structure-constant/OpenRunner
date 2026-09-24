package cn.edu.pku.openrunner.feature.records.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
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

    /** 进度环与百分比数字共用的缓动，只建一次。 */
    private val goalInterpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    /** 目标进度动画。状态重复下发时要先打断上一条，否则数字会来回抢。 */
    private var goalAnimator: ValueAnimator? = null

    /** 数字当前显示的百分比，作为下一次动画的起点。 */
    private var goalShownPercent = 0

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
                            animateGoalProgress(goalProgress, goalPercent, 0, unavailable = true)
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
                            animateGoalProgress(
                                goalProgress,
                                goalPercent,
                                percent,
                                unavailable = userStatus.target <= 0
                            )
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

    override fun onDestroyView() {
        // 动画的每帧回调持有 View 的引用，视图销毁前必须断掉。
        goalAnimator?.cancel()
        goalAnimator = null
        goalShownPercent = 0
        super.onDestroyView()
    }

    /**
     * 进度环与百分比数字一起走。
     *
     * 环原先用 setProgressCompat(percent, true) 让控件自己插值，数字却直接跳到终值，
     * 同一张卡里两个元素各走各的，看着像数字先到、环后到。改成这里统一插值：
     * 每帧把同一个结果同时喂给环（setProgressCompat(v, false)，不再让控件自己动）
     * 与数字，两者必然同步，时长与缓动也由这里说了算。
     *
     * 数据还没到位时 target 传 0、unavailable 传 true：数字显示占位符、环退回 0，
     * 不会出现一个孤零零的百分比配一个没有数据的环。
     */
    private fun animateGoalProgress(
        ring: CircularProgressIndicator,
        percentText: TextView,
        target: Int,
        unavailable: Boolean
    ) {
        goalAnimator?.cancel()
        val from = goalShownPercent
        if (from == target) {
            applyGoalProgress(ring, percentText, target, unavailable)
            return
        }
        goalAnimator = ValueAnimator.ofInt(from, target).apply {
            duration = GOAL_ANIMATION_MILLIS
            interpolator = goalInterpolator
            addUpdateListener { animator ->
                applyGoalProgress(
                    ring,
                    percentText,
                    animator.animatedValue as Int,
                    unavailable
                )
            }
            start()
        }
    }

    private fun applyGoalProgress(
        ring: CircularProgressIndicator,
        percentText: TextView,
        percent: Int,
        unavailable: Boolean
    ) {
        goalShownPercent = percent
        ring.setProgressCompat(percent, false)
        percentText.text = if (unavailable) {
            getString(R.string.record_goal_percent_unavailable)
        } else {
            getString(R.string.record_goal_percent, percent)
        }
    }

    private companion object {
        /** 进度环与数字的动画时长，与 Material 进度指示器默认的 500ms 对齐。 */
        private const val GOAL_ANIMATION_MILLIS = 500L
    }
}
