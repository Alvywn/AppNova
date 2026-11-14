package com.appnova.novatube.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appnova.novatube.config.UiConfig
import com.appnova.novatube.ui.components.GlassCard
import com.appnova.novatube.ui.components.GlassToggleRow
import com.appnova.novatube.ui.components.SectionHeader
import com.appnova.novatube.ui.theme.NovaColors

@Composable
fun UiCustomizationTab(
    ui: UiConfig,
    searchQuery: String,
    onUpdate: ((UiConfig) -> UiConfig) -> Unit,
    onToggleTabVisibility: (String) -> Unit,
    onMoveTab: (String, Int) -> Unit
) {
    val q = searchQuery.trim().lowercase()
    fun matches(text: String) = q.isEmpty() || text.lowercase().contains(q)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (matches("adblock brave shields ads block iklan oled black liquid glass theme hud")) {
            GlassCard {
                SectionHeader("AdBlock & Appearance", "Brave Shields 3-Layer AdBlock + YTKACE OLED")
                GlassToggleRow(
                    title = "Brave Shields AdBlock",
                    subtitle = "Network filter + JSON-prune scriptlet (adPlacements/playerAds) + Cosmetic blocker",
                    checked = ui.adBlockEnabled,
                    onCheckedChange = { v -> onUpdate { it.copy(adBlockEnabled = v) } }
                )
                GlassToggleRow(
                    title = "OLED Pure Black Mode",
                    subtitle = "Replace dark gray YouTube surfaces with true #000000 black",
                    checked = ui.oledModeEnabled,
                    onCheckedChange = { v -> onUpdate { it.copy(oledModeEnabled = v) } }
                )
                GlassToggleRow(
                    title = "Liquid Glass Overlays",
                    subtitle = "Frosted translucent pill styling for Gesture HUD & Skip button",
                    checked = ui.liquidGlassHudEnabled,
                    onCheckedChange = { v -> onUpdate { it.copy(liquidGlassHudEnabled = v) } }
                )
            }
        }

        if (matches("cleanup hide shorts create cast watermark")) {
            GlassCard {
                SectionHeader("Navigation & Feed Cleanup", "Remove clutter from YouTube UI")
                GlassToggleRow(
                    title = "Hide Create (+) Button",
                    subtitle = "Remove the upload/create button from the bottom bar",
                    checked = ui.hideCreateButton,
                    onCheckedChange = { v -> onUpdate { it.copy(hideCreateButton = v) } }
                )
                GlassToggleRow(
                    title = "Hide Cast Button",
                    subtitle = "Remove the Chromecast button from the player & top bar",
                    checked = ui.hideCastButton,
                    onCheckedChange = { v -> onUpdate { it.copy(hideCastButton = v) } }
                )
                GlassToggleRow(
                    title = "Hide Shorts Shelf",
                    subtitle = "Hide Shorts carousels in Home & Search feeds",
                    checked = ui.hideShortsShelf,
                    onCheckedChange = { v -> onUpdate { it.copy(hideShortsShelf = v) } }
                )
                GlassToggleRow(
                    title = "Hide Channel Watermark",
                    subtitle = "Hide branding watermark overlay inside the video player",
                    checked = ui.hideWatermark,
                    onCheckedChange = { v -> onUpdate { it.copy(hideWatermark = v) } }
                )
            }
        }

        if (matches("tab editor home shorts subscriptions you")) {
            GlassCard {
                SectionHeader("Tab Editor", "Show, hide, or reorder bottom navigation tabs")
                ui.tabs.forEachIndexed { index, tab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tab.title,
                                color = if (tab.visible) NovaColors.TextPrimary else NovaColors.TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (tab.visible) "Visible" else "Hidden",
                                color = NovaColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { onMoveTab(tab.id, -1) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) NovaColors.TextPrimary else NovaColors.TextSecondary.copy(alpha = 0.3f)
                                )
                            }
                            IconButton(
                                onClick = { onMoveTab(tab.id, 1) },
                                enabled = index < ui.tabs.lastIndex
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = if (index < ui.tabs.lastIndex) NovaColors.TextPrimary else NovaColors.TextSecondary.copy(alpha = 0.3f)
                                )
                            }
                            IconButton(onClick = { onToggleTabVisibility(tab.id) }) {
                                Icon(
                                    imageVector = if (tab.visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = if (tab.visible) NovaColors.AccentCyan else NovaColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
