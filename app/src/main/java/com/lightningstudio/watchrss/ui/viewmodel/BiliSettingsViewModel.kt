package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightningstudio.watchrss.data.bili.BiliRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BiliSettingsUiState(
    val isLoggedIn: Boolean = false,
    val message: String? = null
)

class BiliSettingsViewModel(
    private val repository: BiliRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BiliSettingsUiState())
    val uiState: StateFlow<BiliSettingsUiState> = _uiState

    fun refreshLoginState() {
        viewModelScope.launch {
            val loggedIn = repository.isLoggedIn()
            _uiState.update { it.copy(isLoggedIn = loggedIn) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearAccount()
            _uiState.update { it.copy(isLoggedIn = false, message = "已退出登录") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
