package com.lightningstudio.watchrss.ui.screen.rss

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import android.graphics.Paint
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssChannel
import com.lightningstudio.watchrss.data.rss.RssItem
import com.lightningstudio.watchrss.data.rss.RssUrlResolver
import com.lightningstudio.watchrss.ui.components.PullRefreshBox
import com.lightningstudio.watchrss.ui.components.SwipeActionButton
import com.lightningstudio.watchrss.ui.components.SwipeActionRow
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun FeedScreen(
    channel: RssChannel?,
    items: List<RssItem>,
    isRefreshing: Boolean,
    hasMore: Boolean,
    openSwipeId: Long?,
    onOpenSwipe: (Long) -> Unit,
    onCloseSwipe: () -> Unit,
    draggingSwipeId: Long?,
    onDragStart: (Long) -> Unit,
    onDragEnd: () -> Unit,
    onHeaderClick: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (RssItem) -> Unit,
    onItemLongClick: (RssItem) -> Unit,
    onFavoriteClick: (RssItem) -> Unit,
    onWatchLaterClick: (RssItem) -> Unit,
    onBack: () -> Unit
) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val itemSpacing = dimensionResource(R.dimen.hey_distance_8dp)
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val maxImageWidthPx = remember(context) {
        val safePaddingPx = context.resources.getDimensionPixelSize(R.dimen.watch_safe_padding)
        (context.resources.displayMetrics.widthPixels - safePaddingPx * 2).coerceAtLeast(1)
    }
    val isAtTop by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
        }
    }

    PullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        indicatorPadding = safePadding,
        isAtTop = { isAtTop }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(safePadding),
            state = listState,
            contentPadding = PaddingValues(bottom = itemSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            item(key = "header") {
                FeedHeader(
                    title = channel?.title ?: "RSS",
                    isRefreshing = isRefreshing,
                    onClick = onHeaderClick
                )
            }
            if (items.isEmpty()) {
                item(key = "empty") {
                    FeedEmpty(
                        onBack = onBack
                    )
                }
            } else {
                items(items, key = { it.id }) { item ->
                    FeedItemEntry(
                        item = item,
                        maxImageWidthPx = maxImageWidthPx,
                        openSwipeId = openSwipeId,
                        onOpenSwipe = onOpenSwipe,
                        onCloseSwipe = onCloseSwipe,
                        draggingSwipeId = draggingSwipeId,
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        onFavoriteClick = { onFavoriteClick(item) },
                        onWatchLaterClick = { onWatchLaterClick(item) }
                    )
                }
                item(key = "actions") {
                    FeedActions(
                        canLoadMore = hasMore,
                        onLoadMore = onLoadMore
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    title: String,
    isRefreshing: Boolean,
    onClick: () -> Unit
) {
    val verticalPadding = dimensionResource(R.dimen.hey_content_horizontal_distance)
    val hintSize = textSize(R.dimen.hey_caption)
    val titleSize = textSize(R.dimen.hey_m_title)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding)
            .clickableWithoutRipple(onClick)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
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
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = titleSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = if (isRefreshing) "正在刷新中..." else "下拉刷新",
            color = colorResource(android.R.color.darker_gray),
            fontSize = hintSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FeedEmpty(
    onBack: () -> Unit
) {
    val padding = dimensionResource(R.dimen.hey_content_horizontal_distance)
    val buttonSpacing = dimensionResource(R.dimen.hey_distance_6dp)
    val hintSize = textSize(R.dimen.hey_caption)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "暂无内容",
            color = colorResource(android.R.color.darker_gray),
            fontSize = hintSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(buttonSpacing))
        FeedPillButton(text = "返回", onClick = onBack)
    }
}

@Composable
private fun FeedActions(
    canLoadMore: Boolean,
    onLoadMore: () -> Unit
) {
    val padding = dimensionResource(R.dimen.hey_distance_4dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeedPillButton(
            text = if (canLoadMore) "加载更多" else "没有更多",
            enabled = canLoadMore,
            onClick = onLoadMore
        )
    }
}

@Composable
private fun FeedPillButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val radius = dimensionResource(R.dimen.hey_button_default_radius)
    val height = dimensionResource(R.dimen.hey_button_height)
    val horizontalPadding = dimensionResource(R.dimen.hey_button_mergin_horizontal)
    val verticalPadding = dimensionResource(R.dimen.hey_button_padding_vertical)
    val textSize = textSize(R.dimen.hey_s_title)
    val background = colorResource(R.color.watch_pill_background)

    Box(
        modifier = Modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(background)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = textSize,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeedItemEntry(
    item: RssItem,
    maxImageWidthPx: Int,
    openSwipeId: Long?,
    onOpenSwipe: (Long) -> Unit,
    onCloseSwipe: () -> Unit,
    draggingSwipeId: Long?,
    onDragStart: (Long) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onWatchLaterClick: () -> Unit
) {
    val actionPadding = dimensionResource(R.dimen.hey_distance_4dp)
    val actionWidth = dimensionResource(R.dimen.watch_swipe_action_button_width)
    val fallbackActionsWidthPx = with(LocalDensity.current) {
        (actionWidth * 2 + actionPadding * 3).toPx()
    }
    val revealGapPx = with(LocalDensity.current) { (actionPadding * 2).toPx() }
    var actionsWidthPx by remember { mutableStateOf(fallbackActionsWidthPx) }
    var cardHeightPx by remember { mutableStateOf(0) }
    val fallbackCardHeight = dimensionResource(R.dimen.feed_card_image_height)
    val cardHeight = if (cardHeightPx > 0) {
        with(LocalDensity.current) { cardHeightPx.toDp() }
    } else {
        fallbackCardHeight
    }
    val pressState = rememberPressScaleState()
    val thumbUrl = remember(item.id, item.imageUrl, item.link) {
        resolveThumbUrl(item)
    }

    SwipeActionRow(
        itemId = item.id,
        openSwipeId = openSwipeId,
        onOpenSwipe = onOpenSwipe,
        onCloseSwipe = onCloseSwipe,
        draggingSwipeId = draggingSwipeId,
        onDragStart = onDragStart,
        onDragEnd = onDragEnd,
        actionsWidthPx = actionsWidthPx,
        revealGapPx = revealGapPx
    ) { offsetModifier ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(cardHeight)
                    .padding(horizontal = actionPadding)
                    .onSizeChanged { size ->
                        actionsWidthPx = size.width.toFloat()
                    },
                horizontalArrangement = Arrangement.spacedBy(actionPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SwipeActionButton(
                    text = "收藏",
                    width = actionWidth,
                    onClick = {
                        onCloseSwipe()
                        onFavoriteClick()
                    },
                    iconRes = R.drawable.ic_action_favorite
                )
                SwipeActionButton(
                    text = "稍后再看",
                    width = actionWidth,
                    onClick = {
                        onCloseSwipe()
                        onWatchLaterClick()
                    },
                    iconRes = R.drawable.ic_action_watch_later
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(offsetModifier)
                    .onSizeChanged { size ->
                        cardHeightPx = size.height
                    }
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer(
                            scaleX = pressState.scale,
                            scaleY = 1f
                        )
                        .background(Color.Black)
                )
                if (thumbUrl.isNullOrBlank()) {
                    FeedTextCard(
                        item = item,
                        pressState = pressState,
                        modifier = Modifier.graphicsLayer(
                            scaleX = pressState.scale,
                            scaleY = pressState.scale
                        ),
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    FeedImageCard(
                        item = item,
                        thumbUrl = thumbUrl,
                        maxImageWidthPx = maxImageWidthPx,
                        pressState = pressState,
                        modifier = Modifier.graphicsLayer(
                            scaleX = pressState.scale,
                            scaleY = pressState.scale
                        ),
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedTextCard(
    item: RssItem,
    pressState: PressScaleState,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val background = colorResource(R.color.watch_card_background)
    val shape = RoundedCornerShape(dimensionResource(R.dimen.hey_card_normal_bg_radius))
    val padding = dimensionResource(R.dimen.hey_content_horizontal_distance)
    val titleSize = textSize(R.dimen.feed_card_title_text_size)
    val summarySize = textSize(R.dimen.feed_card_summary_text_size)
    val summaryTop = dimensionResource(R.dimen.hey_distance_2dp)
    val unreadSize = dimensionResource(R.dimen.hey_distance_8dp)
    val unreadMargin = dimensionResource(R.dimen.hey_distance_6dp)
    val summary = remember(item.id, item.summary) {
        item.summary ?: "暂无摘要"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickableWithoutRipple(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = pressState.interactionSource
            )
            .padding(padding)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = titleSize,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = summary,
                color = Color(0xB3FFFFFF),
                fontSize = summarySize,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = summaryTop)
            )
        }
        if (!item.isRead) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = unreadMargin, end = unreadMargin)
                    .size(unreadSize)
                    .clip(CircleShape)
                    .background(Color(0xFFFF6026))
            )
        }
    }
}

@Composable
private fun FeedImageCard(
    item: RssItem,
    thumbUrl: String,
    maxImageWidthPx: Int,
    pressState: PressScaleState,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val background = colorResource(R.color.watch_card_background)
    val shape = RoundedCornerShape(dimensionResource(R.dimen.hey_card_normal_bg_radius))
    val imageHeight = dimensionResource(R.dimen.feed_card_image_height)
    val padding = dimensionResource(R.dimen.hey_distance_8dp)
    val titleSize = textSize(R.dimen.feed_card_title_text_size)
    val summarySize = textSize(R.dimen.feed_card_summary_text_size)
    val summaryTop = dimensionResource(R.dimen.hey_distance_2dp)
    val unreadSize = dimensionResource(R.dimen.hey_distance_8dp)
    val unreadMargin = dimensionResource(R.dimen.hey_distance_6dp)
    val summary = remember(item.id, item.summary) {
        item.summary ?: "暂无摘要"
    }
    val overlay = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xB0000000))
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickableWithoutRipple(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = pressState.interactionSource
            )
    ) {
        RssThumbnail(
            url = thumbUrl,
            maxWidthPx = maxImageWidthPx,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .background(overlay)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(padding)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = titleSize,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = summary,
                color = Color(0xCCFFFFFF),
                fontSize = summarySize,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = summaryTop)
            )
        }
        if (!item.isRead) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = unreadMargin, end = unreadMargin)
                    .size(unreadSize)
                    .clip(CircleShape)
                    .background(Color(0xFFFF6026))
            )
        }
    }
}

