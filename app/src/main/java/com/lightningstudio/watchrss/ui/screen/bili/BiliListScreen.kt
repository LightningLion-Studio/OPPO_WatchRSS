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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.ui.components.EmptyStateCard
import com.lightningstudio.watchrss.ui.viewmodel.BiliListItem
import com.lightningstudio.watchrss.ui.viewmodel.BiliListUiState

@Composable
fun BiliListScreen(
    uiState: BiliListUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (BiliListItem) -> Unit
) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val itemSpacing = dimensionResource(R.dimen.hey_distance_8dp)
    val spacing = dimensionResource(R.dimen.hey_distance_6dp)

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
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    Text(
                        text = uiState.type.title,
                        color = Color.White,
                        fontSize = textSize(R.dimen.hey_m_title)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        BiliPillButton(text = "刷新", onClick = onRefresh)
                        if (uiState.canLoadMore) {
                            BiliPillButton(text = "加载更多", onClick = onLoadMore)
                        }
                    }
                }
            }
            if (uiState.items.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyStateCard(
                        title = "暂无内容",
                        subtitle = "稍后再试"
                    )
                }
            } else {
                items(uiState.items, key = { it.bvid ?: it.aid ?: it.title }) { item ->
                    BiliVideoCard(
                        title = item.title,
                        subtitle = item.subtitle,
                        coverUrl = item.cover,
                        durationSeconds = item.durationSeconds,
                        onClick = { onItemClick(item) }
                    )
                }
            }
            item {
                if (uiState.isLoading) {
                    Text(
                        text = "加载中...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
