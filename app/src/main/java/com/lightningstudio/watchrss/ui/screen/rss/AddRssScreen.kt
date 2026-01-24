package com.lightningstudio.watchrss.ui.screen.rss

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lightningstudio.watchrss.data.rss.RssChannel
import com.lightningstudio.watchrss.ui.components.WatchSurface
import com.lightningstudio.watchrss.ui.theme.OppoOrange
import com.lightningstudio.watchrss.ui.viewmodel.AddRssUiState
import com.lightningstudio.watchrss.ui.viewmodel.AddRssStep
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AddRssScreen(
    uiState: StateFlow<AddRssUiState>,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onBackToInput: () -> Unit,
    onOpenExisting: (RssChannel) -> Unit,
    onChannelAdded: (String?, Long) -> Unit,
    onConsumed: () -> Unit,
    onClearError: () -> Unit
) {
    val state by uiState.collectAsState()

    LaunchedEffect(state.createdChannelId) {
        val channelId = state.createdChannelId
        if (channelId != null) {
            onChannelAdded(state.url, channelId)
            onConsumed()
        }
    }

    WatchSurface {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(bottom = 70.dp)
            ) {
                Text(
                    text = "添加 RSS",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(9.dp))
                when (state.step) {
                    AddRssStep.INPUT -> {
                        OutlinedTextField(
                            value = state.url,
                            onValueChange = onUrlChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "订阅地址") },
                            placeholder = { Text(text = "https://example.com/feed.xml") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "支持 RSS/Atom/RDF，添加后会自动拉取最新内容。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (state.isLoadingPreview) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "解析中...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        state.errorMessage?.let { message ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            val showActions = message != "请输入 RSS 地址" && message != "URL 不合法"
                            if (showActions) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = onSubmit
                                    ) {
                                        Text(text = "重试")
                                    }
                                    TextButton(onClick = onClearError) {
                                        Text(text = "取消")
                                    }
                                }
                            }
                        }
                    }
                    AddRssStep.PREVIEW -> {
                        val preview = state.preview
                        Text(
                            text = preview?.title ?: "频道预览",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preview?.description?.ifBlank { null } ?: "暂无简介",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        state.errorMessage?.let { message ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    AddRssStep.EXISTING -> {
                        val existing = state.existingChannel
                        Text(
                            text = existing?.title?.let { "已存在：$it" } ?: "已存在该订阅",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "无需重复添加，可直接进入频道。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.errorMessage?.let { message ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            when (state.step) {
                AddRssStep.INPUT -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 26.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onBack) {
                            Text(text = "返回")
                        }
                        Button(
                            onClick = onSubmit,
                            enabled = !state.isSubmitting && !state.isLoadingPreview,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OppoOrange)
                        ) {
                            Text(text = "+", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                AddRssStep.PREVIEW -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConfirm,
                            enabled = !state.isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = OppoOrange)
                        ) {
                            Text(
                                text = if (state.isSubmitting) "添加中" else "确认添加",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        TextButton(onClick = onBackToInput) {
                            Text(text = "修改地址")
                        }
                    }
                }
                AddRssStep.EXISTING -> {
                    val existing = state.existingChannel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                if (existing != null) {
                                    onOpenExisting(existing)
                                }
                            },
                            enabled = existing != null
                        ) {
                            Text(text = "跳转频道", color = OppoOrange)
                        }
                        TextButton(onClick = onBackToInput) {
                            Text(text = "返回")
                        }
                    }
                }
            }
        }
    }
}
