package com.lightningstudio.watchrss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.ui.util.QrCodeGenerator

class ShareQrActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_share_qr)

        val qrView = findViewById<ImageView>(R.id.image_share_qr)
        val closeButton = findViewById<HeyButton>(R.id.button_share_qr_close)

        val link = intent.getStringExtra(EXTRA_LINK).orEmpty().trim()

        closeButton.setOnClickListener { finish() }

        if (link.isEmpty()) {
            HeyToast.showToast(this, "暂无可分享链接", android.widget.Toast.LENGTH_SHORT)
            finish()
            return
        }

        val safePadding = resources.getDimensionPixelSize(R.dimen.watch_safe_padding)
        val framePadding = (qrView.parent as? android.view.View)?.let { view ->
            view.paddingLeft + view.paddingRight
        } ?: (safePadding * 2)
        val size = (resources.displayMetrics.widthPixels - framePadding).coerceAtLeast(1)
        qrView.layoutParams = qrView.layoutParams.apply {
            width = size
            height = size
        }
        val bitmap = QrCodeGenerator.create(link, size)
        if (bitmap == null) {
            HeyToast.showToast(this, "二维码生成失败", android.widget.Toast.LENGTH_SHORT)
            finish()
            return
        }
        qrView.setImageBitmap(bitmap)
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
