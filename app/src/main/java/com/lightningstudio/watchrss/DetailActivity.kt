package com.lightningstudio.watchrss

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.data.rss.OfflineMedia
import com.lightningstudio.watchrss.data.rss.RssItem
import com.lightningstudio.watchrss.data.settings.DEFAULT_READING_FONT_SIZE_SP
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.DetailViewModel
import com.lightningstudio.watchrss.ui.util.ContentBlock
import com.lightningstudio.watchrss.ui.util.RssContentParser
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import com.lightningstudio.watchrss.ui.util.TextStyle
import kotlinx.coroutines.launch
import java.io.File

class DetailActivity : BaseHeytapActivity() {
    private val viewModel: DetailViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private lateinit var titleView: HeyTextView
    private lateinit var contentContainer: LinearLayout
    private lateinit var openButton: HeyButton
    private lateinit var shareButton: ImageButton
    private lateinit var favoriteButton: ImageButton

    private var currentTitle: String = ""
    private var currentLink: String? = null
    private var currentThemeDark: Boolean = true
    private var currentFontSizeSp: Int = DEFAULT_READING_FONT_SIZE_SP
    private var offlineMedia: Map<String, OfflineMedia> = emptyMap()
    private var isFavorite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_detail)

        titleView = findViewById(R.id.text_title)
        contentContainer = findViewById(R.id.content_container)
        openButton = findViewById(R.id.button_open)
        shareButton = findViewById(R.id.button_share)
        favoriteButton = findViewById(R.id.button_favorite)

        openButton.setOnClickListener {
            val link = it.tag as? String
            if (!link.isNullOrBlank()) {
                openLink(link)
            }
        }

        shareButton.setOnClickListener {
            shareCurrent()
        }

        favoriteButton.setOnClickListener { viewModel.toggleFavorite() }

        val blockSpacing = resources.getDimensionPixelSize(R.dimen.detail_block_spacing)
        val safePadding = resources.getDimensionPixelSize(R.dimen.watch_safe_padding)
        val maxImageWidth = (resources.displayMetrics.widthPixels - safePadding * 2).coerceAtLeast(1)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.item.collect { item ->
                        if (item == null) {
                            titleView.text = "加载中..."
                            contentContainer.removeAllViews()
                            openButton.visibility = View.GONE
                            return@collect
                        }
                        currentTitle = item.title
                        currentLink = item.link
                        titleView.text = item.title
                        renderContent(item, blockSpacing, maxImageWidth)
                        updateLinkButton(item.link)
                    }
                }
                launch {
                    viewModel.savedState.collect { state ->
                        isFavorite = state.isFavorite
                        updateActionIconTints()
                    }
                }
                launch {
                    viewModel.offlineMedia.collect { list ->
                        offlineMedia = list.associateBy { it.originUrl }
                        renderContentIfReady(blockSpacing, maxImageWidth)
                    }
                }
                launch {
                    viewModel.readingThemeDark.collect { isDark ->
                        currentThemeDark = isDark
                        applyReadingTheme()
                        renderContentIfReady(blockSpacing, maxImageWidth)
                    }
                }
                launch {
                    viewModel.readingFontSizeSp.collect { sizeSp ->
                        currentFontSizeSp = sizeSp
                        renderContentIfReady(blockSpacing, maxImageWidth)
                    }
                }
            }
        }
    }

    private fun renderContentIfReady(blockSpacing: Int, maxImageWidth: Int) {
        val item = viewModel.item.value ?: return
        renderContent(item, blockSpacing, maxImageWidth)
    }

    private fun applyReadingTheme() {
        val root = findViewById<View>(R.id.detail_root)
        root.setBackgroundColor(if (currentThemeDark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        titleView.setTextColor(if (currentThemeDark) 0xFFFFFFFF.toInt() else 0xFF111111.toInt())
        updateActionIconTints()
    }

    private fun updateActionIconTints() {
        val normalColor = if (currentThemeDark) 0xFFFFFFFF.toInt() else 0xFF111111.toInt()
        val activeColor = resources.getColor(R.color.oppo_orange, theme)
        favoriteButton.setColorFilter(if (isFavorite) activeColor else normalColor)
        shareButton.setColorFilter(normalColor)
    }

    private fun updateLinkButton(link: String?) {
        if (link.isNullOrBlank()) {
            openButton.visibility = View.GONE
            openButton.tag = null
        } else {
            openButton.visibility = View.VISIBLE
            openButton.tag = link
        }
    }

    private fun renderContent(item: RssItem, blockSpacing: Int, maxImageWidth: Int) {
        contentContainer.removeAllViews()
        val raw = item.content ?: item.description
        if (raw.isNullOrBlank()) {
            contentContainer.addView(createTextView("暂无正文", TextStyle.BODY, blockSpacing))
            return
        }
        val blocks = RssContentParser.parse(raw).toMutableList()
        val itemImage = item.imageUrl?.takeIf { it.isNotBlank() }
        if (itemImage != null && blocks.none { it is ContentBlock.Image && it.url == itemImage }) {
            blocks.add(ContentBlock.Image(itemImage, null))
        }
        val itemVideo = item.videoUrl?.takeIf { it.isNotBlank() }
        if (itemVideo != null && blocks.none { it is ContentBlock.Video && it.url == itemVideo }) {
            blocks.add(ContentBlock.Video(itemVideo, null))
        }
        if (blocks.isEmpty()) {
            contentContainer.addView(createTextView("暂无正文", TextStyle.BODY, blockSpacing))
            return
        }
        blocks.forEachIndexed { index, block ->
            when (block) {
                is ContentBlock.Text -> {
                    contentContainer.addView(
                        createTextView(block.text, block.style, if (index == 0) 0 else blockSpacing)
                    )
                }
                is ContentBlock.Image -> {
                    contentContainer.addView(
                        createImageView(block.url, block.alt, if (index == 0) 0 else blockSpacing, maxImageWidth)
                    )
                }
                is ContentBlock.Video -> {
                    contentContainer.addView(
                        createVideoView(block.url, block.poster, if (index == 0) 0 else blockSpacing, maxImageWidth)
                    )
                }
            }
        }
    }

    private fun createTextView(text: String, style: TextStyle, topMargin: Int): HeyTextView {
        val view = layoutInflater.inflate(R.layout.item_detail_text, null, false) as HeyTextView
        view.text = text
        val baseDimen = when (style) {
            TextStyle.TITLE -> R.dimen.detail_title_text_size
            TextStyle.SUBTITLE -> R.dimen.detail_subtitle_text_size
            TextStyle.QUOTE -> R.dimen.detail_body_text_size
            TextStyle.CODE -> R.dimen.detail_body_text_size
            TextStyle.BODY -> R.dimen.detail_body_text_size
        }
        val finalSize = adjustedTextSize(baseDimen)
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalSize)
        view.setLineSpacing(0f, 1.2f)
        if (style == TextStyle.QUOTE) {
            view.alpha = 0.8f
        }
        view.setTextColor(if (currentThemeDark) 0xFFFFFFFF.toInt() else 0xFF111111.toInt())
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
        val resolvedUrl = resolveMediaUrl(url)
        RssImageLoader.load(this, resolvedUrl, view, lifecycleScope, maxImageWidth)
        return view
    }

    private fun createVideoView(
        url: String,
        poster: String?,
        topMargin: Int,
        maxImageWidth: Int
    ): View {
        val view = layoutInflater.inflate(R.layout.item_detail_video, null, false)
        val cover = view.findViewById<ImageView>(R.id.image_video_cover)
        val resolvedPoster = poster?.let { resolveMediaUrl(it) }
        if (!resolvedPoster.isNullOrBlank()) {
            RssImageLoader.load(this, resolvedPoster, cover, lifecycleScope, maxImageWidth)
        } else {
            cover.setImageResource(android.R.color.transparent)
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = topMargin
        view.layoutParams = params
        view.setOnClickListener { openLink(resolveMediaUrl(url)) }
        return view
    }

    private fun resolveMediaUrl(url: String): String {
        val local = offlineMedia[url]?.localPath
        return if (!local.isNullOrBlank()) local else url
    }

    private fun adjustedTextSize(baseDimenRes: Int): Float {
        val basePx = resources.getDimension(baseDimenRes)
        val deltaSp = (currentFontSizeSp - DEFAULT_READING_FONT_SIZE_SP).toFloat()
        val deltaPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            deltaSp,
            resources.displayMetrics
        )
        return (basePx + deltaPx).coerceAtLeast(10f)
    }

    private fun shareCurrent() {
        if (currentTitle.isBlank()) return
        val text = if (!currentLink.isNullOrBlank()) {
            "$currentTitle\n$currentLink"
        } else {
            currentTitle
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "分享"))
    }

    private fun openLink(link: String) {
        val uri = if (link.startsWith("/")) {
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", File(link))
        } else {
            Uri.parse(link)
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    companion object {
        const val EXTRA_ITEM_ID = "itemId"
    }
}
