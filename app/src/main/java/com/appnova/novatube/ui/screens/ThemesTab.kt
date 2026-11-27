package com.appnova.novatube.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appnova.novatube.config.NovaThemeStyle
import com.appnova.novatube.config.UiConfig
import com.appnova.novatube.ui.components.GlassCard
import com.appnova.novatube.ui.components.SectionHeader
import com.appnova.novatube.ui.theme.NovaColors

@Composable
fun ThemesTab(
    ui: UiConfig,
    searchQuery: String,
    onUpdate: ((UiConfig) -> UiConfig) -> Unit
) {
    val q = searchQuery.trim().lowercase()
    val filtered = NovaThemeStyle.entries.filter {
        q.isEmpty() || it.displayName.lowercase().contains(q) || "tema theme".contains(q)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard {
            SectionHeader(
                title = "App Themes",
                subtitle = "Mengganti tampilan seluruh aplikasi, floating menu, dan halaman web"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filtered.forEach { style ->
                    val selected = ui.themeStyle == style
                    val accent = Color(style.accentHex)
                    val surface = Color(style.surfaceHex)
                    val card = Color(style.cardHex)
                    val border = Color(style.borderHex)
                    val desc = when (style) {
                        NovaThemeStyle.LIQUID_GLASS -> "Kaca transparan biru gelap dengan efek frosted blur & border cyan es"
                        NovaThemeStyle.OLED_BLACK -> "Hitam murni #000000 hemat baterai dengan aksen merah tajam"
                        NovaThemeStyle.CYBER_NEON -> "Nuansa ungu gelap futuristik dengan aksen neon magenta & cyan"
                        NovaThemeStyle.CRIMSON_DARK -> "Gelap elegan klasik dengan aksen merah Crimson"
                        NovaThemeStyle.SAKURA_NIGHT -> "Malam plum hangat dengan aksen pink sakura lembut"
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onUpdate {
                                    it.copy(
                                        themeStyle = style,
                                        oledModeEnabled = style == NovaThemeStyle.OLED_BLACK,
                                        liquidGlassHudEnabled = style == NovaThemeStyle.LIQUID_GLASS || style == NovaThemeStyle.CYBER_NEON
                                    )
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = card,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) accent else border
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = style.displayName,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Color swatch preview
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(surface)
                                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(accent)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(border)
                                        )
                                    }
                                }
                                Text(
                                    text = desc,
                                    color = NovaColors.TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Active Theme",
                                    tint = accent,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
