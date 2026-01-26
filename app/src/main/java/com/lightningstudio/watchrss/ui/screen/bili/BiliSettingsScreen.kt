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
import com.heytap.wearable.R as HeytapR
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.ui.components.WatchSurface

@Composable
fun BiliSettingsScreen(
    isLoggedIn: Boolean,
    onLogout: () -> Unit
) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val titleSpacing = dimensionResource(HeytapR.dimen.hey_distance_6dp)
    val infoSpacing = dimensionResource(HeytapR.dimen.hey_distance_4dp)
    val buttonWidth = dimensionResource(R.dimen.watch_action_button_width)
    val buttonHeight = dimensionResource(R.dimen.watch_action_button_height)
    val statusText = if (isLoggedIn) "已登录" else "未登录"

    WatchSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(safePadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(titleSpacing))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(infoSpacing))
            ActionButton(
                label = "退出登录",
                enabled = isLoggedIn,
                width = buttonWidth,
                height = buttonHeight,
                onClick = onLogout
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
