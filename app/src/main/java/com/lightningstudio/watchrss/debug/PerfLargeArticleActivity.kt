package com.lightningstudio.watchrss.debug

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.lightningstudio.watchrss.BaseHeytapActivity
import com.lightningstudio.watchrss.BuildConfig
import com.lightningstudio.watchrss.ui.screen.rss.DetailContent
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.util.ContentBlock
import com.lightningstudio.watchrss.ui.util.buildContentBlocks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PerfLargeArticleActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            finish()
            return
        }
        setupSystemBars()
        PerformanceMonitor.setScenario(this, "perf_large_article")

        setContent {
            WatchRSSTheme {
                val item by produceState(initialValue = null as com.lightningstudio.watchrss.data.rss.RssItem?) {
                    value = withContext(Dispatchers.Default) {
                        PerfDataFactory.buildLargeArticle()
                    }
                }
                val contentBlocks by produceState<List<ContentBlock>>(initialValue = emptyList(), item) {
                    val current = item ?: return@produceState
                    value = withContext(Dispatchers.Default) {
                        buildContentBlocks(current)
                    }
                }

                DetailContent(
                    item = item,
                    contentBlocks = contentBlocks,
                    offlineMedia = emptyMap(),
                    hasOfflineFailures = false,
                    isFavorite = false,
                    isWatchLater = false,
                    readingThemeDark = true,
                    readingFontSizeSp = 18,
                    progressIndicatorEnabled = true,
                    shareUseSystem = true,
                    onToggleFavorite = {},
                    onRetryOfflineMedia = {},
                    onSaveReadingProgress = {},
                    onBack = { _, _, _ -> finish() }
                )
            }
        }
    }
}
