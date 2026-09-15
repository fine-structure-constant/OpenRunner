package cn.edu.pku.openrunner.feature.records.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.pku.openrunner.feature.records.data.RecordRepository
import cn.edu.pku.openrunner.feature.records.domain.RecordListItem
import cn.edu.pku.openrunner.feature.records.domain.RecordUploadState
import cn.edu.pku.openrunner.core.network.ApiException
import cn.edu.pku.openrunner.core.network.UserStatusDto
import cn.edu.pku.openrunner.feature.run.data.RecordAlreadyUploadedException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class RecordListUiState(
    val loading: Boolean = false,
    val records: List<RecordListItem> = emptyList(),
    val errorMessage: String? = null,
    val userStatus: UserStatusDto? = null,
    val statusErrorMessage: String? = null,
    val uploadingLocalIds: Set<String> = emptySet(),
    val attachingPhotoLocalIds: Set<String> = emptySet()
)

sealed interface RecordListEvent {
    data class Message(val text: String) : RecordListEvent
}

class RecordListViewModel(private val repository: RecordRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordListUiState())
    val uiState: StateFlow<RecordListUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<RecordListEvent>(extraBufferCapacity = 4)
    val events = _events

    init {
        viewModelScope.launch {
            repository.observeLocalChanges().collect { loadRecords(showLoading = false) }
        }
    }

    fun refresh() {
        if (_uiState.value.loading) return
        viewModelScope.launch { loadAll() }
    }

    fun upload(localId: String) {
        if (localId in _uiState.value.uploadingLocalIds) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                uploadingLocalIds = _uiState.value.uploadingLocalIds + localId,
                records = _uiState.value.records.map { item ->
                    if (item.localId == localId) {
                        item.copy(uploadState = RecordUploadState.UPLOADING)
                    } else {
                        item
                    }
                },
                errorMessage = null
            )
            runCatching { repository.upload(localId) }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        uploadingLocalIds = _uiState.value.uploadingLocalIds - localId,
                        records = _uiState.value.records.map { item ->
                            if (item.localId == localId) {
                                item.copy(
                                    record = result,
                                    uploadState = if (result.verified) {
                                        RecordUploadState.UPLOADED_VALID
                                    } else {
                                        RecordUploadState.UPLOADED_INVALID
                                    },
                                    failureCode = null,
                                    failureMessage = null
                                )
                            } else {
                                item
                            }
                        }
                    )
                    _events.tryEmit(
                        RecordListEvent.Message(
                            if (result.verified) {
                                "记录已保存，已上传服务器"
                            } else {
                                "记录已上传服务器，但验证未通过"
                            }
                        )
                    )
                    loadAll(showLoading = false)
                }
                .onFailure { error ->
                    if (error is RecordAlreadyUploadedException) {
                        _uiState.value = _uiState.value.copy(
                            uploadingLocalIds = _uiState.value.uploadingLocalIds - localId,
                            records = _uiState.value.records.filterNot { it.localId == localId }
                        )
                        _events.tryEmit(RecordListEvent.Message(error.message.orEmpty()))
                        loadAll(showLoading = false)
                        return@onFailure
                    }
                    _uiState.value = _uiState.value.copy(
                        uploadingLocalIds = _uiState.value.uploadingLocalIds - localId,
                        records = _uiState.value.records.map { item ->
                            if (item.localId == localId) {
                                item.copy(
                                    uploadState = RecordUploadState.FAILED,
                                    failureCode = (error as? ApiException)?.code,
                                    failureMessage = error.message ?: "记录上传失败"
                                )
                            } else {
                                item
                            }
                        }
                    )
                    _events.tryEmit(RecordListEvent.Message(error.message ?: "记录上传失败"))
                    loadRecords(showLoading = false)
                }
        }
    }

    fun delete(item: RecordListItem) {
        if (item.localId in _uiState.value.uploadingLocalIds ||
            item.localId in _uiState.value.attachingPhotoLocalIds
        ) return
        _uiState.value = _uiState.value.copy(
            records = _uiState.value.records.filterNot { it.itemKey == item.itemKey }
        )
        val localId = item.localId
        if (localId == null) {
            _events.tryEmit(RecordListEvent.Message("已从本页移除；刷新后云端记录会重新显示"))
            return
        }
        viewModelScope.launch {
            runCatching { repository.deleteLocal(localId) }
                .onSuccess {
                    _events.tryEmit(RecordListEvent.Message("本地记录已删除"))
                }
                .onFailure { error ->
                    _events.tryEmit(RecordListEvent.Message(error.message ?: "本地记录删除失败"))
                    loadRecords(showLoading = false)
                }
        }
    }

    fun attachPhoto(localId: String, source: Uri) {
        if (localId in _uiState.value.attachingPhotoLocalIds) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                attachingPhotoLocalIds = _uiState.value.attachingPhotoLocalIds + localId,
                records = _uiState.value.records.map { item ->
                    if (item.localId == localId) item.copy(isPhotoProcessing = true) else item
                }
            )
            runCatching { repository.attachPhoto(localId, source) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        attachingPhotoLocalIds = _uiState.value.attachingPhotoLocalIds - localId,
                        records = _uiState.value.records.map { item ->
                            if (item.localId == localId) {
                                item.copy(hasPhoto = true, isPhotoProcessing = false)
                            } else {
                                item
                            }
                        }
                    )
                    _events.tryEmit(RecordListEvent.Message("图片已添加"))
                    loadRecords(showLoading = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        attachingPhotoLocalIds = _uiState.value.attachingPhotoLocalIds - localId,
                        records = _uiState.value.records.map { item ->
                            if (item.localId == localId) {
                                item.copy(isPhotoProcessing = false)
                            } else {
                                item
                            }
                        }
                    )
                    _events.tryEmit(RecordListEvent.Message(error.message ?: "图片处理失败"))
                }
        }
    }

    private suspend fun loadRecords(showLoading: Boolean = true) {
        if (showLoading) {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        }
        runCatching { repository.list() }
            .onSuccess { records ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    records = records.map { item ->
                        if (item.localId in _uiState.value.uploadingLocalIds) {
                            item.copy(uploadState = RecordUploadState.UPLOADING)
                        } else if (item.localId in _uiState.value.attachingPhotoLocalIds) {
                            item.copy(isPhotoProcessing = true)
                        } else {
                            item
                        }
                    },
                    errorMessage = null
                )
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = it.message ?: "记录加载失败"
                )
            }
    }

    private suspend fun loadAll(showLoading: Boolean = true) = supervisorScope {
        if (showLoading) {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        }
        val recordsTask = async { runCatching { repository.list() } }
        val statusTask = async { runCatching { repository.status() } }
        val recordsResult = recordsTask.await()
        val statusResult = statusTask.await()
        val current = _uiState.value
        _uiState.value = current.copy(
            loading = false,
            records = recordsResult.getOrNull()?.map { item ->
                when {
                    item.localId in current.uploadingLocalIds ->
                        item.copy(uploadState = RecordUploadState.UPLOADING)
                    item.localId in current.attachingPhotoLocalIds ->
                        item.copy(isPhotoProcessing = true)
                    else -> item
                }
            } ?: current.records,
            errorMessage = recordsResult.exceptionOrNull()?.message,
            userStatus = statusResult.getOrNull() ?: current.userStatus,
            statusErrorMessage = statusResult.exceptionOrNull()?.message
        )
    }

    class Factory(private val repository: RecordRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecordListViewModel(repository) as T
        }
    }
}
