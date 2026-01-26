package com.lightningstudio.watchrss.ui.screen.rss

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.text.TextPaint
import android.util.TypedValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.ImagePreviewActivity
import com.lightningstudio.watchrss.ShareQrActivity
import com.lightningstudio.watchrss.RssPlayerActivity
import com.lightningstudio.watchrss.WebViewActivity
import com.lightningstudio.watchrss.data.rss.OfflineMedia
import com.lightningstudio.watchrss.data.rss.RssItem
import com.lightningstudio.watchrss.data.settings.DEFAULT_READING_FONT_SIZE_SP
import com.lightningstudio.watchrss.ui.util.ContentBlock
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import com.lightningstudio.watchrss.ui.util.TextStyle as ContentTextStyle
import com.lightningstudio.watchrss.ui.viewmodel.DetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: (Long, Boolean, Boolean) -> Unit
) {
    val item by viewModel.item.collectAsState()
    val savedState by viewModel.savedState.collectAsState()
    val offlineMedia by viewModel.offlineMedia.collectAsState()
    val contentBlocks by viewModel.contentBlocks.collectAsState()
    val readingThemeDark by viewModel.readingThemeDark.collectAsState()
    val readingFontSizeSp by viewModel.readingFontSizeSp.collectAsState()
    val progressIndicatorEnabled by viewModel.detailProgressIndicatorEnabled.collectAsState(initial = true)
    val shareUseSystem by viewModel.shareUseSystem.collectAsState(initial = true)

    val hasOfflineFailures = remember(offlineMedia) { offlineMedia.any { it.localPath == null } }
    val offlineMap = remember(offlineMedia) { offlineMedia.associateBy { it.originUrl } }

    DetailContent(
        item = item,
        contentBlocks = contentBlocks,
        offlineMedia = offlineMap,
        hasOfflineFailures = hasOfflineFailures,
        isFavorite = savedState.isFavorite,
        isWatchLater = savedState.isWatchLater,
        readingThemeDark = readingThemeDark,
        readingFontSizeSp = readingFontSizeSp,
        progressIndicatorEnabled = progressIndicatorEnabled,
        shareUseSystem = shareUseSystem,
        onToggleFavorite = viewModel::toggleFavorite,
        onRetryOfflineMedia = viewModel::retryOfflineMedia,
        onSaveReadingProgress = viewModel::updateReadingProgress,
        onBack = onBack
    )
}

