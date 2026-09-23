package cn.edu.pku.openrunner.feature.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.pku.openrunner.core.network.ApiClient
import cn.edu.pku.openrunner.core.network.PkuNewYouthApi
import cn.edu.pku.openrunner.core.network.UserStatusDto
import cn.edu.pku.openrunner.core.session.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DrawerSummaryState(
    val userId: String? = null,
    val userName: String? = null,
    val status: UserStatusDto? = null,
    val loading: Boolean = false,
    val syncFailed: Boolean = false
)

/**
 * A lightweight, activity-scoped summary; no record-list fetch or token exposure in the drawer.
 *
 * The drawer is opened and closed constantly, while the official summary it shows only moves
 * when an upload lands — so every request from here is one the user did not ask for. The
 * throttling rules live in [DrawerRefreshGate]; this class only adds the two things the gate
 * cannot know about:
 *
 *  * a refresh asked for while a request is already in flight is remembered and run once
 *    afterwards, instead of starting a second request in parallel;
 *  * a refresh the gate defers is actually scheduled, so a mutation that landed during a burst
 *    still gets picked up rather than being dropped.
 */
class DrawerSummaryViewModel(
    private val sessionStore: SessionStore,
    private val api: PkuNewYouthApi = ApiClient.api
) : ViewModel() {
    private val _uiState = MutableStateFlow(DrawerSummaryState())
    val uiState = _uiState.asStateFlow()
    private val gate = DrawerRefreshGate(CACHE_MILLIS, MIN_INTERVAL_MILLIS)
    private var request: Job? = null
    private var deferred: Job? = null
    private var sessionToken: String? = null
    private var refreshQueued = false

    fun syncSession() {
        val token = sessionStore.token?.takeIf { it.isNotBlank() }
        val userId = sessionStore.userId?.takeIf { it.isNotBlank() && token != null }
        if (userId != _uiState.value.userId || token != sessionToken) {
            cancelWork()
            sessionToken = token
            // Nothing fetched for the previous account may be reused for this one.
            gate.reset()
            _uiState.value = DrawerSummaryState(userId = userId)
        }
        _uiState.value = _uiState.value.copy(
            userName = sessionStore.userName?.takeIf { userId != null && it.isNotBlank() }
        )
    }

    fun refresh(force: Boolean = false) {
        syncSession()
        val userId = _uiState.value.userId ?: return
        val token = sessionToken

        if (request?.isActive == true) {
            if (force) refreshQueued = true
            return
        }

        val now = System.currentTimeMillis()
        when (gate.decide(force, now)) {
            DrawerRefreshGate.Decision.SKIP -> return
            DrawerRefreshGate.Decision.DEFER -> {
                scheduleDeferredRefresh()
                return
            }
            DrawerRefreshGate.Decision.FETCH -> Unit
        }

        gate.recordAttempt(now)
        refreshQueued = false
        deferred?.cancel()
        _uiState.value = _uiState.value.copy(loading = true, syncFailed = false)
        request = viewModelScope.launch {
            try {
                val status = api.getUserStatus(userId).requireData()
                if (sessionStore.userId != userId || sessionStore.token != token) {
                    syncSession()
                    return@launch
                }
                gate.recordSuccess(System.currentTimeMillis())
                _uiState.value = _uiState.value.copy(status = status, loading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (sessionStore.userId != userId || sessionStore.token != token) {
                    syncSession()
                } else {
                    _uiState.value = _uiState.value.copy(loading = false, syncFailed = true)
                }
            } finally {
                if (refreshQueued) scheduleDeferredRefresh()
            }
        }
    }

    /**
     * Runs the refresh the gate deferred, once the floor has passed. Re-scheduling cancels the
     * pending job, so a stream of rapid `force` calls still produces a single trailing request.
     */
    private fun scheduleDeferredRefresh() {
        refreshQueued = false
        deferred?.cancel()
        deferred = viewModelScope.launch {
            val wait = gate.deferralMillis(System.currentTimeMillis())
            if (wait > 0) delay(wait)
            refresh(force = true)
        }
    }

    private fun cancelWork() {
        request?.cancel()
        request = null
        deferred?.cancel()
        deferred = null
        refreshQueued = false
    }

    override fun onCleared() {
        cancelWork()
        super.onCleared()
    }

    class Factory(private val sessionStore: SessionStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DrawerSummaryViewModel(sessionStore) as T
    }

    companion object {
        /** How long a successful fetch is trusted when the caller does not force a refresh. */
        private const val CACHE_MILLIS = 30_000L

        /**
         * Floor between two attempts, forced ones included.
         *
         * Long enough that closing and reopening the drawer cannot fire a request per open,
         * short enough that the "打开菜单重试" hint on the failure line stays honest.
         */
        private const val MIN_INTERVAL_MILLIS = 5_000L
    }
}
