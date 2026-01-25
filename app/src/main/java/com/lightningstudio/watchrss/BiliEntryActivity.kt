package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.compose.setContent
import com.lightningstudio.watchrss.ui.screen.bili.BiliEntryNavGraph
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class BiliEntryActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                val repository = (application as WatchRssApplication).container.biliRepository
                val rssRepository = (application as WatchRssApplication).container.rssRepository
                BiliEntryNavGraph(repository = repository, rssRepository = rssRepository)
            }
        }
    }
}
