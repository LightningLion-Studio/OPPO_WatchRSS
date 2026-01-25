package com.lightningstudio.watchrss.ui.screen.rss

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import android.graphics.Paint
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssChannel
import com.lightningstudio.watchrss.data.rss.RssItem
import com.lightningstudio.watchrss.data.rss.RssMediaExtractor
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import com.lightningstudio.watchrss.ui.util.formatRssSummary
import kotlinx.coroutines.launch
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
    val entryList = buildFeedEntries(channel, items, isRefreshing, hasMore)
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val itemSpacing = dimensionResource(R.dimen.hey_distance_8dp)
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(safePadding)
                .feedPullToRefresh(
                    listState = listState,
                    enabled = !isRefreshing,
                    onRefresh = onRefresh
                ),
            state = listState,
            contentPadding = PaddingValues(bottom = itemSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            items(entryList, key = { it.key }) { entry ->
                when (entry) {
                    is FeedEntry.Header -> {
                        FeedHeader(
                            title = entry.title,
                            isRefreshing = entry.isRefreshing,
                            onClick = onHeaderClick
                        )
                    }
                    FeedEntry.Empty -> {
                        FeedEmpty(
                            onRefresh = onRefresh,
                            onBack = onBack
                        )
                    }
                    is FeedEntry.Actions -> {
                        FeedActions(
                            canLoadMore = entry.canLoadMore,
                            isRefreshing = entry.isRefreshing,
                            onRefresh = onRefresh,
                            onLoadMore = onLoadMore
                        )
                    }
                    is FeedEntry.Item -> {
                        FeedItemEntry(
                            item = entry.item,
                            openSwipeId = openSwipeId,
                            onOpenSwipe = onOpenSwipe,
                            onCloseSwipe = onCloseSwipe,
                            draggingSwipeId = draggingSwipeId,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onClick = { onItemClick(entry.item) },
                            onLongClick = { onItemLongClick(entry.item) },
                            onFavoriteClick = { onFavoriteClick(entry.item) },
                            onWatchLaterClick = { onWatchLaterClick(entry.item) }
                        )
                    }
                }
            }
        }
    }
}

private sealed class FeedEntry(val key: String) {
    data class Header(val title: String, val isRefreshing: Boolean) : FeedEntry("header")
    data class Item(val item: RssItem) : FeedEntry("item_${item.id}")
    data object Empty : FeedEntry("empty")
    data class Actions(val canLoadMore: Boolean, val isRefreshing: Boolean) : FeedEntry("actions")
}

private fun buildFeedEntries(
    channel: RssChannel?,
    items: List<RssItem>,
    isRefreshing: Boolean,
    hasMore: Boolean
): List<FeedEntry> {
    val entries = mutableListOf<FeedEntry>()
    entries.add(FeedEntry.Header(channel?.title ?: "RSS", isRefreshing))
    if (items.isEmpty()) {
        entries.add(FeedEntry.Empty)
    } else {
        entries.addAll(items.map { FeedEntry.Item(it) })
        entries.add(FeedEntry.Actions(hasMore, isRefreshing))
    }
    return entries
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
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val padding = dimensionResource(R.dimen.hey_content_horizontal_distance)
    val buttonSpacingLarge = dimensionResource(R.dimen.hey_distance_6dp)
    val buttonSpacingSmall = dimensionResource(R.dimen.hey_distance_4dp)
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
        Spacer(modifier = Modifier.height(buttonSpacingLarge))
        FeedPillButton(text = "刷新", onClick = onRefresh)
        Spacer(modifier = Modifier.height(buttonSpacingSmall))
        FeedPillButton(text = "返回", onClick = onBack)
    }
}

