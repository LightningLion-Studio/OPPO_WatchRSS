package com.lightningstudio.watchrss.ui.screen.bili

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.ui.components.EmptyStateCard
import com.lightningstudio.watchrss.ui.viewmodel.BiliFeedUiState
import com.lightningstudio.watchrss.sdk.bili.BiliItem

@Composable
fun BiliFeedScreen(
    uiState: BiliFeedUiState,
    onLoginClick: () -> Unit,
    onRefresh: () -> Unit,
    onHeaderClick: () -> Unit,
    onItemClick: (com.lightningstudio.watchrss.sdk.bili.BiliItem) -> Unit
) {
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
                .padding(horizontal = safePadding)
                .biliPullToRefresh(
                    listState = listState,
                    enabled = !uiState.isRefreshing,
                    onRefresh = onRefresh
                ),
            state = listState,
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
                    onHeaderClick = onHeaderClick
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
                    val summary = remember(item) { buildFeedSummary(item) }
                    BiliFeedCard(
                        title = item.title.orEmpty(),
                        summary = summary,
                        coverUrl = item.cover,
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
    onHeaderClick: () -> Unit
) {
    val titleSize = textSize(R.dimen.hey_m_title)
    val captionSize = textSize(R.dimen.hey_caption)
    val spacing = dimensionResource(R.dimen.hey_distance_6dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickableWithoutRipple(onClick = onHeaderClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Text(
                text = "哔哩哔哩",
                color = Color.White,
                fontSize = titleSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (isLoggedIn) "已登录" else "未登录",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = captionSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!isLoggedIn) {
            Spacer(modifier = Modifier.height(spacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BiliPillButton(text = "登录", onClick = onLoginClick)
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

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return clickable(
        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

private fun buildFeedSummary(item: BiliItem): String {
    val owner = item.owner?.name
    val views = item.stat?.view?.let { "播放 ${formatBiliCount(it)}" }
    val parts = listOfNotNull(owner, views)
    return if (parts.isNotEmpty()) parts.joinToString(" · ") else "哔哩哔哩推荐"
}

@Composable
private fun Modifier.biliPullToRefresh(
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
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabledState.value) return Offset.Zero
                if (available.y < 0f) {
                    pullDistance = (pullDistance + available.y).coerceAtLeast(0f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!enabledState.value) return Velocity.Zero
                val shouldRefresh = pullDistance >= thresholdPx
                pullDistance = 0f
                if (shouldRefresh) {
                    refreshState.value.invoke()
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    return nestedScroll(connection)
}
