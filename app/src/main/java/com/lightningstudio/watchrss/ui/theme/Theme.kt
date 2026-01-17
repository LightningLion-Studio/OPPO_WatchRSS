package com.lightningstudio.watchrss.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OppoOrange,
    secondary = OppoOrangeDark,
    background = WatchBackground,
    surface = WatchSurface,
    surfaceVariant = WatchSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = WatchTextPrimary,
    onSurface = WatchTextPrimary,
    onSurfaceVariant = WatchTextSecondary,
    outline = WatchDivider
)

private val LightColorScheme = lightColorScheme(
    primary = OppoOrange,
    secondary = OppoOrangeDark,
    background = Color(0xFFF7F4F0),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDE6DE),
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF4A4A4A),
    outline = Color(0xFFDDD4CB)
)

@Composable
fun WatchRSSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
