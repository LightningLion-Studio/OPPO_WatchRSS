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
import androidx.compose.foundation.shape.CircleShape
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 70.dp)
            ) {
                Text(
                    text = "添加 RSS",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(9.dp))
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

                state.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

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
                    enabled = !state.isSubmitting,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OppoOrange)
                ) {
                    Text(text = "+", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
