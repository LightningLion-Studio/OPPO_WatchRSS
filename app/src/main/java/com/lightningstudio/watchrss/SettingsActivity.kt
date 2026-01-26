package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.content.Intent
import com.lightningstudio.watchrss.BuildConfig
import com.lightningstudio.watchrss.debug.PerfLargeArticleActivity
import com.lightningstudio.watchrss.debug.PerfLargeListActivity
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
                    shareUseSystem = viewModel.shareUseSystem,
                    readingFontSizeSp = viewModel.readingFontSizeSp,
                    showPerformanceTools = BuildConfig.DEBUG,
                    onSelectCacheLimit = viewModel::updateCacheLimitMb,
                    onToggleReadingTheme = viewModel::toggleReadingTheme,
                    onToggleShareMode = viewModel::toggleShareUseSystem,
                    onSelectFontSize = viewModel::updateReadingFontSizeSp,
                    onOpenPerfLargeList = {
                        startActivity(Intent(this, PerfLargeListActivity::class.java))
                    },
                    onOpenPerfLargeArticle = {
                        startActivity(Intent(this, PerfLargeArticleActivity::class.java))
                    }
                )
            }
        }
    }
}