@Composable
internal fun DetailContent(
    item: RssItem?,
    contentBlocks: List<ContentBlock>,
    offlineMedia: Map<String, OfflineMedia>,
    hasOfflineFailures: Boolean,
    isFavorite: Boolean,
    isWatchLater: Boolean,
    readingThemeDark: Boolean,
    readingFontSizeSp: Int,
    progressIndicatorEnabled: Boolean,
    shareUseSystem: Boolean,
    onToggleFavorite: () -> Unit,
    onRetryOfflineMedia: () -> Unit,
    onSaveReadingProgress: (Float) -> Unit,
    onBack: (Long, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val pagePadding = dimensionResource(R.dimen.detail_page_horizontal_padding)
    val blockSpacing = dimensionResource(R.dimen.detail_block_spacing)
    val titlePadding = dimensionResource(R.dimen.detail_title_safe_padding)
    val actionSpacing = dimensionResource(R.dimen.hey_content_horizontal_distance)
    val iconSize = dimensionResource(R.dimen.hey_listitem_lefticon_height_width)
    val iconPadding = dimensionResource(R.dimen.hey_distance_6dp)
    val extraSafePadding = if (progressIndicatorEnabled) {
        with(density) { 16f.toDp() }
    } else {
        0.dp
    }

    val backgroundColor = if (readingThemeDark) Color.Black else Color.White
    val textColor = if (readingThemeDark) Color.White else Color(0xFF111111)
    val activeColor = colorResource(R.color.oppo_orange)
    val normalIconColor = textColor

    val maxImageWidthPx = remember(context) {
        val safePaddingPx = context.resources.getDimensionPixelSize(R.dimen.watch_safe_padding)
        (context.resources.displayMetrics.widthPixels - safePaddingPx * 2).coerceAtLeast(1)
    }

    var pendingRestoreProgress by remember { mutableStateOf<Float?>(null) }
    var hasRestoredPosition by remember { mutableStateOf(false) }
    var lastItemId by remember { mutableStateOf<Long?>(null) }
    var reachedBottom by remember { mutableStateOf(false) }
    var ringProgress by remember { mutableStateOf(0f) }
    var lastSavedProgress by remember { mutableStateOf(-1f) }
    var lastProgressSavedAt by remember { mutableStateOf(0L) }

    val onSaveReadingProgressState = rememberUpdatedState(onSaveReadingProgress)
    val onBackState = rememberUpdatedState(onBack)
    val isWatchLaterState = rememberUpdatedState(isWatchLater)
    val hasRestoredPositionState = rememberUpdatedState(hasRestoredPosition)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(item?.id) {
        val itemId = item?.id ?: return@LaunchedEffect
        if (itemId != lastItemId) {
            lastItemId = itemId
            pendingRestoreProgress = item?.readingProgress
            hasRestoredPosition = false
            lastSavedProgress = -1f
            lastProgressSavedAt = 0L
            reachedBottom = false
        }
    }

    LaunchedEffect(readingFontSizeSp, readingThemeDark, offlineMedia) {
        if (item == null || !hasRestoredPosition) return@LaunchedEffect
        pendingRestoreProgress = calculateReadingProgress(listState)
        hasRestoredPosition = false
    }

    LaunchedEffect(pendingRestoreProgress, listState.layoutInfo.totalItemsCount) {
        val progress = pendingRestoreProgress ?: return@LaunchedEffect
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems == 0) {
            if (progress <= 0f) {
                pendingRestoreProgress = null
                hasRestoredPosition = true
            }
            return@LaunchedEffect
        }
        val target = ((totalItems - 1) * progress)
            .roundToInt()
            .coerceIn(0, totalItems - 1)
        listState.scrollToItem(target)
        pendingRestoreProgress = null
        hasRestoredPosition = true
    }

    LaunchedEffect(listState, density) {
        val thresholdPx = with(density) { 8.dp.toPx() }
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.layoutInfo.totalItemsCount
            )
        }.collectLatest {
            val readingProgress = calculateReadingProgress(listState)
            ringProgress = readingProgress
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
            reachedBottom = if (lastVisible == null) {
                false
            } else {
                val bottom = lastVisible.offset + lastVisible.size
                lastVisible.index >= layoutInfo.totalItemsCount - 1 &&
                    bottom >= layoutInfo.viewportEndOffset - thresholdPx
            }
            if (hasRestoredPositionState.value) {
                maybeSaveReadingProgress(
                    readingProgress = readingProgress,
                    force = false,
                    lastSavedProgress = { lastSavedProgress },
                    lastProgressSavedAt = { lastProgressSavedAt },
                    updateLastSavedProgress = { lastSavedProgress = it },
                    updateLastProgressSavedAt = { lastProgressSavedAt = it },
                    onSave = onSaveReadingProgressState.value
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                maybeSaveReadingProgress(
                    readingProgress = calculateReadingProgress(listState),
                    force = true,
                    lastSavedProgress = { lastSavedProgress },
                    lastProgressSavedAt = { lastProgressSavedAt },
                    updateLastSavedProgress = { lastSavedProgress = it },
                    updateLastProgressSavedAt = { lastProgressSavedAt = it },
                    onSave = onSaveReadingProgressState.value
                )
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        val progress = calculateReadingProgress(listState)
        maybeSaveReadingProgress(
            readingProgress = progress,
            force = true,
            lastSavedProgress = { lastSavedProgress },
            lastProgressSavedAt = { lastProgressSavedAt },
            updateLastSavedProgress = { lastSavedProgress = it },
            updateLastProgressSavedAt = { lastProgressSavedAt = it },
            onSave = onSaveReadingProgressState.value
        )
        onBackState.value(item?.id ?: 0L, reachedBottom, isWatchLaterState.value)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val bodyFontSize = adjustedTextSizeSp(
            context = context,
            density = density,
            baseDimenRes = R.dimen.detail_body_text_size,
            currentFontSizeSp = readingFontSizeSp
        )
        val link = item?.link?.trim().orEmpty()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = pagePadding)
        ) {
            item(key = "topSpacer") {
                Spacer(modifier = Modifier.height(safePadding + extraSafePadding))
            }
            item(key = "titleGap") {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.hey_distance_4dp)))
            }
            item(key = "title") {
                DetailTitle(
                    title = item?.title ?: "加载中...",
                    titlePadding = titlePadding,
                    textColor = textColor
                )
            }
            if (link.isNotEmpty()) {
                item(key = "linkAction") {
                    Spacer(modifier = Modifier.height(blockSpacing))
                    DetailActionButton(
                        text = "打开原文",
                        fontSize = bodyFontSize,
                        onClick = { openLinkInApp(context, link) }
                    )
                }
            }
            if (hasOfflineFailures) {
                item(key = "offlineAction") {
                    Spacer(modifier = Modifier.height(blockSpacing))
                    DetailActionButton(
                        text = "离线媒体下载失败，点此重试",
                        fontSize = bodyFontSize,
                        onClick = onRetryOfflineMedia
                    )
                }
            }
            item(key = "contentGap") {
                Spacer(modifier = Modifier.height(blockSpacing))
            }
            if (item == null) {
                item(key = "loading") {
                    // Keep layout consistent while content loads.
                }
            } else if (contentBlocks.isEmpty()) {
                item(key = "emptyContent") {
                    DetailTextBlock(
                        text = "暂无正文",
                        style = ContentTextStyle.BODY,
                        textColor = textColor,
                        fontSizeSp = bodyFontSize,
                        topPadding = 0.dp
                    )
                }
            } else {
                itemsIndexed(
                    items = contentBlocks,
                    key = { index, block -> "${block.hashCode()}_$index" }
                ) { index, block ->
                    val topPadding = if (index == 0) 0.dp else blockSpacing
                    when (block) {
                        is ContentBlock.Text -> {
                            DetailTextBlock(
                                text = block.text,
                                style = block.style,
                                textColor = textColor,
                                fontSizeSp = adjustedTextSizeSp(
                                    context = context,
                                    density = density,
                                    baseDimenRes = when (block.style) {
                                        ContentTextStyle.TITLE -> R.dimen.detail_title_text_size
                                        ContentTextStyle.SUBTITLE -> R.dimen.detail_subtitle_text_size
                                        ContentTextStyle.QUOTE -> R.dimen.detail_body_text_size
                                        ContentTextStyle.CODE -> R.dimen.detail_body_text_size
                                        ContentTextStyle.BODY -> R.dimen.detail_body_text_size
                                    },
                                    currentFontSizeSp = readingFontSizeSp
                                ),
                                topPadding = topPadding
                            )
                        }
                        is ContentBlock.Image -> {
                            val resolvedUrl = resolveMediaUrl(block.url, offlineMedia)
                            DetailImageBlock(
                                url = resolvedUrl,
                                alt = block.alt,
                                maxWidthPx = maxImageWidthPx,
                                topPadding = topPadding,
                                onClick = { openImagePreview(context, resolvedUrl, block.alt) }
                            )
                        }
                        is ContentBlock.Video -> {
                            val resolvedUrl = resolveMediaUrl(block.url, offlineMedia)
                            DetailVideoBlock(
                                poster = block.poster?.let { resolveMediaUrl(it, offlineMedia) },
                                videoUrl = resolvedUrl,
                                maxWidthPx = maxImageWidthPx,
                                topPadding = topPadding,
                                onClick = { openRssVideo(context, resolvedUrl, block.url) }
                            )
                        }
                    }
                }
            }
            item(key = "actionSpacing") {
                Spacer(modifier = Modifier.height(actionSpacing))
            }
            item(key = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIconButton(
                        iconRes = R.drawable.ic_action_favorite,
                        contentDescription = "收藏",
                        tint = if (isFavorite) activeColor else normalIconColor,
                        size = iconSize,
                        padding = iconPadding,
                        onClick = onToggleFavorite
                    )
                    Spacer(modifier = Modifier.width(actionSpacing))
                    CircleIconButton(
                        iconRes = R.drawable.ic_action_share,
                        contentDescription = "分享",
                        tint = normalIconColor,
                        size = iconSize,
                        padding = iconPadding,
                        onClick = {
                            val title = item?.title.orEmpty()
                            val shareLink = item?.link?.trim().orEmpty().ifBlank { null }
                            if (shareUseSystem) {
                                shareCurrent(context, title, shareLink)
                            } else {
                                showShareQr(context, title, shareLink)
                            }
                        }
                    )
                }
            }
            item(key = "bottomSpacer") {
                Spacer(modifier = Modifier.height(safePadding + extraSafePadding))
            }
        }

        if (progressIndicatorEnabled) {
            ProgressRing(progress = ringProgress)
        }
    }
}

