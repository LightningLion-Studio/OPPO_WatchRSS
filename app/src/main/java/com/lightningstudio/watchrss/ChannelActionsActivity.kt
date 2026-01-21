package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.heytap.wearable.support.widget.HeyDialog
import com.lightningstudio.watchrss.data.rss.BuiltinChannelType
import com.lightningstudio.watchrss.ui.screen.ActionDialogScreen
import com.lightningstudio.watchrss.ui.screen.ActionItem
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.ChannelActionsViewModel

class ChannelActionsActivity : BaseHeytapActivity() {
    private val viewModel: ChannelActionsViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        if (!viewModel.isValid()) {
            finish()
            return
        }

        val quick = intent.getBooleanExtra(EXTRA_QUICK, false)
        setContent {
            WatchRSSTheme {
                val channel by viewModel.channel.collectAsState()
                val isValid = channel != null
                val pinLabel = if (channel?.isPinned == true) "取消置顶" else "置顶"
                val isBuiltin = channel?.let { BuiltinChannelType.fromUrl(it.url) != null } ?: false
                val canMarkRead = channel?.let { it.unreadCount > 0 && !isBuiltin } ?: false

                val items = buildList {
                    add(
                        ActionItem(
                            label = "移动到顶部",
                            enabled = isValid,
                            onClick = {
                                viewModel.moveToTop()
                                finish()
                            }
                        )
                    )
                    add(
                        ActionItem(
                            label = pinLabel,
                            enabled = isValid,
                            onClick = {
                                viewModel.togglePinned()
                                finish()
                            }
                        )
                    )
                    add(
                        ActionItem(
                            label = "标记已读",
                            enabled = isValid && canMarkRead,
                            onClick = {
                                viewModel.markRead()
                                finish()
                            }
                        )
                    )
                    if (!quick) {
                        add(
                            ActionItem(
                                label = "删除",
                                enabled = isValid,
                                onClick = { confirmDelete() }
                            )
                        )
                    }
                    add(
                        ActionItem(
                            label = "取消",
                            onClick = { finish() }
                        )
                    )
                }

                ActionDialogScreen(items = items)
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
        const val EXTRA_CHANNEL_ID = "channelId"
        const val EXTRA_QUICK = "quick"
    }
}
