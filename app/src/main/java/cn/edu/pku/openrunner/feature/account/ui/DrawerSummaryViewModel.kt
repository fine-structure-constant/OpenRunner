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

/** A lightweight, activity-scoped summary; no record-list fetch or token exposure in the drawer. */
class DrawerSummaryViewModel(
    private val sessionStore: SessionStore,
    private val api: PkuNewYouthApi = ApiClient.api
) : ViewModel() {
    private val _uiState = MutableStateFlow(DrawerSummaryState())
    val uiState = _uiState.asStateFlow()
    private var request: Job? = null
    private var sessionToken: String? = null
    private var lastSuccessAt = 0L

    fun syncSession() {
        val token = sessionStore.token?.takeIf { it.isNotBlank() }
        val userId = sessionStore.userId?.takeIf { it.isNotBlank() && token != null }
        if (userId != _uiState.value.userId || token != sessionToken) {
            request?.cancel()
            sessionToken = token
            lastSuccessAt = 0L
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
        if (!force && (_uiState.value.loading ||
                System.currentTimeMillis() - lastSuccessAt < CACHE_MILLIS)) return
        request?.cancel()
        _uiState.value = _uiState.value.copy(loading = true, syncFailed = false)
        request = viewModelScope.launch {
            try {
                val status = api.getUserStatus(userId).requireData()
                if (sessionStore.userId != userId || sessionStore.token != token) {
                    syncSession()
                    return@launch
                }
                lastSuccessAt = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(status = status, loading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (sessionStore.userId != userId || sessionStore.token != token) {
                    syncSession()
                } else {
                    _uiState.value = _uiState.value.copy(loading = false, syncFailed = true)
                }
            }
        }
    }

    class Factory(private val sessionStore: SessionStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DrawerSummaryViewModel(sessionStore) as T
    }

    companion object {
        private const val CACHE_MILLIS = 30_000L
    }
}
