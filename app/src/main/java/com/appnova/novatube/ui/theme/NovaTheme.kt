package com.appnova.novatube.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NovaColors {
    val OledBlack = Color(0xFF000000)
    val GlassSurface = Color(0xCC12141A)
    val GlassSurfaceElevated = Color(0xE61B1E26)
    val GlassBorder = Color(0x38FFFFFF)
    val AccentRed = Color(0xFFFF2D55)
    val AccentCyan = Color(0xFF38BDF8)
    val AccentGreen = Color(0xFF22C55E)
    val AccentAmber = Color(0xFFF59E0B)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
}

private val NovaDarkScheme = darkColorScheme(
    primary = NovaColors.AccentRed,
    secondary = NovaColors.AccentCyan,
    background = NovaColors.OledBlack,
    surface = NovaColors.GlassSurface,
    surfaceVariant = NovaColors.GlassSurfaceElevated,
    onPrimary = Color.White,
    onBackground = NovaColors.TextPrimary,
    onSurface = NovaColors.TextPrimary,
    onSurfaceVariant = NovaColors.TextSecondary,
    outline = NovaColors.GlassBorder
)

@Composable
fun NovaTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NovaDarkScheme,
        content = content
    )
}
