package com.lightningstudio.watchrss

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.lightningstudio.watchrss.ui.screen.rss.AddRssScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.AddRssViewModel
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory

class AddRssActivity : BaseHeytapActivity() {
    private val viewModel: AddRssViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        val presetUrl = intent.getStringExtra(EXTRA_URL)?.trim().orEmpty()
        if (presetUrl.isNotEmpty()) {
            viewModel.updateUrl(presetUrl)
        }

        setContent {
            WatchRSSTheme {
                AddRssScreen(
                    uiState = viewModel.uiState,
                    onUrlChange = viewModel::updateUrl,
                    onSubmit = viewModel::submit,
                    onConfirm = viewModel::confirmAdd,
                    onBack = { finish() },
                    onBackToInput = viewModel::backToInput,
                    onOpenExisting = { existing ->
                        openChannel(existing.url, existing.id)
                    },
                    onChannelAdded = { url, channelId ->
                        openChannel(url, channelId)
                    },
                    onConsumed = viewModel::consumeCreatedChannel,
                    onClearError = viewModel::clearError
                )
            }
        }
    }

    private fun openChannel(url: String?, channelId: Long) {
        val builtin = com.lightningstudio.watchrss.data.rss.BuiltinChannelType.fromUrl(url)
            ?: builtinFromInputUrl(url)
        when (builtin) {
            com.lightningstudio.watchrss.data.rss.BuiltinChannelType.BILI -> {
                startActivity(Intent(this, BiliEntryActivity::class.java))
            }
            com.lightningstudio.watchrss.data.rss.BuiltinChannelType.DOUYIN -> {
                startActivity(Intent(this, DouyinEntryActivity::class.java))
            }
            null -> {
                val intent = Intent(this, FeedActivity::class.java)
                intent.putExtra(FeedActivity.EXTRA_CHANNEL_ID, channelId)
                startActivity(intent)
            }
        }
        finish()
    }

    private fun builtinFromInputUrl(url: String?): com.lightningstudio.watchrss.data.rss.BuiltinChannelType? {
        if (url.isNullOrBlank()) return null
        val host = runCatching { Uri.parse(url).host }.getOrNull()
        return com.lightningstudio.watchrss.data.rss.BuiltinChannelType.fromHost(host)
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}
