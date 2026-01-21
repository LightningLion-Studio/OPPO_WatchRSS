package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.compose.setContent
import com.lightningstudio.watchrss.ui.screen.InfoScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class InfoActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""

        setContent {
            WatchRSSTheme {
                InfoScreen(title = title, content = content)
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
    }
}
