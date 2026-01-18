package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightningstudio.watchrss.data.rss.RssRepository
import com.lightningstudio.watchrss.data.rss.SavedState
import com.lightningstudio.watchrss.data.settings.SettingsRepository
import com.lightningstudio.watchrss.data.settings.DEFAULT_READING_FONT_SIZE_SP
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RssRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val itemId: Long = savedStateHandle["itemId"] ?: 0L

    val item = repository.observeItem(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val savedState = repository.observeSavedState(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedState(false, false))

    val offlineMedia = repository.observeOfflineMedia(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val readingThemeDark = settingsRepository.readingThemeDark
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val readingFontSizeSp = settingsRepository.readingFontSizeSp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_READING_FONT_SIZE_SP)

    val detailProgressIndicatorEnabled = settingsRepository.detailProgressIndicatorEnabled

    init {
        if (itemId > 0L) {
            viewModelScope.launch {
                repository.markItemRead(itemId)
            }
        }
    }

    fun toggleFavorite() {
        if (itemId <= 0L) return
        viewModelScope.launch {
            repository.toggleFavorite(itemId)
        }
    }

    fun toggleLike() {
        if (itemId <= 0L) return
        viewModelScope.launch {
            repository.toggleLike(itemId)
        }
    }

    fun updateReadingProgress(progress: Float) {
        if (itemId <= 0L) return
        viewModelScope.launch {
            repository.updateItemReadingProgress(itemId, progress)
        }
    }
}