@Composable
private fun FeedActions(
    canLoadMore: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit
) {
    val padding = dimensionResource(R.dimen.hey_distance_4dp)
    val spacing = dimensionResource(R.dimen.hey_distance_6dp)

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
        Spacer(modifier = Modifier.width(spacing))
        FeedPillButton(
            text = if (isRefreshing) "刷新中" else "刷新",
            enabled = !isRefreshing,
            onClick = onRefresh
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
    val pressState = rememberPressScaleState()
    val thumbUrl = remember(item.id, item.imageUrl, item.description, item.link) {
        resolveThumbUrl(item)
    }

    FeedSwipeRow(
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
                .height(IntrinsicSize.Min)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(horizontal = actionPadding)
                    .onSizeChanged { size ->
                        actionsWidthPx = size.width.toFloat()
                    },
                horizontalArrangement = Arrangement.spacedBy(actionPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedSwipeActionButton(
                    text = "收藏",
                    width = actionWidth,
                    onClick = {
                        onCloseSwipe()
                        onFavoriteClick()
                    }
                )
                FeedSwipeActionButton(
                    text = "稍后再看",
                    width = actionWidth,
                    onClick = {
                        onCloseSwipe()
                        onWatchLaterClick()
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(offsetModifier)
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
private fun FeedSwipeActionButton(
    text: String,
    width: Dp,
    onClick: () -> Unit
) {
    val radius = dimensionResource(R.dimen.hey_card_normal_bg_radius)
    val textSize = textSize(R.dimen.feed_card_action_text_size)
    val textPadding = dimensionResource(R.dimen.hey_distance_8dp)

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFF2A2A2A))
            .clickableWithoutRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = textSize,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = textPadding)
        )
    }
}

@Composable
private fun FeedSwipeRow(
    itemId: Long,
    openSwipeId: Long?,
    onOpenSwipe: (Long) -> Unit,
    onCloseSwipe: () -> Unit,
    draggingSwipeId: Long?,
    onDragStart: (Long) -> Unit,
    onDragEnd: () -> Unit,
    actionsWidthPx: Float,
    revealGapPx: Float,
    content: @Composable (Modifier) -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val revealWidth = (actionsWidthPx + revealGapPx).coerceAtLeast(0f)
    val dragThreshold = revealWidth * 0.5f
    val openSwipeIdState = rememberUpdatedState(openSwipeId)

    LaunchedEffect(openSwipeId, actionsWidthPx, revealGapPx, draggingSwipeId) {
        if (draggingSwipeId != itemId && openSwipeId != itemId && offsetX.value != 0f) {
            offsetX.animateTo(0f, animationSpec = tween(durationMillis = 180))
        }
        if (revealWidth > 0f && offsetX.value < -revealWidth) {
            offsetX.snapTo(-revealWidth)
        }
    }

    val dragModifier = Modifier.pointerInput(itemId, actionsWidthPx, revealGapPx) {
        if (revealWidth <= 0f) return@pointerInput
        detectHorizontalDragGestures(
            onDragStart = {
                onDragStart(itemId)
                if (openSwipeIdState.value != null && openSwipeIdState.value != itemId) {
                    onCloseSwipe()
                }
            },
            onDragEnd = {
                val shouldOpen = offsetX.value <= -dragThreshold
                val target = if (shouldOpen) -revealWidth else 0f
                scope.launch {
                    offsetX.animateTo(target, animationSpec = tween(durationMillis = 180))
                }
                if (shouldOpen) {
                    onOpenSwipe(itemId)
                } else if (openSwipeIdState.value == itemId) {
                    onCloseSwipe()
                }
                onDragEnd()
            },
            onDragCancel = {
                scope.launch {
                    offsetX.animateTo(0f, animationSpec = tween(durationMillis = 180))
                }
                if (openSwipeIdState.value == itemId) {
                    onCloseSwipe()
                }
                onDragEnd()
            }
        ) { change, dragAmount ->
            change.consume()
            val newOffset = (offsetX.value + dragAmount).coerceIn(-revealWidth, 0f)
            scope.launch {
                offsetX.snapTo(newOffset)
            }
        }
    }

    val offsetModifier = Modifier
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .then(dragModifier)

    content(offsetModifier)
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
    val summary = remember(item.id, item.description) {
        formatRssSummary(item.description) ?: "暂无摘要"
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
    val summary = remember(item.id, item.description) {
        formatRssSummary(item.description) ?: "暂无摘要"
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val maxWidthPx = remember(context) { context.resources.displayMetrics.widthPixels }
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

@Composable
private fun Modifier.feedPullToRefresh(
    listState: LazyListState,
    enabled: Boolean,
    onRefresh: () -> Unit
): Modifier {
    if (!enabled) return this
    val thresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val enabledState = rememberUpdatedState(enabled)
    val refreshState = rememberUpdatedState(onRefresh)
    var pullDistance by remember { mutableStateOf(0f) }
    val connection = remember(listState, thresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabledState.value) return Offset.Zero
                val isAtTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (available.y > 0 && isAtTop) {
                    pullDistance += available.y
                    if (pullDistance >= thresholdPx) {
                        pullDistance = 0f
                        refreshState.value()
                    }
                } else if (available.y < 0) {
                    pullDistance = 0f
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < 0) {
                    pullDistance = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pullDistance = 0f
                return Velocity.Zero
            }
        }
    }

    return this.nestedScroll(connection)
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
        ?: firstImageFromHtml(item.description)
    return normalizeMediaUrl(candidate, item.link)
}

private fun firstImageFromHtml(html: String?): String? {
    if (html.isNullOrBlank()) return null
    val image = RssMediaExtractor.extractFromHtml(html)
        .firstOrNull { it.type == com.lightningstudio.watchrss.data.rss.OfflineMediaType.IMAGE }
    return image?.url
}

private fun normalizeMediaUrl(raw: String?, baseLink: String?): String? {
    val trimmed = raw?.trim()?.ifEmpty { return null } ?: return null
    if (trimmed.startsWith("data:", ignoreCase = true)) return null
    val sanitized = trimmed.replace(" ", "%20")
    if (sanitized.startsWith("//")) {
        val scheme = baseLink?.let { runCatching { java.net.URI(it).scheme }.getOrNull() }
            ?: "https"
        return "$scheme:$sanitized"
    }
    if (sanitized.startsWith("http://") || sanitized.startsWith("https://") ||
        sanitized.startsWith("file://") || sanitized.startsWith("/")
    ) {
        if (sanitized.startsWith("/") && !baseLink.isNullOrBlank()) {
            return resolveRelativeUrl(baseLink, sanitized)
        }
        if (sanitized.startsWith("/") && baseLink.isNullOrBlank()) {
            return if (java.io.File(sanitized).exists()) sanitized else null
        }
        return sanitized
    }
    if (!sanitized.contains("://")) {
        if (sanitized.startsWith("www.", ignoreCase = true)) {
            return "https://$sanitized"
        }
        if (!baseLink.isNullOrBlank()) {
            return resolveRelativeUrl(baseLink, sanitized)
        }
    }
    return null
}

private fun resolveRelativeUrl(baseLink: String, relative: String): String? {
    return try {
        java.net.URL(java.net.URL(baseLink), relative).toString()
    } catch (e: Exception) {
        null
    }
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
