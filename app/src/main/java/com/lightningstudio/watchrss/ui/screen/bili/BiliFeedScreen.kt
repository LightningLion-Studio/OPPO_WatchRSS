package com.lightningstudio.watchrss.ui.screen.bili

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.ui.components.EmptyStateCard
import com.lightningstudio.watchrss.ui.viewmodel.BiliFeedUiState

@Composable
fun BiliFeedScreen(
    uiState: BiliFeedUiState,
    onLoginClick: () -> Unit,
    onRefresh: () -> Unit,
    onOpenWatchLater: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorites: () -> Unit,
    onItemClick: (com.lightningstudio.watchrss.sdk.bili.BiliItem) -> Unit
) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val itemSpacing = dimensionResource(R.dimen.hey_distance_8dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = safePadding),
            contentPadding = PaddingValues(
                top = safePadding,
                bottom = safePadding + itemSpacing
            ),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            item {
                BiliFeedHeader(
                    isLoggedIn = uiState.isLoggedIn,
                    onLoginClick = onLoginClick,
                    onRefresh = onRefresh,
                    onOpenWatchLater = onOpenWatchLater,
                    onOpenHistory = onOpenHistory,
                    onOpenFavorites = onOpenFavorites
                )
            }
            if (!uiState.isLoggedIn) {
                item {
                    EmptyStateCard(
                        title = "需要登录",
                        subtitle = "登录后获取推荐内容"
                    )
                }
            } else if (uiState.items.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "暂无内容",
                        subtitle = "稍后再试或刷新"
                    )
                }
            } else {
                items(uiState.items, key = { it.bvid ?: it.aid ?: it.title.orEmpty() }) { item ->
                    BiliVideoCard(
                        title = item.title.orEmpty(),
                        subtitle = item.owner?.name,
                        coverUrl = item.cover,
                        durationSeconds = item.duration,
                        onClick = { onItemClick(item) }
                    )
                }
            }
            item {
                if (uiState.isRefreshing) {
                    Text(
                        text = "加载中...",
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
private fun BiliFeedHeader(
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    onRefresh: () -> Unit,
    onOpenWatchLater: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val titleSize = textSize(R.dimen.hey_m_title)
    val captionSize = textSize(R.dimen.hey_caption)
    val spacing = dimensionResource(R.dimen.hey_distance_6dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Text(
            text = "哔哩哔哩",
            color = Color.White,
            fontSize = titleSize
        )
        Text(
            text = if (isLoggedIn) "已登录" else "未登录",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = captionSize
        )
        if (isLoggedIn) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                BiliPillButton(text = "刷新", onClick = onRefresh)
                BiliPillButton(text = "稍后再看", onClick = onOpenWatchLater)
            }
            Spacer(modifier = Modifier.height(spacing))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                BiliPillButton(text = "历史记录", onClick = onOpenHistory)
                BiliPillButton(text = "收藏夹", onClick = onOpenFavorites)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                BiliPillButton(text = "登录", onClick = onLoginClick)
                BiliPillButton(text = "刷新", onClick = onRefresh)
            }
        }
    }
}

@Composable
private fun textSize(id: Int): androidx.compose.ui.unit.TextUnit {
    return androidx.compose.ui.platform.LocalDensity.current.run {
        dimensionResource(id).toSp()
    }
}
