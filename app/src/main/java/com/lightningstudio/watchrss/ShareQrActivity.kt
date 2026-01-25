package com.lightningstudio.watchrss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.ui.screen.ShareQrScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class ShareQrActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        val link = intent.getStringExtra(EXTRA_LINK).orEmpty().trim()

        if (link.isEmpty()) {
            HeyToast.showToast(this, "暂无可分享链接", android.widget.Toast.LENGTH_SHORT)
            finish()
            return
        }

        setContent {
            WatchRSSTheme {
                ShareQrScreen(
                    link = link,
                    onQrError = {
                        HeyToast.showToast(this, "二维码生成失败", android.widget.Toast.LENGTH_SHORT)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_LINK = "extra_link"

        fun createIntent(context: Context, title: String?, link: String): Intent {
            return Intent(context, ShareQrActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_LINK, link)
            }
        }
    }
}
