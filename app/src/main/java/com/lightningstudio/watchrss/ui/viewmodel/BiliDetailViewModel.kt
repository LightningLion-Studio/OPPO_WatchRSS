package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightningstudio.watchrss.data.bili.BiliRepository
import com.lightningstudio.watchrss.sdk.bili.BiliPage
import com.lightningstudio.watchrss.sdk.bili.BiliVideoDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BiliDetailUiState(
    val isLoading: Boolean = true,
    val detail: BiliVideoDetail? = null,
    val selectedPageIndex: Int = 0,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val isWatchLater: Boolean = false,
    val message: String? = null
)

class BiliDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BiliRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BiliDetailUiState())
    val uiState: StateFlow<BiliDetailUiState> = _uiState

    private val aid: Long? = savedStateHandle.get<String>("aid")?.toLongOrNull()
    private val bvid: String? = savedStateHandle.get<String>("bvid")?.takeIf { it.isNotBlank() }
    private val cidArg: Long? = savedStateHandle.get<String>("cid")?.toLongOrNull()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val result = repository.fetchVideoDetail(aid = aid, bvid = bvid)
            if (result.isSuccess) {
                val detail = result.data
                val selected = resolveInitialPageIndex(detail?.pages, cidArg)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        detail = detail,
                        selectedPageIndex = selected,
                        message = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = result.message ?: "获取详情失败"
                    )
                }
            }
        }
    }

    fun selectPage(index: Int) {
        _uiState.update { it.copy(selectedPageIndex = index) }
    }

    fun like() {
        val safeAid = aid ?: return
        viewModelScope.launch {
            val result = repository.like(safeAid, like = !_uiState.value.isLiked)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLiked = !it.isLiked, message = "已更新点赞") }
            } else {
                _uiState.update { it.copy(message = result.message ?: "点赞失败") }
            }
        }
    }

    fun coin() {
        val safeAid = aid ?: return
        viewModelScope.launch {
            val result = repository.coin(safeAid)
            if (result.isSuccess) {
                _uiState.update { it.copy(message = if (result.data == true) "投币并点赞" else "投币成功") }
            } else {
                _uiState.update { it.copy(message = result.message ?: "投币失败") }
            }
        }
    }

    fun favorite() {
        val safeAid = aid ?: return
        viewModelScope.launch {
            val result = repository.favorite(safeAid, add = !_uiState.value.isFavorited)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isFavorited = !it.isFavorited,
                        message = if (it.isFavorited) "已取消收藏" else "已收藏"
                    )
                }
            } else {
                _uiState.update { it.copy(message = result.message ?: "收藏失败") }
            }
        }
    }

    fun addToWatchLater() {
        viewModelScope.launch {
            val result = repository.addToView(aid = aid, bvid = bvid)
            if (result.isSuccess) {
                _uiState.update { it.copy(isWatchLater = true, message = "已加入稍后再看") }
            } else {
                _uiState.update { it.copy(message = result.message ?: "加入失败") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun selectedCid(): Long? {
        val detail = _uiState.value.detail ?: return cidArg
        val pages = detail.pages
        if (pages.isEmpty()) return cidArg ?: detail.item.cid
        val index = _uiState.value.selectedPageIndex.coerceIn(0, pages.lastIndex)
        return pages[index].cid ?: cidArg ?: detail.item.cid
    }

    fun selectedPage(): BiliPage? {
        val detail = _uiState.value.detail ?: return null
        val pages = detail.pages
        if (pages.isEmpty()) return null
        val index = _uiState.value.selectedPageIndex.coerceIn(0, pages.lastIndex)
        return pages[index]
    }

    private fun resolveInitialPageIndex(pages: List<BiliPage>?, cid: Long?): Int {
        val safePages = pages ?: return 0
        if (cid == null) return 0
        val index = safePages.indexOfFirst { it.cid == cid }
        return if (index >= 0) index else 0
    }
}
