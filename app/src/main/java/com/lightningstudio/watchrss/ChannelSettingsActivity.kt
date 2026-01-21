package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.heytap.wearable.support.widget.HeyDialog
import com.lightningstudio.watchrss.data.rss.BuiltinChannelType
import com.lightningstudio.watchrss.ui.screen.rss.ChannelSettingsScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.ChannelDetailViewModel

class ChannelSettingsActivity : BaseHeytapActivity() {
    private val viewModel: ChannelDetailViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                val channel by viewModel.channel.collectAsState()
                val isBuiltin = channel?.let { BuiltinChannelType.fromUrl(it.url) != null } ?: false
                val showOriginalContent = channel != null && !isBuiltin
                val originalContentEnabled = channel?.useOriginalContent ?: false
                val deleteEnabled = channel != null

                ChannelSettingsScreen(
                    showOriginalContent = showOriginalContent,
                    originalContentEnabled = originalContentEnabled,
                    onToggleOriginalContent = {
                        if (showOriginalContent) {
                            viewModel.setOriginalContentEnabled(!originalContentEnabled)
                        }
                    },
                    deleteEnabled = deleteEnabled,
                    onDelete = {
                        if (deleteEnabled) {
                            confirmDelete()
                        }
                    }
                )
            }
        }
    }

    private fun confirmDelete() {
        HeyDialog.HeyBuilder(this)
            .setTitle("删除频道")
            .setMessage("删除后将移除本地缓存")
            .setPositiveButton("删除") { _ ->
                viewModel.delete()
                finish()
            }
            .setNegativeButton("取消") { _ -> }
            .create()
            .show()
    }

    companion object {
        const val EXTRA_CHANNEL_ID = ChannelDetailActivity.EXTRA_CHANNEL_ID
    }
}
