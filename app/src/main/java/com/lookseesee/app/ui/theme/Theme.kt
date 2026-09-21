package com.lookseesee.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppPrimary = Color(0xFF3B6FB6)
val AppPrimaryDark = Color(0xFF26507F)
val AppBackground = Color(0xFFF5F7FA)
val SunsetDusk = Color(0xFF2B1B3D)
val SunsetOrange = Color(0xFFFF8A5B)
val SunsetPink = Color(0xFFFFC6A8)
val SunsetGold = Color(0xFFFFD37A)

private val LightColors = lightColorScheme(
    primary = AppPrimary,
    secondary = AppPrimaryDark,
    background = AppBackground,
)

private val DarkColors = darkColorScheme(
    primary = AppPrimary,
    secondary = AppPrimaryDark,
    background = SunsetDusk,
)

@Composable
fun LookSeeSeeTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
