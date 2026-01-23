package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.compose.setContent
import com.lightningstudio.watchrss.ui.screen.PlatformEntryScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class DouyinEntryActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                PlatformEntryScreen(
                    title = "抖音",
                    message = "抖音内容接入准备中"
                )
            }
        }
    }
}
