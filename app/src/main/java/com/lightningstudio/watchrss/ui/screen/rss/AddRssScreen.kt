package com.lightningstudio.watchrss.ui.screen.rss

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightningstudio.watchrss.ui.components.WatchSurface
import com.lightningstudio.watchrss.ui.theme.OppoOrange
import com.lightningstudio.watchrss.ui.viewmodel.AddRssUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AddRssScreen(
    uiState: StateFlow<AddRssUiState>,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onChannelAdded: (Long) -> Unit,
    onConsumed: () -> Unit
) {
    val state by uiState.collectAsState()

    LaunchedEffect(state.createdChannelId) {
        val channelId = state.createdChannelId
        if (channelId != null) {
            onChannelAdded(channelId)
            onConsumed()
        }
    }

    WatchSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "添加 RSS",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = state.url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "订阅地址") },
                placeholder = { Text(text = "https://example.com/feed.xml") },
                singleLine = true
            )

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "返回")
                }
                Button(
                    onClick = onSubmit,
                    enabled = !state.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = OppoOrange)
                ) {
                    Text(text = if (state.isSubmitting) "添加中" else "添加")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "支持 RSS/Atom/RDF，添加后会自动拉取最新内容。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
