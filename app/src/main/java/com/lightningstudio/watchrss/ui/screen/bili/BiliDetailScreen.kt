package com.lightningstudio.watchrss.ui.screen.bili

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.sdk.bili.BiliPage
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import com.lightningstudio.watchrss.ui.viewmodel.BiliDetailUiState

@Composable
fun BiliDetailScreen(
    uiState: BiliDetailUiState,
    onPlayClick: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onLike: () -> Unit,
    onCoin: () -> Unit,
    onFavorite: () -> Unit,
    onWatchLater: () -> Unit,
    onShare: () -> Unit
) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val spacing = dimensionResource(R.dimen.hey_distance_6dp)
    val listState = rememberLazyListState()
    val detail = uiState.detail

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = safePadding),
            contentPadding = PaddingValues(
                top = safePadding,
                bottom = safePadding + spacing
            ),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            item {
                Text(
                    text = detail?.item?.title ?: "加载中...",
                    color = Color.White,
                    fontSize = textSize(R.dimen.hey_m_title),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item {
                BiliCoverCard(
                    coverUrl = detail?.item?.cover,
                    onClick = onPlayClick
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    val owner = detail?.item?.owner?.name
                    if (!owner.isNullOrBlank()) {
                        Text(
                            text = owner,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = textSize(R.dimen.hey_caption)
                        )
                    }
                    BiliStatsRow(
                        viewCount = detail?.item?.stat?.view,
                        likeCount = detail?.item?.stat?.like,
                        danmakuCount = detail?.item?.stat?.danmaku
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        BiliPillButton(text = if (uiState.isLiked) "取消点赞" else "点赞", onClick = onLike)
                        BiliPillButton(text = "投币", onClick = onCoin)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        BiliPillButton(text = if (uiState.isFavorited) "取消收藏" else "收藏", onClick = onFavorite)
                        BiliPillButton(text = "稍后再看", onClick = onWatchLater)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        BiliPillButton(text = "分享", onClick = onShare)
                    }
                }
            }
            if (!detail?.pages.isNullOrEmpty()) {
                item {
                    Text(
                        text = "分P",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = textSize(R.dimen.hey_caption)
                    )
                }
                itemsIndexed(detail!!.pages) { index, page ->
                    BiliPageEntry(
                        page = page,
                        selected = uiState.selectedPageIndex == index,
                        onClick = { onSelectPage(index) }
                    )
                }
            }
            if (!detail?.desc.isNullOrBlank()) {
                item {
                    Text(
                        text = detail?.desc.orEmpty(),
                        color = Color.White,
                        fontSize = textSize(R.dimen.hey_s_title)
                    )
                }
            }
            if (!uiState.message.isNullOrBlank()) {
                item {
                    Text(
                        text = uiState.message.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BiliCoverCard(
    coverUrl: String?,
    onClick: () -> Unit
) {
    val radius = dimensionResource(R.dimen.hey_card_normal_bg_radius)
    val coverHeight = dimensionResource(R.dimen.hey_card_large_height)
    val context = androidx.compose.ui.platform.LocalContext.current
    val maxWidthPx = remember(context) { context.resources.displayMetrics.widthPixels.coerceAtLeast(1) }
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, coverUrl, maxWidthPx) {
        value = if (coverUrl.isNullOrBlank()) null else {
            RssImageLoader.loadBitmap(context, coverUrl, maxWidthPx)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(coverHeight)
            .clip(RoundedCornerShape(radius))
            .background(Color.Black)
            .clickableWithoutRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        val safeBitmap = bitmap
        if (safeBitmap != null) {
            Image(
                bitmap = safeBitmap.asImageBitmap(),
                contentDescription = "视频封面",
                modifier = Modifier.fillMaxSize()
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_play_circle),
            contentDescription = "播放",
            modifier = Modifier.size(dimensionResource(R.dimen.hey_listitem_widget_size))
        )
    }
}

@Composable
private fun BiliPageEntry(
    page: BiliPage,
    selected: Boolean,
    onClick: () -> Unit
) {
    val radius = dimensionResource(R.dimen.hey_card_normal_bg_radius)
    val background = if (selected) Color(0xFF2A2A2A) else Color(0xFF1B1B1B)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(background)
            .clickableWithoutRipple(onClick)
            .padding(dimensionResource(R.dimen.hey_distance_6dp))
    ) {
        Text(
            text = page.part?.ifBlank { null } ?: "第${page.page ?: 1}集",
            color = Color.White,
            fontSize = textSize(R.dimen.hey_s_title),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun textSize(id: Int): androidx.compose.ui.unit.TextUnit {
    return androidx.compose.ui.platform.LocalDensity.current.run {
        dimensionResource(id).toSp()
    }
}

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return clickable(
        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
