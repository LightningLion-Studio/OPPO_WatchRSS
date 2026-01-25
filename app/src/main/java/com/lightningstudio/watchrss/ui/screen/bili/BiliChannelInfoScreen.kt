package com.lightningstudio.watchrss.ui.screen.bili

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import com.lightningstudio.watchrss.R

@Composable
fun BiliChannelInfoScreen(
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    onOpenWatchLater: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val spacing = dimensionResource(R.dimen.hey_distance_6dp)
    val titleSize = textSize(R.dimen.hey_m_title)
    val captionSize = textSize(R.dimen.hey_caption)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(safePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "哔哩哔哩",
            color = Color.White,
            fontSize = titleSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(spacing))
        Text(
            text = if (isLoggedIn) "已登录" else "未登录",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = captionSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(spacing))
        if (!isLoggedIn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BiliPillButton(text = "登录", onClick = onLoginClick)
            }
            Spacer(modifier = Modifier.height(spacing))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            BiliPillButton(text = "稍后再看", onClick = onOpenWatchLater, enabled = isLoggedIn)
            BiliPillButton(text = "历史记录", onClick = onOpenHistory, enabled = isLoggedIn)
            BiliPillButton(text = "收藏夹", onClick = onOpenFavorites, enabled = isLoggedIn)
        }
    }
}

@Composable
private fun textSize(id: Int): androidx.compose.ui.unit.TextUnit {
    return androidx.compose.ui.platform.LocalDensity.current.run {
        dimensionResource(id).toSp()
    }
}
