package com.lookseesee.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Soft pink palette for the setup and session chrome, with a deep rose text color
// that complements rather than fights the background. A single fixed scheme rather
// than following system light/dark mode - a kiosk app benefits from one consistent
// look rather than one that silently changes on the parent's phone.
val PinkBackground = Color(0xFFFDE6EF)
val PinkSurface = Color(0xFFFBD5E3)
val RoseText = Color(0xFF6B2545)
val RosePrimary = Color(0xFFD6336C)
val RosePrimaryDark = Color(0xFFB23A5B)
val CreamOnPrimary = Color(0xFFFFF8FA)

val SunsetDusk = Color(0xFF2B1B3D)
val SunsetOrange = Color(0xFFFF8A5B)
val SunsetPink = Color(0xFFFFC6A8)
val SunsetGold = Color(0xFFFFD37A)

private val AppColors = lightColorScheme(
    primary = RosePrimary,
    onPrimary = CreamOnPrimary,
    secondary = RosePrimaryDark,
    onSecondary = CreamOnPrimary,
    background = PinkBackground,
    onBackground = RoseText,
    surface = PinkSurface,
    onSurface = RoseText,
    surfaceVariant = PinkSurface,
    onSurfaceVariant = RoseText,
    outline = RosePrimaryDark,
    outlineVariant = RosePrimary,
)

@Composable
fun LookSeeSeeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}
