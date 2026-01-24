package com.lightningstudio.watchrss

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyTextView
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.data.rss.OfflineMedia
import com.lightningstudio.watchrss.data.rss.RssItem
import com.lightningstudio.watchrss.data.settings.DEFAULT_READING_FONT_SIZE_SP
import com.lightningstudio.watchrss.debug.DebugLogBuffer
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.DetailViewModel
import com.lightningstudio.watchrss.ui.util.ContentBlock
import com.lightningstudio.watchrss.ui.util.RssContentParser
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import com.lightningstudio.watchrss.ui.util.TextStyle
import com.lightningstudio.watchrss.ui.widget.ProgressRingView
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

class DetailActivity : BaseHeytapActivity() {
    private val viewModel: DetailViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private lateinit var titleView: HeyTextView
    private lateinit var contentContainer: LinearLayout
    private lateinit var detailScroll: ScrollView
    private lateinit var safeTopSpacer: View
    private lateinit var safeBottomSpacer: View
    private lateinit var openButton: HeyButton
    private lateinit var shareButton: ImageButton
    private lateinit var favoriteButton: ImageButton
    private lateinit var progressRing: ProgressRingView

    private var currentTitle: String = ""
    private var currentLink: String? = null
    private var currentThemeDark: Boolean = true
    private var currentFontSizeSp: Int = DEFAULT_READING_FONT_SIZE_SP
    private var offlineMedia: Map<String, OfflineMedia> = emptyMap()
    private var isFavorite: Boolean = false
    private var progressIndicatorEnabled: Boolean = true
    private var shareUseSystem: Boolean = true
    private var baseSafePadding: Int = 0
    private var fromWatchLater: Boolean = false
    private var reachedBottom: Boolean = false
    private var isWatchLater: Boolean = false
    private var lastSavedProgress: Float = -1f
    private var lastProgressSavedAt: Long = 0L
    private var lastRenderedItemId: Long = -1L
    private var lastRenderedSignature: Int = 0
    private var pendingRestoreItemId: Long = -1L
    private var pendingRestoreProgress: Float = 0f
    private var hasRestoredPosition: Boolean = false
    private var restoreLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContent {
            WatchRSSTheme {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { _ ->
                        val root = layoutInflater.inflate(R.layout.activity_detail, null, false)
                        bindViews(root)
                        root
                    }
                )
            }
        }
    }

    private fun bindViews(root: View) {
        titleView = root.findViewById(R.id.text_title)
        contentContainer = root.findViewById(R.id.content_container)
        detailScroll = root.findViewById(R.id.detail_scroll)
        safeTopSpacer = root.findViewById(R.id.detail_safe_top)
        safeBottomSpacer = root.findViewById(R.id.detail_safe_bottom)
        openButton = root.findViewById(R.id.button_open)
        shareButton = root.findViewById(R.id.button_share)
        favoriteButton = root.findViewById(R.id.button_favorite)
        progressRing = root.findViewById(R.id.detail_progress_ring)
        progressRing.visibility = View.GONE
        baseSafePadding = resources.getDimensionPixelSize(R.dimen.watch_safe_padding)
        fromWatchLater = intent.getBooleanExtra(EXTRA_FROM_WATCH_LATER, false)

        openButton.setOnClickListener {
            val link = it.tag as? String
            if (!link.isNullOrBlank()) {
                openLinkInApp(link)
            }
        }

        shareButton.setOnClickListener {
            if (shareUseSystem) {
                shareCurrent()
            } else {
                showShareQr()
            }
        }

        favoriteButton.setOnClickListener { viewModel.toggleFavorite() }

        val blockSpacing = resources.getDimensionPixelSize(R.dimen.detail_block_spacing)
        val safePadding = resources.getDimensionPixelSize(R.dimen.watch_safe_padding)
        val maxImageWidth = (resources.displayMetrics.widthPixels - safePadding * 2).coerceAtLeast(1)

        detailScroll.setOnScrollChangeListener { _, _, _, _, _ ->
            updateProgressIndicator()
            checkReachedBottom()
        }

        onBackPressedDispatcher.addCallback(this) {
            handleBackPress()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.item.collect { item ->
                        if (item == null) {
                            titleView.text = "加载中..."
                            contentContainer.removeAllViews()
                            openButton.visibility = View.GONE
                            lastRenderedItemId = -1L
                            lastRenderedSignature = 0
                            return@collect
                        }
                        val signature = renderSignature(item)
                        val shouldRender = item.id != lastRenderedItemId || signature != lastRenderedSignature
                        if (!shouldRender) {
                            return@collect
                        }
                        lastRenderedItemId = item.id
                        lastRenderedSignature = signature
                        DebugLogBuffer.log(
                            "orig",
                            "detail itemId=${item.id} contentLen=${item.content?.length ?: 0} descLen=${item.description?.length ?: 0}"
                        )
                        currentTitle = item.title
                        currentLink = item.link
                        reachedBottom = false
                        lastSavedProgress = -1f
                        lastProgressSavedAt = 0L
                        applyTitleText(item.title)
                        renderContentWithRestore(
                            item,
                            blockSpacing,
                            maxImageWidth,
                            progressOverride = item.readingProgress
                        )
                        updateLinkButton(item.link)
                    }
                }
                launch {
                    viewModel.savedState.collect { state ->
                        isFavorite = state.isFavorite
                        isWatchLater = state.isWatchLater
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
                        applyReadingFontSizeToOpenButton()
                        renderContentIfReady(blockSpacing, maxImageWidth)
                    }
                }
                launch {
                    viewModel.detailProgressIndicatorEnabled.collect { enabled ->
                        progressIndicatorEnabled = enabled
                        progressRing.visibility = if (enabled) View.VISIBLE else View.GONE
                        applyProgressPadding(enabled)
                        if (enabled) {
                            postRestoreAndUpdate()
                        }
                    }
                }
                launch {
                    viewModel.shareUseSystem.collect { useSystem ->
                        shareUseSystem = useSystem
                    }
                }
            }
        }
    }

    private fun applyReadingFontSizeToOpenButton() {
        val sizePx = adjustedTextSize(R.dimen.detail_body_text_size)
        openButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
    }

    private fun renderContentIfReady(blockSpacing: Int, maxImageWidth: Int) {
        val item = viewModel.item.value ?: return
        renderContentWithRestore(item, blockSpacing, maxImageWidth, progressOverride = null)
    }

    private fun renderSignature(item: RssItem): Int {
        return listOf(
            item.title,
            item.description,
            item.content,
            item.link,
            item.imageUrl,
            item.audioUrl,
            item.videoUrl,
            item.pubDate
        ).hashCode()
    }

    private fun renderContentWithRestore(
        item: RssItem,
        blockSpacing: Int,
        maxImageWidth: Int,
        progressOverride: Float?
    ) {
        val progress = when {
            progressOverride != null -> progressOverride
            !hasRestoredPosition && pendingRestoreItemId == item.id -> pendingRestoreProgress
            else -> calculateReadingProgress()
        }
        prepareRestore(item.id, progress)
        renderContent(item, blockSpacing, maxImageWidth)
        postRestoreAndUpdate()
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

    private fun applyTitleText(title: String) {
        titleView.text = formatTitleForWidthLimits(title)
    }

    private fun checkReachedBottom() {
        if (reachedBottom) return
        val content = detailScroll.getChildAt(0) ?: return
        val threshold = (resources.displayMetrics.density * 8f).toInt()
        val diff = content.bottom - (detailScroll.height + detailScroll.scrollY)
        if (diff <= threshold) {
            reachedBottom = true
        }
    }

    private fun handleBackPress() {
        maybeRecordReadingProgress(calculateReadingProgress(), force = true)
        if (fromWatchLater && reachedBottom && isWatchLater) {
            val itemId = viewModel.item.value?.id ?: 0L
            if (itemId > 0L) {
                val data = Intent().putExtra(EXTRA_REMOVE_WATCH_LATER_ID, itemId)
                setResult(RESULT_OK, data)
            }
        }
        finish()
    }

    override fun onPause() {
        maybeRecordReadingProgress(calculateReadingProgress(), force = true)
        super.onPause()
    }

    private fun calculateReadingProgress(): Float {
        val child = detailScroll.getChildAt(0) ?: return 0f
        val scrollRange = (child.height - detailScroll.height).coerceAtLeast(0)
        return if (scrollRange == 0) {
            1f
        } else {
            (detailScroll.scrollY.toFloat() / scrollRange.toFloat()).coerceIn(0f, 1f)
        }
    }

    private fun maybeRecordReadingProgress(progress: Float, force: Boolean) {
        val clamped = progress.coerceIn(0f, 1f)
        val now = SystemClock.elapsedRealtime()
        if (!force && lastSavedProgress >= 0f) {
            val diff = abs(clamped - lastSavedProgress)
            if (diff < 0.02f && now - lastProgressSavedAt < 1500L) return
        }
        lastSavedProgress = clamped
        lastProgressSavedAt = now
        viewModel.updateReadingProgress(clamped)
    }

    private fun formatTitleForWidthLimits(title: String): String {
        val normalized = title.trim().replace('\n', ' ')
        if (normalized.isEmpty()) {
            return title
        }
        val firstLimitPx =
            resources.getDimensionPixelSize(R.dimen.detail_title_first_line_max_width).toFloat()
        val secondLimitPx =
            resources.getDimensionPixelSize(R.dimen.detail_title_second_line_max_width).toFloat()
        val paint = titleView.paint
        val lines = mutableListOf<String>()
        var start = 0
        var lineIndex = 0
        while (start < normalized.length) {
            val limitPx = when (lineIndex) {
                0 -> firstLimitPx
                else -> secondLimitPx
            }
            val end = breakTextIndex(normalized, start, limitPx, paint)
            if (end <= start) {
                lines.add(normalized.substring(start, start + 1))
                start += 1
            } else {
                lines.add(normalized.substring(start, end))
                start = end
            }
            lineIndex++
        }
        return lines.joinToString("\n")
    }

    private fun breakTextIndex(
        text: String,
        start: Int,
        maxWidthPx: Float,
        paint: TextPaint
    ): Int {
        if (start >= text.length || maxWidthPx <= 0f) {
            return text.length
        }
        val count = paint.breakText(text, start, text.length, true, maxWidthPx, null)
        if (count <= 0) {
            return start
        }
        var end = start + count
        while (end < text.length && text[end] == ' ') {
            end++
        }
        return end
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

    private fun updateProgressIndicator() {
        val child = detailScroll.getChildAt(0) ?: run {
            if (progressIndicatorEnabled) {
                progressRing.setProgress(0f)
            }
            return
        }
        val scrollRange = (child.height - detailScroll.height).coerceAtLeast(0)
        val uiProgress = if (scrollRange == 0) {
            0f
        } else {
            detailScroll.scrollY.toFloat() / scrollRange.toFloat()
        }
        if (progressIndicatorEnabled) {
            progressRing.setProgress(uiProgress)
        }
        val readingProgress = if (scrollRange == 0) {
            1f
        } else {
            detailScroll.scrollY.toFloat() / scrollRange.toFloat()
        }
        if (hasRestoredPosition) {
            maybeRecordReadingProgress(readingProgress, force = false)
        }
    }

    private fun prepareRestore(itemId: Long, progress: Float) {
        pendingRestoreItemId = itemId
        pendingRestoreProgress = progress.coerceIn(0f, 1f)
        hasRestoredPosition = false
    }

    private fun postRestoreAndUpdate() {
        detailScroll.post {
            val restored = restoreScrollPositionIfNeeded()
            updateProgressIndicator()
            if (restored) {
                clearRestoreLayoutListener()
            } else {
                ensureRestoreLayoutListener()
            }
        }
        ensureRestoreLayoutListener()
    }

    private fun restoreScrollPositionIfNeeded(): Boolean {
        if (hasRestoredPosition) return true
        val item = viewModel.item.value ?: return false
        if (item.id != pendingRestoreItemId) return false
        val child = detailScroll.getChildAt(0) ?: return false
        val scrollRange = (child.height - detailScroll.height).coerceAtLeast(0)
        if (scrollRange == 0 && pendingRestoreProgress > 0f) return false
        val target = (scrollRange * pendingRestoreProgress).toInt().coerceAtLeast(0)
        detailScroll.scrollTo(0, target)
        hasRestoredPosition = true
        return true
    }

    private fun ensureRestoreLayoutListener() {
        if (restoreLayoutListener != null) return
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val restored = restoreScrollPositionIfNeeded()
            if (restored) {
                updateProgressIndicator()
                clearRestoreLayoutListener()
            }
        }
        restoreLayoutListener = listener
        detailScroll.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    private fun clearRestoreLayoutListener() {
        val listener = restoreLayoutListener ?: return
        detailScroll.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        restoreLayoutListener = null
    }

    override fun onDestroy() {
        clearRestoreLayoutListener()
        super.onDestroy()
    }

    private fun applyProgressPadding(enabled: Boolean) {
        val extra = if (enabled) 16 else 0
        updateSpacerHeight(safeTopSpacer, baseSafePadding + extra)
        updateSpacerHeight(safeBottomSpacer, baseSafePadding + extra)
    }

    private fun updateSpacerHeight(view: View, height: Int) {
        val params = view.layoutParams ?: return
        if (params.height != height) {
            params.height = height
            view.layoutParams = params
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
        view.setOnClickListener { openExternalLink(resolveMediaUrl(url)) }
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

    private fun showShareQr() {
        val link = currentLink?.trim().orEmpty()
        if (link.isBlank()) {
            HeyToast.showToast(this, "暂无可分享链接", android.widget.Toast.LENGTH_SHORT)
            return
        }
        startActivity(ShareQrActivity.createIntent(this, currentTitle, link))
    }

    private fun openLinkInApp(link: String) {
        val trimmed = link.trim()
        if (trimmed.isEmpty()) return
        startActivity(WebViewActivity.createIntent(this, trimmed))
    }

    private fun openExternalLink(link: String) {
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
        const val EXTRA_FROM_WATCH_LATER = "fromWatchLater"
        const val EXTRA_REMOVE_WATCH_LATER_ID = "removeWatchLaterId"
    }
}