@Composable
private fun RssThumbnail(
    url: String,
    maxWidthPx: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, url, maxWidthPx) {
        value = RssImageLoader.loadBitmap(context, url, maxWidthPx)
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "缩略图",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1C1C1C))
        )
    }
}

@Stable
private data class PressScaleState(
    val scale: Float,
    val interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource
)

@Composable
private fun rememberPressScaleState(): PressScaleState {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = if (pressed) 240 else 360),
        label = "pressScale"
    )
    return PressScaleState(scale, interactionSource)
}

@Composable
private fun textSize(id: Int): TextUnit {
    val density = LocalDensity.current
    return with(density) { dimensionResource(id).toSp() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.clickableWithoutRipple(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource =
        androidx.compose.foundation.interaction.MutableInteractionSource()
): Modifier {
    return if (onLongClick != null) {
        combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    }
}

private fun resolveThumbUrl(item: RssItem): String? {
    val candidate = item.imageUrl?.takeIf { it.isNotBlank() }
    return RssUrlResolver.resolveMediaUrl(candidate, item.link)
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
    val firstLimit = min(firstLimitPx, availableWidthPx)
    val secondLimit = min(secondLimitPx, availableWidthPx)
    val lines = mutableListOf<String>()
    var start = 0
    var lineIndex = 0
    while (start < normalized.length) {
        val limit = if (lineIndex == 0) firstLimit else secondLimit
        val end = breakTextIndex(normalized, start, limit, paint)
        if (end <= start) {
            lines.add(normalized.substring(start, start + 1))
            start += 1
        } else {
            lines.add(normalized.substring(start, end))
            start = end
        }
        lineIndex++
    }
    balanceSingleCharLines(lines, paint, firstLimit, secondLimit)
    return lines.joinToString("\n")
}

private fun breakTextIndex(text: String, start: Int, widthPx: Float, paint: TextPaint): Int {
    var low = start
    var high = text.length
    while (low < high) {
        val mid = (low + high + 1) / 2
        val current = text.substring(start, mid)
        if (paint.measureText(current) <= widthPx) {
            low = mid
        } else {
            high = mid - 1
        }
    }
    return low
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
