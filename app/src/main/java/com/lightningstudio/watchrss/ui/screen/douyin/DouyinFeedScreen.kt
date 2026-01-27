package com.lightningstudio.watchrss.ui.screen.douyin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.sdk.douyin.DouyinVideo
import com.lightningstudio.watchrss.ui.components.EmptyStateCard
import com.lightningstudio.watchrss.ui.components.PullRefreshBox
import com.lightningstudio.watchrss.ui.components.ToastMessage
import com.lightningstudio.watchrss.ui.screen.bili.BiliFeedCard
import com.lightningstudio.watchrss.ui.screen.bili.formatBiliCount
import com.lightningstudio.watchrss.ui.viewmodel.DouyinFeedUiState

@Composable
fun DouyinFeedScreen(
    uiState: DouyinFeedUiState,
    onRefresh: () -> Unit,
    onItemClick: (DouyinVideo, Int) -> Unit
) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val itemSpacing = dimensionResource(R.dimen.hey_distance_8dp)
    val listState = rememberLazyListState()
    val isAtTop = remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PullRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            indicatorPadding = safePadding,
            isAtTop = { isAtTop.value }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = safePadding),
                state = listState,
                contentPadding = PaddingValues(
                    top = safePadding,
                    bottom = safePadding + itemSpacing
                ),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                item {
                    DouyinFeedHeader()
                }
                if (uiState.items.isEmpty()) {
                    item {
                        val title = if (uiState.message.isNullOrBlank()) "暂无内容" else "加载失败"
                        val subtitle = uiState.message ?: "下拉刷新获取推荐内容"
                        EmptyStateCard(title = title, subtitle = subtitle)
                    }
                } else {
                    itemsIndexed(uiState.items) { index, item ->
                        val title = item.desc?.ifBlank { "抖音视频" } ?: "抖音视频"
                        val summary = buildSummary(item)
                        BiliFeedCard(
                            title = title,
                            summary = summary,
                            coverUrl = item.coverUrl,
                            onClick = { onItemClick(item, index) }
                        )
                    }
                }
            }
        }

        if (!uiState.message.isNullOrBlank() && uiState.items.isNotEmpty()) {
            ToastMessage(text = uiState.message!!)
        }
    }
}

@Composable
private fun DouyinFeedHeader() {
    val headerPadding = dimensionResource(R.dimen.hey_distance_6dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = headerPadding, vertical = headerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "抖音精选",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.hey_distance_2dp)))
        Text(
            text = "下拉刷新获取最新推荐",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun buildSummary(item: DouyinVideo): String {
    val author = item.authorName?.takeIf { it.isNotBlank() } ?: "未知作者"
    val like = if (item.likeCount > 0) "赞 ${formatBiliCount(item.likeCount)}" else null
    return listOfNotNull(author, like).joinToString(" · ")
}
