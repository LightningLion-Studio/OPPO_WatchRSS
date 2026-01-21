package com.lightningstudio.watchrss.ui.screen.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SettingsScreen(
    cacheLimitMb: StateFlow<Long>,
    cacheUsageMb: StateFlow<Long>,
    readingThemeDark: StateFlow<Boolean>,
    detailProgressIndicatorEnabled: StateFlow<Boolean>,
    shareUseSystem: StateFlow<Boolean>,
    readingFontSizeSp: StateFlow<Int>,
    onSelectCacheLimit: (Long) -> Unit,
    onToggleReadingTheme: () -> Unit,
    onToggleProgressIndicator: () -> Unit,
    onToggleShareMode: () -> Unit,
    onSelectFontSize: (Int) -> Unit
) {
    val cacheLimit by cacheLimitMb.collectAsState()
    val usage by cacheUsageMb.collectAsState()
    val themeDark by readingThemeDark.collectAsState()
    val progressEnabled by detailProgressIndicatorEnabled.collectAsState()
    val useSystemShare by shareUseSystem.collectAsState()
    val fontSizeSp by readingFontSizeSp.collectAsState()

    val cacheOptions = remember {
        listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L, 200L, 300L)
    }
    val fontOptions = remember { (12..32 step 2).toList() }

    val lowerCache = cacheOptions.lastOrNull { it < cacheLimit }
    val higherCache = cacheOptions.firstOrNull { it > cacheLimit }
    val lowerFont = fontOptions.lastOrNull { it < fontSizeSp }
    val higherFont = fontOptions.firstOrNull { it > fontSizeSp }

    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val sectionSpacing = dimensionResource(HeytapR.dimen.hey_content_horizontal_distance)
    val entrySpacing = dimensionResource(HeytapR.dimen.hey_distance_8dp)
    val valueSpacing = dimensionResource(HeytapR.dimen.hey_distance_4dp)
    val stepperSpacing = dimensionResource(HeytapR.dimen.hey_distance_6dp)
    val valueIndent = dimensionResource(HeytapR.dimen.hey_content_horizontal_distance_6_0)
    val pillHeight = dimensionResource(HeytapR.dimen.hey_multiple_item_height)

    WatchSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(safePadding)
        ) {
            SettingsHeader()

            Spacer(modifier = Modifier.height(sectionSpacing))

            SettingsPillRow(label = "缓存上限") {
                RoundIconButton(
                    text = "-",
                    enabled = lowerCache != null,
                    onClick = { lowerCache?.let(onSelectCacheLimit) }
                )
                Spacer(modifier = Modifier.width(stepperSpacing))
                Text(
                    text = "${cacheLimit}M",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(stepperSpacing))
                RoundIconButton(
                    text = "+",
                    enabled = higherCache != null,
                    onClick = { higherCache?.let(onSelectCacheLimit) }
                )
            }
            Text(
                text = "当前已用 ${usage}MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = valueIndent, top = valueSpacing)
            )

            Spacer(modifier = Modifier.height(entrySpacing))

            SettingsPillRow(label = "阅读主题") {
                Switch(checked = themeDark, onCheckedChange = { onToggleReadingTheme() })
            }
            Text(
                text = if (themeDark) "深色" else "浅色",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = valueIndent, top = valueSpacing)
            )

            Spacer(modifier = Modifier.height(entrySpacing))

            SettingsPillRow(label = "阅读进度条") {
                Switch(checked = progressEnabled, onCheckedChange = { onToggleProgressIndicator() })
            }
            Text(
                text = if (progressEnabled) "开启" else "关闭",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = valueIndent, top = valueSpacing)
            )

            Spacer(modifier = Modifier.height(entrySpacing))

            SettingsPillRow(label = "分享方式") {
                Switch(checked = useSystemShare, onCheckedChange = { onToggleShareMode() })
            }
            Text(
                text = if (useSystemShare) "系统分享" else "二维码",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = valueIndent, top = valueSpacing)
            )

            Spacer(modifier = Modifier.height(entrySpacing))

            SettingsPillRow(label = "字体大小") {
                RoundIconButton(
                    text = "-",
                    enabled = lowerFont != null,
                    onClick = { lowerFont?.let(onSelectFontSize) }
                )
                Spacer(modifier = Modifier.width(stepperSpacing))
                Text(
                    text = "${fontSizeSp}sp",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(stepperSpacing))
                RoundIconButton(
                    text = "+",
                    enabled = higherFont != null,
                    onClick = { higherFont?.let(onSelectFontSize) }
                )
            }
            Text(
                text = "正文阅读字号",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = valueIndent, top = valueSpacing)
            )

            Spacer(modifier = Modifier.height(pillHeight))
        }
    }
}

@Composable
private fun SettingsHeader() {
    val padding = dimensionResource(HeytapR.dimen.hey_distance_4dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsPillRow(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    val pillColor = colorResource(R.color.watch_pill_background)
    val pillRadius = dimensionResource(HeytapR.dimen.hey_button_default_radius)
    val pillHeight = dimensionResource(HeytapR.dimen.hey_multiple_item_height)
    val startPadding = dimensionResource(HeytapR.dimen.hey_content_horizontal_distance_6_0)
    val endPadding = dimensionResource(HeytapR.dimen.hey_distance_10dp)
    val verticalPadding = dimensionResource(HeytapR.dimen.hey_distance_8dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(pillHeight)
            .clip(RoundedCornerShape(pillRadius))
            .background(pillColor)
            .padding(
                start = startPadding,
                end = endPadding,
                top = verticalPadding,
                bottom = verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}

@Composable
private fun RoundIconButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val size = dimensionResource(HeytapR.dimen.hey_distance_20dp)
    val backgroundColor = androidx.compose.ui.graphics.Color(0xFF303030)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
