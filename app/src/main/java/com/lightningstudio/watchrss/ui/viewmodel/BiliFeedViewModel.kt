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
            _uiState.update { it.copy(isLoggedIn = true, isRefreshing = true, message = null) }
            val result = repository.fetchFeed()
            if (result.isSuccess) {
                val page = result.data
                updateFeed(page)
            } else {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        message = result.message ?: "获取推荐失败"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateFeed(page: BiliFeedPage?) {
        val refreshedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isRefreshing = false,
                items = page?.items ?: emptyList(),
                feedSource = page?.source,
                lastRefreshAt = refreshedAt,
                message = if (page?.items.isNullOrEmpty()) "暂无推荐内容" else null
            )
        }
    }
}
