package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightningstudio.watchrss.data.bili.BiliRepository
import com.lightningstudio.watchrss.sdk.bili.BiliFavoritePage
import com.lightningstudio.watchrss.sdk.bili.BiliHistoryCursor
import com.lightningstudio.watchrss.sdk.bili.BiliHistoryPage
import com.lightningstudio.watchrss.sdk.bili.BiliItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BiliListType(val id: String, val title: String) {
    WATCH_LATER("watch_later", "稍后再看"),
    HISTORY("history", "历史记录"),
    FAVORITE("favorite", "收藏夹");

    companion object {
        fun from(id: String?): BiliListType {
            return values().firstOrNull { it.id == id } ?: WATCH_LATER
        }
    }
}

data class BiliListItem(
    val aid: Long?,
    val bvid: String?,
    val cid: Long?,
    val title: String,
    val cover: String?,
    val subtitle: String?,
    val durationSeconds: Int?
)

data class BiliListUiState(
    val type: BiliListType,
    val isLoading: Boolean = true,
    val items: List<BiliListItem> = emptyList(),
    val message: String? = null,
    val canLoadMore: Boolean = false
)

class BiliListViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BiliRepository
) : ViewModel() {
    private val type: BiliListType = BiliListType.from(savedStateHandle.get<String>("type"))
    private val _uiState = MutableStateFlow(BiliListUiState(type = type))
    val uiState: StateFlow<BiliListUiState> = _uiState

    private var historyCursor: BiliHistoryCursor? = null
    private var favoritePage = 1
    private var favoriteMediaId: Long? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, items = emptyList()) }
            when (type) {
                BiliListType.WATCH_LATER -> {
                    val result = repository.fetchToView()
                    if (result.isSuccess) {
                        val items = result.data?.items?.map { it.toListItem() }.orEmpty()
                        _uiState.update { it.copy(isLoading = false, items = items, canLoadMore = false) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, message = result.message ?: "加载失败") }
                    }
                }
                BiliListType.HISTORY -> {
                    val result = repository.fetchHistory()
                    handleHistoryResult(result.data, result.message)
                }
                BiliListType.FAVORITE -> {
                    val page = fetchFavoritePage(reset = true)
                    handleFavoritePage(page)
                }
            }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            when (type) {
                BiliListType.HISTORY -> {
                    val cursor = historyCursor ?: return@launch
                    val result = repository.fetchHistory(cursor)
                    handleHistoryResult(result.data, result.message, append = true)
                }
                BiliListType.FAVORITE -> {
                    val page = fetchFavoritePage(reset = false)
                    handleFavoritePage(page, append = true)
                }
                else -> Unit
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun fetchFavoritePage(reset: Boolean): BiliFavoritePage? {
        if (reset) {
            favoritePage = 1
            favoriteMediaId = null
        }
        val mediaId = favoriteMediaId ?: run {
            val folders = repository.fetchFavoriteFolders()
            if (!folders.isSuccess) return null
            val first = folders.data?.firstOrNull() ?: return null
            val id = first.id ?: first.fid ?: return null
            favoriteMediaId = id
            id
        }
        val result = repository.fetchFavoriteItems(mediaId, pn = favoritePage)
        return if (result.isSuccess) result.data else null
    }

    private fun handleFavoritePage(
        page: BiliFavoritePage?,
        append: Boolean = false
    ) {
        if (page == null) {
            _uiState.update { it.copy(isLoading = false, message = "加载收藏失败") }
            return
        }
        val items = page.medias.map { media ->
            BiliListItem(
                aid = null,
                bvid = media.bvid,
                cid = null,
                title = media.title.orEmpty(),
                cover = media.cover,
                subtitle = media.owner?.name,
                durationSeconds = media.duration
            )
        }
        favoritePage += 1
        _uiState.update {
            val merged = if (append) it.items + items else items
            it.copy(
                isLoading = false,
                items = merged,
                canLoadMore = page.hasMore
            )
        }
    }

    private fun handleHistoryResult(
        page: BiliHistoryPage?,
        error: String?,
        append: Boolean = false
    ) {
        if (page == null) {
            _uiState.update { it.copy(isLoading = false, message = error ?: "加载失败") }
            return
        }
        historyCursor = page.cursor
        val items = page.items.map { item ->
            BiliListItem(
                aid = item.history?.oid,
                bvid = item.history?.bvid,
                cid = item.history?.cid,
                title = item.title.orEmpty(),
                cover = item.cover,
                subtitle = item.authorName,
                durationSeconds = item.duration?.toInt()
            )
        }
        _uiState.update {
            val merged = if (append) it.items + items else items
            it.copy(
                isLoading = false,
                items = merged,
                canLoadMore = page.cursor != null && items.isNotEmpty()
            )
        }
    }

    private fun BiliItem.toListItem(): BiliListItem {
        return BiliListItem(
            aid = aid,
            bvid = bvid,
            cid = cid,
            title = title.orEmpty(),
            cover = cover,
            subtitle = owner?.name,
            durationSeconds = duration
        )
    }
}
