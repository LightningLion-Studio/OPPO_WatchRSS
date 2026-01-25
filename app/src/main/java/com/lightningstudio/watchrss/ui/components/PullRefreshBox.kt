package com.lightningstudio.watchrss.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

@Composable
fun PullRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorPadding: Dp = 0.dp,
    isAtTop: () -> Boolean = { true },
    content: @Composable BoxScope.() -> Unit
) {
    val thresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val refreshState = rememberUpdatedState(onRefresh)
    val isRefreshingState = rememberUpdatedState(isRefreshing)
    var pullDistance by remember { mutableFloatStateOf(0f) }
    var triggered by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            triggered = false
        }
    }
    val connection = remember(thresholdPx, isAtTop) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && pullDistance > 0f) {
                    val newValue = (pullDistance + available.y).coerceAtLeast(0f)
                    val consumed = newValue - pullDistance
                    pullDistance = newValue
                    if (pullDistance <= 0f) {
                        triggered = false
                    }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isRefreshingState.value) {
                    pullDistance = 0f
                    triggered = true
                    return Offset.Zero
                }
                if (isAtTop() && available.y > 0f) {
                    pullDistance += available.y
                    if (!triggered && pullDistance >= thresholdPx) {
                        triggered = true
                        refreshState.value()
                    }
                    return Offset(0f, available.y)
                }
                if (!isAtTop()) {
                    pullDistance = 0f
                    triggered = false
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pullDistance = 0f
                triggered = false
                return Velocity.Zero
            }
        }
    }
    Box(modifier = modifier.nestedScroll(connection)) {
        content()
        val progress = (pullDistance / thresholdPx).coerceIn(0f, 1f)
        val shouldShow = isRefreshing || progress > 0f
        if (shouldShow) {
            CircularProgressIndicator(
                progress = { if (isRefreshing) 1f else progress },
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = indicatorPadding)
                    .alpha(if (isRefreshing) 1f else progress.coerceAtLeast(0.2f))
            )
        }
    }
}
