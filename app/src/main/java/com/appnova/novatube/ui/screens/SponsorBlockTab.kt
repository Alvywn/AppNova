package com.appnova.novatube.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appnova.novatube.config.SkipMode
import com.appnova.novatube.config.SponsorBlockConfig
import com.appnova.novatube.config.SponsorCategory
import com.appnova.novatube.ui.components.GlassCard
import com.appnova.novatube.ui.components.GlassToggleRow
import com.appnova.novatube.ui.components.SectionHeader
import com.appnova.novatube.ui.theme.NovaColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SponsorBlockTab(
    sponsorBlock: SponsorBlockConfig,
    searchQuery: String,
    onUpdate: ((SponsorBlockConfig) -> SponsorBlockConfig) -> Unit,
    onToggleCategory: (SponsorCategory, Boolean) -> Unit,
    onSetSkipMode: (SkipMode) -> Unit
) {
    val q = searchQuery.trim().lowercase()
    fun matches(text: String) = q.isEmpty() || text.lowercase().contains(q)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (matches("sponsorblock skip toast markers mode")) {
            GlassCard {
                SectionHeader("SponsorBlock", "Powered by sponsor.ajay.app")
                GlassToggleRow(
                    title = "Enable SponsorBlock",
                    subtitle = "Fetch & skip community-submitted video segments",
                    checked = sponsorBlock.enabled,
                    onCheckedChange = { v -> onUpdate { it.copy(enabled = v) } }
                )
                GlassToggleRow(
                    title = "Show Skip Toast Alert",
                    subtitle = "Display Liquid Glass notification when a segment is skipped",
                    checked = sponsorBlock.showSkipToast,
                    onCheckedChange = { v -> onUpdate { it.copy(showSkipToast = v) } }
                )
                GlassToggleRow(
                    title = "Seekbar Segment Markers",
                    subtitle = "Highlight sponsor segments on the player timeline",
                    checked = sponsorBlock.showSeekbarMarkers,
                    onCheckedChange = { v -> onUpdate { it.copy(showSeekbarMarkers = v) } }
                )

                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Skip Mode")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkipMode.entries.forEach { mode ->
                        FilterChip(
                            selected = sponsorBlock.skipMode == mode,
                            onClick = { onSetSkipMode(mode) },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NovaColors.AccentRed.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        GlassCard {
            SectionHeader("Categories", "Choose which segments to skip or mark")
            SponsorCategory.entries.forEach { category ->
                if (matches(category.displayName + " " + category.apiName)) {
                    GlassToggleRow(
                        title = category.displayName,
                        subtitle = "API category: ${category.apiName}",
                        checked = category in sponsorBlock.enabledCategories,
                        onCheckedChange = { enabled -> onToggleCategory(category, enabled) },
                        leadingDotColor = Color(category.defaultColorHex)
                    )
                }
            }
        }
    }
}
