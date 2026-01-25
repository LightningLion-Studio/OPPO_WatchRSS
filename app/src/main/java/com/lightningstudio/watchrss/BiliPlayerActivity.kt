package com.lightningstudio.watchrss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lightningstudio.watchrss.ui.screen.bili.BiliPlayerScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.BiliPlayerViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliViewModelFactory

class BiliPlayerActivity : BaseHeytapActivity() {
    private val repository by lazy { (application as WatchRssApplication).container.biliRepository }
    private val viewModel: BiliPlayerViewModel by viewModels {
        BiliViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        val aid = intent.getStringExtra(EXTRA_AID)?.toLongOrNull()
        val bvid = intent.getStringExtra(EXTRA_BVID)
        val link = repository.shareLink(bvid, aid)

        setContent {
            WatchRSSTheme {
                val uiState by viewModel.uiState.collectAsState()
                BiliPlayerScreen(
                    uiState = uiState,
                    onRetry = viewModel::loadPlayUrl,
                    onOpenWeb = {
                        val safeLink = link ?: return@BiliPlayerScreen
                        startActivity(WebViewActivity.createIntent(this, safeLink))
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_AID = "aid"
        private const val EXTRA_BVID = "bvid"
        private const val EXTRA_CID = "cid"

        fun createIntent(context: Context, aid: Long?, bvid: String?, cid: Long?): Intent {
            return Intent(context, BiliPlayerActivity::class.java).apply {
                putExtra(EXTRA_AID, aid?.toString().orEmpty())
                putExtra(EXTRA_BVID, bvid.orEmpty())
                putExtra(EXTRA_CID, cid?.toString().orEmpty())
            }
        }
    }
}
