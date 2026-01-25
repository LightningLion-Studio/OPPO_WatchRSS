package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightningstudio.watchrss.data.bili.BiliRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BiliPlayerUiState(
    val isLoading: Boolean = true,
    val playUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val message: String? = null
)

class BiliPlayerViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BiliRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BiliPlayerUiState())
    val uiState: StateFlow<BiliPlayerUiState> = _uiState

    private val aid: Long? = savedStateHandle.get<String>("aid")?.toLongOrNull()
    private val bvid: String? = savedStateHandle.get<String>("bvid")?.takeIf { it.isNotBlank() }
    private val cid: Long? = savedStateHandle.get<String>("cid")?.toLongOrNull()

    init {
        loadPlayUrl()
    }

    fun loadPlayUrl() {
        val safeCid = cid
        if (safeCid == null) {
            _uiState.update { it.copy(isLoading = false, message = "缺少播放参数") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val result = repository.fetchPlayUrlMp4(cid = safeCid, aid = aid, bvid = bvid)
            if (result.isSuccess) {
                val url = result.data?.durl?.firstOrNull()?.url
                val headers = repository.buildPlayHeaders()
                if (url.isNullOrBlank()) {
                    _uiState.update { it.copy(isLoading = false, message = "播放地址为空") }
                } else {
                    _uiState.update { it.copy(isLoading = false, playUrl = url, headers = headers) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, message = result.message ?: "获取播放地址失败") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
