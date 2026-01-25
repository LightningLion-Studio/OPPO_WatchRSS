package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightningstudio.watchrss.data.bili.BiliRepository
import com.lightningstudio.watchrss.sdk.bili.BiliFeedPage
import com.lightningstudio.watchrss.sdk.bili.BiliFeedSource
import com.lightningstudio.watchrss.sdk.bili.BiliItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BiliFeedUiState(
    val isLoggedIn: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<BiliItem> = emptyList(),
    val feedSource: BiliFeedSource? = null,
    val lastRefreshAt: Long? = null,
    val message: String? = null
)

class BiliFeedViewModel(
    private val repository: BiliRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BiliFeedUiState())
    val uiState: StateFlow<BiliFeedUiState> = _uiState

    init {
        refreshLoginState()
    }

    fun refreshLoginState() {
        viewModelScope.launch {
            val loggedIn = repository.isLoggedIn()
            _uiState.update { it.copy(isLoggedIn = loggedIn) }
            if (loggedIn && _uiState.value.items.isEmpty()) {
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val loggedIn = repository.isLoggedIn()
            if (!loggedIn) {
                _uiState.update {
                    it.copy(
                        isLoggedIn = false,
                        isRefreshing = false,
                        items = emptyList(),
                        feedSource = null,
                        message = "请先登录获取推荐内容"
                    )
                }
                return@launch
            }
            val useCachePrefix = _uiState.value.items.isEmpty()
            val cachedItems = if (useCachePrefix) repository.readFeedCache() else emptyList()
            val cachedPrefix = if (useCachePrefix) cachedItems.take(5) else emptyList()
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    isRefreshing = true,
                    items = if (cachedPrefix.isNotEmpty()) cachedPrefix else it.items,
                    feedSource = if (cachedPrefix.isNotEmpty()) null else it.feedSource,
                    message = null
                )
            }
            val result = repository.fetchFeed()
            if (result.isSuccess) {
                val page = result.data
                val freshItems = page?.items.orEmpty()
                val merged = if (cachedPrefix.isNotEmpty()) {
                    mergeCachedAndFresh(cachedPrefix, freshItems)
                } else {
                    freshItems
                }
                repository.writeFeedCache(freshItems)
                updateFeed(page, merged)
            } else {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        items = if (cachedItems.isNotEmpty()) cachedItems else it.items,
                        message = result.message ?: "获取推荐失败"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateFeed(page: BiliFeedPage?, items: List<BiliItem>) {
        val refreshedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isRefreshing = false,
                items = items,
                feedSource = page?.source,
                lastRefreshAt = refreshedAt,
                message = if (items.isEmpty()) "暂无推荐内容" else null
            )
        }
    }

    private fun mergeCachedAndFresh(
        cachedPrefix: List<BiliItem>,
        freshItems: List<BiliItem>
    ): List<BiliItem> {
        if (cachedPrefix.isEmpty()) return freshItems
        val seen = cachedPrefix.map { itemKey(it) }.toMutableSet()
        val filteredFresh = freshItems.filter { seen.add(itemKey(it)) }
        return cachedPrefix + filteredFresh
    }

    private fun itemKey(item: BiliItem): String {
        return when {
            !item.bvid.isNullOrBlank() -> "bvid:${item.bvid}"
            item.aid != null -> "aid:${item.aid}"
            else -> "title:${item.title.orEmpty()}"
        }
    }
}
