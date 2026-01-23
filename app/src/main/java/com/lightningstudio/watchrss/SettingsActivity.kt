package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.lightningstudio.watchrss.ui.screen.rss.SettingsScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.SettingsViewModel

class SettingsActivity : BaseHeytapActivity() {
    private val viewModel: SettingsViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                SettingsScreen(
                    cacheLimitMb = viewModel.cacheLimitMb,
                    cacheUsageMb = viewModel.cacheUsageMb,
                    readingThemeDark = viewModel.readingThemeDark,
                    detailProgressIndicatorEnabled = viewModel.detailProgressIndicatorEnabled,
                    shareUseSystem = viewModel.shareUseSystem,
                    readingFontSizeSp = viewModel.readingFontSizeSp,
                    onSelectCacheLimit = viewModel::updateCacheLimitMb,
                    onToggleReadingTheme = viewModel::toggleReadingTheme,
                    onToggleProgressIndicator = viewModel::toggleDetailProgressIndicator,
                    onToggleShareMode = viewModel::toggleShareUseSystem,
                    onSelectFontSize = viewModel::updateReadingFontSizeSp
                )
            }
        }
    }
}
