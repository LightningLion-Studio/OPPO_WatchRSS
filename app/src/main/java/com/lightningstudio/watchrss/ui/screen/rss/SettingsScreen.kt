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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightningstudio.watchrss.ui.components.WatchSurface
import com.lightningstudio.watchrss.ui.theme.OppoOrange
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SettingsScreen(
    cacheLimitMb: StateFlow<Long>,
    cacheUsageMb: StateFlow<Long>,
    onBack: () -> Unit,
    onSelectCacheLimit: (Long) -> Unit
) {
    val selectedLimit by cacheLimitMb.collectAsState()
    val usage by cacheUsageMb.collectAsState()
    val options = listOf(
        10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L, 200L, 300L
    )
    val lowerOption = options.lastOrNull { it < selectedLimit }
    val higherOption = options.firstOrNull { it > selectedLimit }

    WatchSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
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
                    text = "缓存设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(0.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "当前已用 ${usage}MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            CacheLimitStepper(
                sizeMb = selectedLimit,
                canDecrease = lowerOption != null,
                canIncrease = higherOption != null,
                onDecrease = { lowerOption?.let(onSelectCacheLimit) },
                onIncrease = { higherOption?.let(onSelectCacheLimit) }
            )
        }
    }
}

@Composable
private fun CacheLimitStepper(
    sizeMb: Long,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "缓存上限",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${sizeMb}MB",
                style = MaterialTheme.typography.labelLarge,
                color = OppoOrange
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDecrease, enabled = canDecrease) {
                    Text(text = "-", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(onClick = onIncrease, enabled = canIncrease) {
                    Text(text = "+", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
