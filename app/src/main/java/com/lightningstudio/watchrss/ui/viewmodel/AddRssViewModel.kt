package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightningstudio.watchrss.data.rss.RssRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddRssUiState(
    val url: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdChannelId: Long? = null
)

class AddRssViewModel(private val repository: RssRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddRssUiState())
    val uiState: StateFlow<AddRssUiState> = _uiState.asStateFlow()

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(url = url, errorMessage = null)
    }

    fun submit() {
        val url = _uiState.value.url
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入 RSS 地址")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            val result = repository.addChannel(url)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    isSubmitting = false,
                    createdChannelId = result.getOrNull()?.id
                )
            } else {
                _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "添加失败"
                )
            }
        }
    }

    fun consumeCreatedChannel() {
        _uiState.value = _uiState.value.copy(createdChannelId = null)
    }
}
