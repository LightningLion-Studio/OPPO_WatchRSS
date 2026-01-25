package com.lightningstudio.watchrss.ui.screen.bili

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.heytap.wearable.R as HeytapR
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.ui.components.WatchSurface
import com.lightningstudio.watchrss.ui.util.formatTime

@Composable
fun BiliChannelInfoScreen(
    isLoggedIn: Boolean,
    lastRefreshAt: Long?,
    onLoginClick: () -> Unit,
    onOpenWatchLater: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val titleSpacing = dimensionResource(HeytapR.dimen.hey_distance_6dp)
    val infoSpacing = dimensionResource(HeytapR.dimen.hey_distance_4dp)
    val buttonSpacing = dimensionResource(HeytapR.dimen.hey_distance_4dp)
    val buttonWidth = dimensionResource(R.dimen.watch_action_button_width)
    val buttonHeight = dimensionResource(R.dimen.watch_action_button_height)
    val description = if (isLoggedIn) {
        "已登录，可查看推荐内容"
    } else {
        "未登录，登录后获取推荐内容"
    }
    val url = "https://www.bilibili.com"
    val updatedText = lastRefreshAt?.let { formatTime(it) } ?: "--"

    WatchSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(safePadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "哔哩哔哩",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(titleSpacing))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(infoSpacing))
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(infoSpacing))
            Text(
                text = "更新: $updatedText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(infoSpacing))
            Text(
                text = "未读 0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(buttonSpacing))

            if (!isLoggedIn) {
                ActionButton(
                    label = "登录",
                    enabled = true,
                    width = buttonWidth,
                    height = buttonHeight,
                    onClick = onLoginClick
                )
                Spacer(modifier = Modifier.height(buttonSpacing))
            }

            ActionButton(
                label = "稍后再看",
                enabled = isLoggedIn,
                width = buttonWidth,
                height = buttonHeight,
                onClick = onOpenWatchLater
            )

            Spacer(modifier = Modifier.height(buttonSpacing))

            ActionButton(
                label = "历史记录",
                enabled = isLoggedIn,
                width = buttonWidth,
                height = buttonHeight,
                onClick = onOpenHistory
            )

            Spacer(modifier = Modifier.height(buttonSpacing))

            ActionButton(
                label = "收藏夹",
                enabled = isLoggedIn,
                width = buttonWidth,
                height = buttonHeight,
                onClick = onOpenFavorites
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val pillColor = colorResource(R.color.watch_pill_background)
    val pillRadius = dimensionResource(HeytapR.dimen.hey_button_default_radius)

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(pillRadius))
            .background(pillColor)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
