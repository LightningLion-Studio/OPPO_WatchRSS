package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightningstudio.watchrss.data.douyin.DouyinErrorCodes
import com.lightningstudio.watchrss.data.douyin.DouyinRepository
import com.lightningstudio.watchrss.data.douyin.formatDouyinError
import com.lightningstudio.watchrss.sdk.douyin.DouyinVideo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DouyinFeedUiState(
    val isLoggedIn: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<DouyinVideo> = emptyList(),
    val message: String? = null
)

class DouyinFeedViewModel(
    private val repository: DouyinRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DouyinFeedUiState())
    val uiState: StateFlow<DouyinFeedUiState> = _uiState

    init {
        viewModelScope.launch {
            val loggedIn = repository.isLoggedIn()
            _uiState.update { it.copy(isLoggedIn = loggedIn) }
            if (loggedIn) {
                refresh()
            }
        }
    }

    fun applyCookie(rawCookie: String) {
        viewModelScope.launch {
            val result = repository.applyCookieHeader(rawCookie)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoggedIn = true, message = null) }
                refresh()
            } else {
                _uiState.update {
                    it.copy(message = result.exceptionOrNull()?.message ?: "登录失败")
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = null) }
            val result = repository.fetchFeed()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isRefreshing = false, items = result.data.orEmpty(), message = null)
                }
            } else {
                if (result.code == DouyinErrorCodes.NOT_LOGGED_IN) {
                    repository.clearCookie()
                    _uiState.update {
                        it.copy(isRefreshing = false, isLoggedIn = false, items = emptyList())
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            message = formatDouyinError(result.code, result.message)
                        )
                    }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
