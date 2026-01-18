package com.lightningstudio.watchrss.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lightningstudio.watchrss.R

private val WatchFontFamily = FontFamily(
    Font(R.font.oppo_sans, weight = FontWeight.Normal),
    Font(R.font.oppo_sans, weight = FontWeight.Medium),
    Font(R.font.oppo_sans, weight = FontWeight.SemiBold)
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = WatchFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.028.sp,
        lineHeight = 29.125.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = WatchFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.387.sp,
        lineHeight = 24.756.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = WatchFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.747.sp,
        lineHeight = 20.387.sp
    ),
    titleMedium = TextStyle(
        fontFamily = WatchFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.834.sp,
        lineHeight = 17.475.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = WatchFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.834.sp,
        lineHeight = 17.475.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = WatchFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.65.sp,
        lineHeight = 14.562.sp
    ),
    bodySmall = TextStyle(
        fontFamily = WatchFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.194.sp,
        lineHeight = 13.106.sp
    ),
    labelSmall = TextStyle(
        fontFamily = WatchFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 8.009.sp,
        lineHeight = 10.194.sp
    )
)
