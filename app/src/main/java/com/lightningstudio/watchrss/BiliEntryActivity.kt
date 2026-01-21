package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.compose.setContent
import com.lightningstudio.watchrss.ui.screen.PlatformEntryScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class BiliEntryActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                PlatformEntryScreen(
                    title = "B站",
                    message = "B站内容接入准备中"
                )
            }
        }
    }
}
