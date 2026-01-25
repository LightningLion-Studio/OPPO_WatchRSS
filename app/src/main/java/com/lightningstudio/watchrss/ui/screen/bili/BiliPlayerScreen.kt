package com.lightningstudio.watchrss.ui.screen.bili

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.viewinterop.AndroidView
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.ui.viewmodel.BiliPlayerUiState

@Composable
fun BiliPlayerScreen(
    uiState: BiliPlayerUiState,
    onRetry: () -> Unit,
    onOpenWeb: () -> Unit,
    onBack: () -> Unit
) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val spacing = dimensionResource(R.dimen.hey_distance_6dp)
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    LaunchedEffect(uiState.playUrl) {
        playbackError = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!uiState.playUrl.isNullOrBlank()) {
            AndroidView(
                factory = {
                    VideoView(context).apply {
                        setOnPreparedListener { it.start() }
                        setOnErrorListener { _, _, _ ->
                            playbackError = "播放失败"
                            true
                        }
                    }.also { videoViewRef = it }
                },
                update = { view ->
                    val tagUrl = view.tag as? String
                    val targetUrl = uiState.playUrl.orEmpty()
                    if (tagUrl != targetUrl) {
                        view.tag = targetUrl
                        view.setVideoURI(Uri.parse(targetUrl), uiState.headers)
                        view.start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        val errorText = playbackError ?: uiState.message
        if (!errorText.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(safePadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    BiliPillButton(text = "重试", onClick = onRetry)
                    BiliPillButton(text = "浏览器打开", onClick = onOpenWeb)
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(safePadding)
        ) {
            BiliPillButton(text = "返回", onClick = onBack)
        }
    }
}