@Composable
private fun DetailTitle(
    title: String,
    titlePadding: Dp,
    textColor: Color
) {
    val hintSize = textSize(R.dimen.hey_m_title)
    val context = LocalContext.current
    val density = LocalDensity.current
    val titleSizePx = with(density) { dimensionResource(R.dimen.hey_m_title).toPx() }
    val firstLimitPx = with(density) {
        dimensionResource(R.dimen.detail_title_first_line_max_width).toPx()
    }
    val secondLimitPx = with(density) {
        dimensionResource(R.dimen.detail_title_second_line_max_width).toPx()
    }
    val typeface = remember(context) { ResourcesCompat.getFont(context, R.font.oppo_sans) }
    val paint = remember(typeface, titleSizePx) {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = titleSizePx
            this.typeface = typeface
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = titlePadding)
    ) {
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val formattedTitle = remember(title, availableWidthPx, titleSizePx, typeface) {
            formatTitleForWidthLimits(
                title = title,
                paint = paint,
                availableWidthPx = availableWidthPx,
                firstLimitPx = firstLimitPx,
                secondLimitPx = secondLimitPx
            )
        }
        Text(
            text = formattedTitle,
            color = textColor,
            fontSize = hintSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailActionButton(
    text: String,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.hey_button_default_radius))
    val padding = PaddingValues(
        horizontal = dimensionResource(R.dimen.hey_content_horizontal_distance),
        vertical = dimensionResource(R.dimen.hey_distance_6dp)
    )
    Box(
        modifier = Modifier
            .clip(shape)
            .background(colorResource(R.color.watch_card_background))
            .clickableWithoutRipple(onClick)
            .padding(padding)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = fontSize
        )
    }
}

