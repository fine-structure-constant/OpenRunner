package cn.edu.pku.openrunner.feature.tasks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
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
import cn.edu.pku.openrunner.feature.tasks.data.TaskRepository
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {
    private val viewModel: TaskListViewModel by viewModels {
        TaskListViewModel.Factory(
            TaskRepository(SessionStore(requireContext()), ApiClient.api)
        )
    }
    private val adapter = TaskAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tasks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<RecyclerView>(R.id.tasks_list)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.tasks_refresh)
        val progress = view.findViewById<ProgressBar>(R.id.tasks_progress)
        val empty = view.findViewById<TextView>(R.id.tasks_empty)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        refresh.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.tasks)
                    refresh.isRefreshing = state.loading
                    progress.visibility = if (state.loading && state.tasks.isEmpty()) View.VISIBLE else View.GONE
                    empty.text = if (state.errorMessage == null) {
                        getString(R.string.tasks_empty)
                    } else {
                        getString(R.string.tasks_error, state.errorMessage)
                    }
                    empty.visibility = if (!state.loading && state.tasks.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        viewModel.refresh()
    }
}
