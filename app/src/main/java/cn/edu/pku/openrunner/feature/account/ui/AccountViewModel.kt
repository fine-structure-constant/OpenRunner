package cn.edu.pku.openrunner.feature.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.pku.openrunner.core.network.ApiClient
import cn.edu.pku.openrunner.core.network.ApiException
import cn.edu.pku.openrunner.core.network.PkuNewYouthApi
import cn.edu.pku.openrunner.core.network.RunRecordDto
import cn.edu.pku.openrunner.core.network.UserStatusDto
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.records.domain.RecordOrdering
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AccountUiState(
    val loading: Boolean = false,
    val authenticated: Boolean = false,
    val needsLogin: Boolean = false,
    val userId: String? = null,
    val userName: String? = null,
    val department: String? = null,
    val token: String? = null,
    val status: UserStatusDto? = null,
    val records: List<RunRecordDto> = emptyList(),
    val errorMessage: String? = null
)

class AccountViewModel(
    private val sessionStore: SessionStore,
    private val api: PkuNewYouthApi = ApiClient.api
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun refresh() {
        val userId = sessionStore.userId
        val token = sessionStore.token
        if (userId.isNullOrBlank() || token.isNullOrBlank()) {
            _uiState.value = AccountUiState(needsLogin = true)
            return
        }
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = true,
                needsLogin = false,
                errorMessage = null,
                userId = userId,
                userName = sessionStore.userName,
                department = sessionStore.department,
                token = token
            )
            try {
                val status = api.getUserStatus(userId).requireData()
                val records = api.getRecords(userId).requireData()
                _uiState.value = AccountUiState(
                    authenticated = true,
                    userId = userId,
                    userName = sessionStore.userName,
                    department = sessionStore.department,
                    token = token,
                    status = status,
                    records = RecordOrdering.newestRecordsFirst(records)
                )
            } catch (error: Exception) {
                if (isAuthenticationFailure(error)) {
                    sessionStore.clear()
                    _uiState.value = AccountUiState(
                        needsLogin = true,
                        errorMessage = "登录状态已失效，请重新登录"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        errorMessage = error.message ?: "账户数据加载失败"
                    )
                }
            }
        }
    }

    fun logout() {
        sessionStore.clear()
        _uiState.value = AccountUiState(needsLogin = true)
    }

    private fun isAuthenticationFailure(error: Exception): Boolean {
        if (error is ApiException && error.code in setOf(3, 14, 401, 403)) return true
        if (error is HttpException && error.code() in setOf(401, 403)) return true
        val message = error.message?.lowercase().orEmpty()
        return listOf("token", "unauthorized", "未登录", "登录失效", "失效", "认证").any(message::contains)
    }

    class Factory(
        private val sessionStore: SessionStore,
        private val api: PkuNewYouthApi = ApiClient.api
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountViewModel(sessionStore, api) as T
        }
    }
}