@Composable
private fun DetailTextBlock(
    text: String,
    style: ContentTextStyle,
    textColor: Color,
    fontSizeSp: TextUnit,
    topPadding: Dp
) {
    val lineHeight = fontSizeSp * 1.2f
    val fontFamily = if (style == ContentTextStyle.CODE) {
        FontFamily.Monospace
    } else {
        null
    }
    val color = if (style == ContentTextStyle.QUOTE) {
        textColor.copy(alpha = 0.8f)
    } else {
        textColor
    }
    Text(
        text = text,
        color = color,
        fontSize = fontSizeSp,
        lineHeight = lineHeight,
        fontFamily = fontFamily,
        style = TextStyle(textAlign = TextAlign.Start),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
    )
}

@Composable
private fun DetailImageBlock(
    url: String,
    alt: String?,
    maxWidthPx: Int,
    topPadding: Dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, url, maxWidthPx) {
        value = RssImageLoader.loadBitmap(context, url, maxWidthPx)
    }
    val safeBitmap = bitmap
    val ratio = safeBitmap?.let { it.width.toFloat() / it.height.toFloat() }?.takeIf { it > 0f }
    if (safeBitmap != null && ratio != null) {
        Image(
            bitmap = safeBitmap.asImageBitmap(),
            contentDescription = alt,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .aspectRatio(ratio)
                .clickableWithoutRipple(onClick)
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .height(dimensionResource(R.dimen.hey_card_large_height))
                .background(colorResource(R.color.watch_card_background))
        )
    }
}

