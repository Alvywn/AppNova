package com.appnova.novatube.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appnova.novatube.config.PlaybackConfig
import com.appnova.novatube.ui.components.GlassCard
import com.appnova.novatube.ui.components.GlassToggleRow
import com.appnova.novatube.ui.components.SectionHeader
import com.appnova.novatube.ui.theme.NovaColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaybackTab(
    playback: PlaybackConfig,
    searchQuery: String,
    onUpdate: ((PlaybackConfig) -> PlaybackConfig) -> Unit
) {
    val q = searchQuery.trim().lowercase()
    fun matches(text: String) = q.isEmpty() || text.lowercase().contains(q)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (matches("background playback pip loop")) {
            GlassCard {
                SectionHeader("Background & PiP", "Play without interruptions")
                if (matches("background playback")) {
                    GlassToggleRow(
                        title = "Background Playback",
                        subtitle = "Keep audio playing when switching apps or locking screen",
                        checked = playback.backgroundPlayEnabled,
                        onCheckedChange = { v -> onUpdate { it.copy(backgroundPlayEnabled = v) } }
                    )
                }
                if (matches("picture in picture pip")) {
                    GlassToggleRow(
                        title = "Unlock Picture-in-Picture",
                        subtitle = "Auto-enter PiP when leaving the YouTube watch screen",
                        checked = playback.pipUnlockEnabled,
                        onCheckedChange = { v -> onUpdate { it.copy(pipUnlockEnabled = v) } }
                    )
                }
                if (matches("loop repeat")) {
                    GlassToggleRow(
                        title = "Loop Video by Default",
                        subtitle = "Automatically repeat videos when playback ends",
                        checked = playback.loopByDefault,
                        onCheckedChange = { v -> onUpdate { it.copy(loopByDefault = v) } }
                    )
                }
            }
        }

        if (matches("speed playback rate")) {
            GlassCard {
                SectionHeader(
                    title = "Playback Speed",
                    subtitle = "Default speed: ${String.format("%.2fx", playback.defaultSpeed)}"
                )
                Slider(
                    value = playback.defaultSpeed,
                    onValueChange = { raw ->
                        val snapped = (raw * 20f).roundToInt() / 20f
                        onUpdate { it.copy(defaultSpeed = snapped.coerceIn(0.25f, 4.0f)) }
                    },
                    valueRange = 0.25f..4.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NovaColors.AccentRed,
                        activeTrackColor = NovaColors.AccentRed
                    )
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { speed ->
                        val selected = kotlin.math.abs(playback.defaultSpeed - speed) < 0.02f
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdate { it.copy(defaultSpeed = speed) } },
                            label = { Text("${speed}x") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NovaColors.AccentRed.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                GlassToggleRow(
                    title = "Extended Speed Options (up to 4.0x)",
                    subtitle = "Unlock custom speeds beyond YouTube's default 2.0x limit",
                    checked = playback.customSpeedOptionsEnabled,
                    onCheckedChange = { v -> onUpdate { it.copy(customSpeedOptionsEnabled = v) } }
                )
            }
        }

        if (matches("gestures brightness volume seek")) {
            GlassCard {
                SectionHeader("Player Gestures", "Swipe & double-tap controls with Liquid Glass HUD")
                GlassToggleRow(
                    title = "Enable Player Gestures",
                    subtitle = "Master switch for swipe brightness, volume, and seek",
                    checked = playback.gesturesEnabled,
                    onCheckedChange = { v -> onUpdate { it.copy(gesturesEnabled = v) } }
                )
                if (playback.gesturesEnabled) {
                    GlassToggleRow(
                        title = "Left-Side Brightness Swipe",
                        subtitle = "Swipe vertically on the left side of the player",
                        checked = playback.brightnessGestureEnabled,
                        onCheckedChange = { v -> onUpdate { it.copy(brightnessGestureEnabled = v) } }
                    )
                    GlassToggleRow(
                        title = "Right-Side Volume Swipe",
                        subtitle = "Swipe vertically on the right side of the player",
                        checked = playback.volumeGestureEnabled,
                        onCheckedChange = { v -> onUpdate { it.copy(volumeGestureEnabled = v) } }
                    )
                    GlassToggleRow(
                        title = "Double-Tap Seek HUD",
                        subtitle = "Show Liquid Glass seek indicator (${playback.seekStepSeconds}s step)",
                        checked = playback.swipeSeekEnabled,
                        onCheckedChange = { v -> onUpdate { it.copy(swipeSeekEnabled = v) } }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Seek Step", color = NovaColors.TextSecondary, fontSize = 13.sp)
                        Text(
                            "${playback.seekStepSeconds}s",
                            color = NovaColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Slider(
                        value = playback.seekStepSeconds.toFloat(),
                        onValueChange = { v ->
                            onUpdate { it.copy(seekStepSeconds = v.roundToInt().coerceIn(5, 30)) }
                        },
                        valueRange = 5f..30f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = NovaColors.AccentCyan,
                            activeTrackColor = NovaColors.AccentCyan
                        )
                    )
                }
            }
        }
    }
}
