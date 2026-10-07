package com.lookseesee.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm manila/brown palette for the setup and session chrome. Deliberately a single
// fixed scheme rather than following system light/dark mode - a kiosk app benefits
// from one consistent look rather than one that silently changes on the parent's phone.
val ManilaBackground = Color(0xFFFBEFD1)
val ManilaSurface = Color(0xFFF2DFAE)
val BrownText = Color(0xFF5C4033)
val BrownPrimary = Color(0xFF8B5E34)
val BrownPrimaryDark = Color(0xFF6B4423)
val CreamOnPrimary = Color(0xFFFFFBF0)

val SunsetDusk = Color(0xFF2B1B3D)
val SunsetOrange = Color(0xFFFF8A5B)
val SunsetPink = Color(0xFFFFC6A8)
val SunsetGold = Color(0xFFFFD37A)

private val AppColors = lightColorScheme(
    primary = BrownPrimary,
    onPrimary = CreamOnPrimary,
    secondary = BrownPrimaryDark,
    onSecondary = CreamOnPrimary,
    background = ManilaBackground,
    onBackground = BrownText,
    surface = ManilaSurface,
    onSurface = BrownText,
    surfaceVariant = ManilaSurface,
    onSurfaceVariant = BrownText,
    outline = BrownPrimaryDark,
    outlineVariant = BrownPrimary,
)

@Composable
fun LookSeeSeeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}
