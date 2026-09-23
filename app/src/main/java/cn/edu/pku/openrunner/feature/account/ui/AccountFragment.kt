package cn.edu.pku.openrunner.feature.account.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.MainActivity
import cn.edu.pku.openrunner.core.network.RunRecordDto
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.auth.ui.AuthActivity
import java.text.DateFormat
import kotlinx.coroutines.launch

class AccountFragment : Fragment() {
    private val viewModel: AccountViewModel by viewModels {
        AccountViewModel.Factory(SessionStore(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_account, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val loginPanel = view.findViewById<View>(R.id.account_login_panel)
        val profilePanel = view.findViewById<View>(R.id.account_profile_panel)
        val error = view.findViewById<TextView>(R.id.account_error)
        val name = view.findViewById<TextView>(R.id.account_name)
        val id = view.findViewById<TextView>(R.id.account_id)
        val department = view.findViewById<TextView>(R.id.account_department)
        val token = view.findViewById<TextView>(R.id.account_token)
        val stats = view.findViewById<TextView>(R.id.account_stats)
        val records = view.findViewById<LinearLayout>(R.id.account_records)
        val login = view.findViewById<Button>(R.id.account_login)
        val logout = view.findViewById<Button>(R.id.account_logout)

        login.setOnClickListener {
            startActivity(Intent(requireContext(), AuthActivity::class.java))
        }
        logout.setOnClickListener { viewModel.logout() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    (activity as? MainActivity)?.refreshNavigation()
                    loginPanel.visibility = if (state.needsLogin) View.VISIBLE else View.GONE
                    profilePanel.visibility = if (state.authenticated) View.VISIBLE else View.GONE
                    error.text = state.errorMessage.orEmpty()
                    error.visibility = if (state.errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE
                    name.text = state.userName?.takeIf { it.isNotBlank() } ?: "未同步"
                    id.text = state.userId ?: "—"
                    department.text = state.department?.takeIf { it.isNotBlank() } ?: "—"
                    token.text = state.token ?: "—"
                    state.status?.let { current ->
                        stats.text = getString(
                            R.string.account_stats,
                            current.current / 1000.0,
                            current.bonus / 1000.0,
                            current.target / 1000.0,
                            current.validCount,
                            if (current.isPassed) getString(R.string.account_passed) else getString(R.string.account_in_progress),
                            state.records.size
                        )
                    }
                    renderRecords(records, state.records)
                }
            }
        }
        viewModel.refresh()
    }

    override fun onResume() {
        super.onResume()
        if (view != null) viewModel.refresh()
    }

    private fun renderRecords(container: LinearLayout, items: List<RunRecordDto>) {
        container.removeAllViews()
        if (items.isEmpty()) {
            addRecordLine(container, getString(R.string.account_no_records))
            return
        }
        items.take(12).forEach { record ->
            val date = if (record.date != null) {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(record.date)
            } else {
                "日期未知"
            }
            addRecordLine(
                container,
                getString(
                    R.string.account_record_item,
                    date,
                    record.distance,
                    record.duration.toInt(),
                    record.step
                )
            )
        }
    }

    private fun addRecordLine(container: LinearLayout, text: String) {
        val card = layoutInflater.inflate(R.layout.item_account_record, container, false)
        card.findViewById<TextView>(R.id.account_record_text).text = text
        container.addView(card)
    }
}
