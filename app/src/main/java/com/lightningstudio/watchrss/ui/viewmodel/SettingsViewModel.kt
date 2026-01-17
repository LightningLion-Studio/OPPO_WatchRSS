package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightningstudio.watchrss.data.rss.RssRepository
import com.lightningstudio.watchrss.data.settings.DEFAULT_CACHE_LIMIT_MB
import com.lightningstudio.watchrss.data.settings.MB_BYTES
import com.lightningstudio.watchrss.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val rssRepository: RssRepository
) : ViewModel() {
    val cacheLimitMb: StateFlow<Long> = settingsRepository.cacheLimitBytes
        .map { it / MB_BYTES }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_CACHE_LIMIT_MB)

    val cacheUsageMb: StateFlow<Long> = rssRepository.observeCacheUsageBytes()
        .map { it / MB_BYTES }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun updateCacheLimitMb(value: Long) {
        viewModelScope.launch {
            settingsRepository.setCacheLimitBytes(value * MB_BYTES)
            rssRepository.trimCacheToLimit()
        }
    }
}
