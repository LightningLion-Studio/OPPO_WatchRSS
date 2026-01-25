package com.lightningstudio.watchrss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.ui.screen.bili.BiliDetailScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.BiliDetailViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliViewModelFactory

class BiliDetailActivity : BaseHeytapActivity() {
    private val repository by lazy { (application as WatchRssApplication).container.biliRepository }
    private val viewModel: BiliDetailViewModel by viewModels {
        BiliViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                val context = LocalContext.current
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.message) {
                    val message = uiState.message
                    if (!message.isNullOrBlank()) {
                        HeyToast.showToast(context, message, android.widget.Toast.LENGTH_SHORT)
                        viewModel.clearMessage()
                    }
                }

                BiliDetailScreen(
                    uiState = uiState,
                    onPlayClick = {
                        val cid = viewModel.selectedCid()
                        val item = uiState.detail?.item
                        context.startActivity(
                            BiliPlayerActivity.createIntent(context, item?.aid, item?.bvid, cid)
                        )
                    },
                    onSelectPage = viewModel::selectPage,
                    onLike = viewModel::like,
                    onCoin = viewModel::coin,
                    onFavorite = viewModel::favorite,
                    onShare = {
                        val item = uiState.detail?.item
                        val link = repository.shareLink(item?.bvid, item?.aid)
                        shareLink(context, item?.title, link)
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_AID = "aid"
        private const val EXTRA_BVID = "bvid"
        private const val EXTRA_CID = "cid"

        fun createIntent(context: Context, aid: Long?, bvid: String?, cid: Long?): Intent {
            return Intent(context, BiliDetailActivity::class.java).apply {
                putExtra(EXTRA_AID, aid?.toString().orEmpty())
                putExtra(EXTRA_BVID, bvid.orEmpty())
                putExtra(EXTRA_CID, cid?.toString().orEmpty())
            }
        }
    }
}

private fun shareLink(context: Context, title: String?, link: String?) {
    val safeTitle = title?.trim().orEmpty()
    val safeLink = link?.trim().orEmpty()
    if (safeTitle.isEmpty() && safeLink.isEmpty()) return
    val text = when {
        safeTitle.isNotEmpty() && safeLink.isNotEmpty() -> "$safeTitle\n$safeLink"
        safeTitle.isNotEmpty() -> safeTitle
        else -> safeLink
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}