@Composable
private fun DetailVideoBlock(
    poster: String?,
    videoUrl: String,
    maxWidthPx: Int,
    topPadding: Dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, poster, videoUrl, maxWidthPx) {
        value = if (!poster.isNullOrBlank()) {
            RssImageLoader.loadBitmap(context, poster, maxWidthPx)
        } else {
            loadVideoFrame(context, videoUrl, maxWidthPx)
        }
    }
    val shape = RoundedCornerShape(dimensionResource(R.dimen.hey_card_normal_bg_radius))
    val padding = dimensionResource(R.dimen.hey_distance_6dp)
    val coverHeight = dimensionResource(R.dimen.hey_card_large_height)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .clip(shape)
            .background(colorResource(R.color.watch_card_background))
            .clickableWithoutRipple(onClick)
            .padding(padding)
    ) {
        val safeCover = coverBitmap
        if (safeCover != null) {
            Image(
                bitmap = safeCover.asImageBitmap(),
                contentDescription = "视频封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverHeight)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverHeight)
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_play_circle),
            contentDescription = "播放",
            modifier = Modifier
                .align(Alignment.Center)
                .size(dimensionResource(R.dimen.hey_listitem_widget_size))
        )
    }
}

private suspend fun loadVideoFrame(
    context: Context,
    url: String,
    maxWidthPx: Int
): Bitmap? {
    if (url.isBlank()) return null
    return withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            when {
                url.startsWith("file://") -> retriever.setDataSource(url.removePrefix("file://"))
                url.startsWith("/") -> retriever.setDataSource(url)
                url.startsWith("content://") -> retriever.setDataSource(context, Uri.parse(url))
                else -> retriever.setDataSource(url, emptyMap())
            }
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return@withContext null
            if (maxWidthPx > 0 && frame.width > maxWidthPx) {
                val height = (frame.height * (maxWidthPx.toFloat() / frame.width)).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(frame, maxWidthPx, height, true)
            } else {
                frame
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}

@Composable
private fun CircleIconButton(
    iconRes: Int,
    contentDescription: String,
    tint: Color,
    size: Dp,
    padding: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF303030))
            .clickableWithoutRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint)
        )
    }
}

@Composable
private fun ProgressRing(progress: Float) {
    val strokeWidthPx = 12f
    val baseColor = Color(0xFF202124)
    val progressColor = Color(0xFF476CFF)

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val minSize = min(size.width, size.height)
        if (minSize <= 0f) return@Canvas
        val radius = minSize / 2f - strokeWidthPx / 2f
        val center = Offset(x = size.width / 2f, y = size.height / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        drawCircle(
            color = baseColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidthPx)
        )
        if (progress <= 0f) return@Canvas
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = progress.coerceIn(0f, 1f) * 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun textSize(id: Int): TextUnit {
    val density = LocalDensity.current
    return with(density) { dimensionResource(id).toSp() }
}

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

private fun adjustedTextSizeSp(
    context: Context,
    density: androidx.compose.ui.unit.Density,
    baseDimenRes: Int,
    currentFontSizeSp: Int
): TextUnit {
    val basePx = context.resources.getDimension(baseDimenRes)
    val deltaSp = (currentFontSizeSp - DEFAULT_READING_FONT_SIZE_SP).toFloat()
    val deltaPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        deltaSp,
        context.resources.displayMetrics
    )
    val sizePx = (basePx + deltaPx).coerceAtLeast(10f)
    return with(density) { sizePx.toSp() }
}

private fun resolveMediaUrl(url: String, offlineMedia: Map<String, OfflineMedia>): String {
    val local = offlineMedia[url]?.localPath
    return if (!local.isNullOrBlank()) local else url
}

private fun calculateReadingProgress(listState: androidx.compose.foundation.lazy.LazyListState): Float {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    if (totalItems == 0) return 1f
    val firstIndex = listState.firstVisibleItemIndex
    val firstOffset = listState.firstVisibleItemScrollOffset
    val firstSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
    val offsetProgress = if (firstSize > 0) firstOffset.toFloat() / firstSize.toFloat() else 0f
    val rawProgress = (firstIndex + offsetProgress) / totalItems.toFloat()
    return rawProgress.coerceIn(0f, 1f)
}

private fun maybeSaveReadingProgress(
    readingProgress: Float,
    force: Boolean,
    lastSavedProgress: () -> Float,
    lastProgressSavedAt: () -> Long,
    updateLastSavedProgress: (Float) -> Unit,
    updateLastProgressSavedAt: (Long) -> Unit,
    onSave: (Float) -> Unit
) {
    val clamped = readingProgress.coerceIn(0f, 1f)
    val now = SystemClock.elapsedRealtime()
    if (!force && lastSavedProgress() >= 0f) {
        val diff = abs(clamped - lastSavedProgress())
        if (diff < 0.02f && now - lastProgressSavedAt() < 1500L) return
    }
    updateLastSavedProgress(clamped)
    updateLastProgressSavedAt(now)
    onSave(clamped)
}

