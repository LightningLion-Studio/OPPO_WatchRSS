package com.lightningstudio.watchrss

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.DetailViewModel
import com.lightningstudio.watchrss.ui.util.ContentBlock
import com.lightningstudio.watchrss.ui.util.RssContentParser
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import com.lightningstudio.watchrss.ui.util.TextStyle
import kotlinx.coroutines.launch

class DetailActivity : BaseHeytapActivity() {
    private val viewModel: DetailViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_detail)

        val openButton = findViewById<HeyButton>(R.id.button_open)
        val titleView = findViewById<HeyTextView>(R.id.text_title)
        val contentContainer = findViewById<LinearLayout>(R.id.content_container)
        val blockSpacing = resources.getDimensionPixelSize(R.dimen.detail_block_spacing)
        val safePadding = resources.getDimensionPixelSize(R.dimen.watch_safe_padding)
        val maxImageWidth = (resources.displayMetrics.widthPixels - safePadding * 2).coerceAtLeast(1)

        openButton.setOnClickListener {
            val link = it.tag as? String
            if (!link.isNullOrBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.item.collect { item ->
                    if (item == null) {
                        titleView.text = "加载中..."
                        contentContainer.removeAllViews()
                        openButton.visibility = View.GONE
                        return@collect
                    }

                    titleView.text = item.title
                    val content = item.content?.takeIf { it.isNotBlank() }
                        ?: item.description?.takeIf { it.isNotBlank() }
                    renderContent(contentContainer, content, blockSpacing, maxImageWidth)

                    if (item.link.isNullOrBlank()) {
                        openButton.visibility = View.GONE
                        openButton.tag = null
                    } else {
                        openButton.visibility = View.VISIBLE
                        openButton.tag = item.link
                    }
                }
            }
        }
    }

    private fun renderContent(
        container: LinearLayout,
        raw: String?,
        blockSpacing: Int,
        maxImageWidth: Int
    ) {
        container.removeAllViews()
        if (raw.isNullOrBlank()) {
            container.addView(createTextView("暂无正文", TextStyle.BODY, blockSpacing))
            return
        }
        val blocks = RssContentParser.parse(raw)
        if (blocks.isEmpty()) {
            container.addView(createTextView("暂无正文", TextStyle.BODY, blockSpacing))
            return
        }
        blocks.forEachIndexed { index, block ->
            when (block) {
                is ContentBlock.Text -> {
                    container.addView(
                        createTextView(block.text, block.style, if (index == 0) 0 else blockSpacing)
                    )
                }
                is ContentBlock.Image -> {
                    container.addView(
                        createImageView(block.url, block.alt, if (index == 0) 0 else blockSpacing, maxImageWidth)
                    )
                }
            }
        }
    }

    private fun createTextView(text: String, style: TextStyle, topMargin: Int): HeyTextView {
        val view = layoutInflater.inflate(R.layout.item_detail_text, null, false) as HeyTextView
        view.text = text
        view.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(
                when (style) {
                    TextStyle.TITLE -> R.dimen.detail_title_text_size
                    TextStyle.SUBTITLE -> R.dimen.detail_subtitle_text_size
                    TextStyle.QUOTE -> R.dimen.detail_body_text_size
                    TextStyle.CODE -> R.dimen.detail_body_text_size
                    TextStyle.BODY -> R.dimen.detail_body_text_size
                }
            )
        )
        if (style == TextStyle.QUOTE) {
            view.alpha = 0.8f
        }
        view.setLineSpacing(0f, 1.2f)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = topMargin
        view.layoutParams = params
        return view
    }

    private fun createImageView(
        url: String,
        alt: String?,
        topMargin: Int,
        maxImageWidth: Int
    ): ImageView {
        val view = ImageView(this)
        view.adjustViewBounds = true
        view.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        view.contentDescription = alt ?: ""
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = topMargin
        view.layoutParams = params
        RssImageLoader.load(url, view, lifecycleScope, maxImageWidth)
        return view
    }

    companion object {
        const val EXTRA_ITEM_ID = "itemId"
    }
}
