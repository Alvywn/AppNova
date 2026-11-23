package com.appnova.novatube.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.appnova.novatube.config.NovaThemeStyle

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

val LocalNovaThemeStyle = compositionLocalOf { NovaThemeStyle.LIQUID_GLASS }

@Composable
fun NovaTubeTheme(
    themeStyle: NovaThemeStyle = NovaThemeStyle.LIQUID_GLASS,
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = Color(themeStyle.accentHex),
        secondary = Color(themeStyle.accentHex),
        background = Color(themeStyle.surfaceHex),
        surface = Color(themeStyle.cardHex),
        surfaceVariant = Color(themeStyle.cardHex),
        onPrimary = Color.Black,
        onBackground = NovaColors.TextPrimary,
        onSurface = NovaColors.TextPrimary,
        onSurfaceVariant = NovaColors.TextSecondary,
        outline = Color(themeStyle.borderHex)
    )
    CompositionLocalProvider(LocalNovaThemeStyle provides themeStyle) {
        MaterialTheme(
            colorScheme = scheme,
            content = content
        )
    }
}