private fun openLinkInApp(context: Context, link: String) {
    val trimmed = link.trim()
    if (trimmed.isEmpty()) return
    context.startActivity(WebViewActivity.createIntent(context, trimmed))
}

private fun shareCurrent(context: Context, title: String, link: String?) {
    if (title.isBlank()) return
    val text = if (!link.isNullOrBlank()) {
        "$title\n$link"
    } else {
        title
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}

private fun showShareQr(context: Context, title: String, link: String?) {
    val trimmed = link?.trim().orEmpty()
    if (trimmed.isEmpty()) {
        HeyToast.showToast(context, "暂无可分享链接", android.widget.Toast.LENGTH_SHORT)
        return
    }
    context.startActivity(ShareQrActivity.createIntent(context, title, trimmed))
}

private fun openExternalLink(context: Context, link: String) {
    val trimmed = link.trim()
    if (trimmed.isEmpty()) return
    val uri = if (trimmed.startsWith("/")) {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(trimmed))
    } else {
        Uri.parse(trimmed)
    }
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(intent)
}

private fun openImagePreview(context: Context, url: String, alt: String?) {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return
    context.startActivity(ImagePreviewActivity.createIntent(context, trimmed, alt))
}

private fun openRssVideo(context: Context, playUrl: String, webUrl: String?) {
    val trimmed = playUrl.trim()
    if (trimmed.isEmpty()) return
    context.startActivity(RssPlayerActivity.createIntent(context, trimmed, webUrl))
}

private fun formatTitleForWidthLimits(
    title: String,
    paint: TextPaint,
    availableWidthPx: Float,
    firstLimitPx: Float,
    secondLimitPx: Float
): String {
    val normalized = title.trim().replace('\n', ' ')
    if (normalized.isEmpty()) {
        return title
    }
    val cappedFirst = min(firstLimitPx, availableWidthPx)
    val cappedSecond = min(secondLimitPx, availableWidthPx)
    val lines = mutableListOf<String>()
    var start = 0
    var lineIndex = 0
    while (start < normalized.length) {
        val limitPx = if (lineIndex == 0) cappedFirst else cappedSecond
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
    balanceSingleCharLines(lines, paint, cappedFirst, cappedSecond)
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

private fun balanceSingleCharLines(
    lines: MutableList<String>,
    paint: TextPaint,
    firstLimitPx: Float,
    otherLimitPx: Float
) {
    var index = 1
    while (index < lines.size) {
        val current = lines[index]
        if (current.length == 1) {
            val prevIndex = index - 1
            val prev = lines[prevIndex]
            val prevLimit = if (prevIndex == 0) firstLimitPx else otherLimitPx
            val mergedPrev = prev + current
            if (paint.measureText(mergedPrev) <= prevLimit) {
                lines[prevIndex] = mergedPrev
                lines.removeAt(index)
                continue
            }
            if (prev.length > 1) {
                val shiftedPrev = prev.dropLast(1)
                val shiftedCurrent = prev.takeLast(1) + current
                val currentLimit = if (index == 0) firstLimitPx else otherLimitPx
                if (paint.measureText(shiftedCurrent) <= currentLimit) {
                    lines[prevIndex] = shiftedPrev
                    lines[index] = shiftedCurrent
                    if (prevIndex > 0) {
                        index--
                        continue
                    }
                }
            }
            if (index + 1 < lines.size) {
                val next = lines[index + 1]
                if (next.isNotEmpty()) {
                    val mergedCurrent = current + next.first()
                    val currentLimit = if (index == 0) firstLimitPx else otherLimitPx
                    if (paint.measureText(mergedCurrent) <= currentLimit) {
                        lines[index] = mergedCurrent
                        val remaining = next.substring(1)
                        if (remaining.isEmpty()) {
                            lines.removeAt(index + 1)
                            continue
                        } else {
                            lines[index + 1] = remaining
                        }
                    }
                }
            }
        }
        index++
    }
}
