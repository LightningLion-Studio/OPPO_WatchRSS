package com.lightningstudio.watchrss.ui.screen.rss

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lightningstudio.watchrss.WebViewActivity
import com.lightningstudio.watchrss.data.rss.RssItem
import com.lightningstudio.watchrss.ui.components.WatchSurface
import com.lightningstudio.watchrss.ui.theme.OppoOrange
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DetailScreen(
    item: StateFlow<RssItem?>,
    onBack: () -> Unit
) {
    val detail by item.collectAsState()
    val context = LocalContext.current

    WatchSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "返回")
                }
                Text(
                    text = "详情",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!detail?.link.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            detail?.link?.let { link ->
                                context.startActivity(WebViewActivity.createIntent(context, link))
                            }
                        }
                    ) {
                        Text(text = "打开", color = OppoOrange)
                    }
                } else {
                    Spacer(modifier = Modifier.width(36.dp))
                }
            }

            if (detail == null) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "加载中...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = detail!!.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            val contentText = detail!!.content?.ifBlank { null }
                ?: detail!!.description?.ifBlank { null }
                ?: "暂无正文"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = contentText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(9.dp))
            }
        }
    }
}
