package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lightningstudio.watchrss.ui.screen.rss.ChannelDetailScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.ChannelDetailViewModel

class ChannelDetailActivity : BaseHeytapActivity() {
    private val viewModel: ChannelDetailViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        val channelId = intent.getLongExtra(EXTRA_CHANNEL_ID, 0L)

        setContent {
            WatchRSSTheme {
                val channel by viewModel.channel.collectAsState()
                ChannelDetailScreen(
                    channel = channel,
                    onOpenSettings = {
                        if (channelId <= 0L) return@ChannelDetailScreen
                        val intent = Intent(this, ChannelSettingsActivity::class.java)
                        intent.putExtra(ChannelSettingsActivity.EXTRA_CHANNEL_ID, channelId)
                        startActivity(intent)
                    },
                    onMarkRead = viewModel::markRead
                )
            }
        }
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "channelId"
    }
}
