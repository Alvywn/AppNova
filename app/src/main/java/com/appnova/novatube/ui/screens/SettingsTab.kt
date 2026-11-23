package com.appnova.novatube.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appnova.novatube.config.NovaConfig
import com.appnova.novatube.config.PlaybackConfig
import com.appnova.novatube.config.SkipMode
import com.appnova.novatube.config.SponsorBlockConfig
import com.appnova.novatube.config.SponsorCategory
import com.appnova.novatube.config.UiConfig
import com.appnova.novatube.ui.theme.LocalNovaThemeStyle
import com.appnova.novatube.ui.theme.NovaColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(
    config: NovaConfig,
    onUpdatePlayback: ((PlaybackConfig) -> PlaybackConfig) -> Unit,
    onUpdateSponsorBlock: ((SponsorBlockConfig) -> SponsorBlockConfig) -> Unit,
    onToggleSponsorCategory: (SponsorCategory, Boolean) -> Unit,
    onSetSkipMode: (SkipMode) -> Unit,
    onUpdateUi: ((UiConfig) -> UiConfig) -> Unit,
    onToggleTabVisibility: (String) -> Unit,
    onMoveTab: (String, Int) -> Unit,
    onResetDefaults: () -> Unit
) {
    var settingsSection by remember { mutableIntStateOf(0) }
    var filterQuery by remember { mutableStateOf("") }
    val sections = listOf("Playback", "SponsorBlock", "Themes", "UI & Tabs", "Updates")
    val activeTheme = LocalNovaThemeStyle.current
    val accentColor = Color(activeTheme.accentHex)
    val cardColor = Color(activeTheme.cardHex)
    val borderColor = Color(activeTheme.borderHex)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Search settings...", color = NovaColors.TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = NovaColors.TextSecondary)
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = NovaColors.TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = cardColor,
                    unfocusedContainerColor = cardColor,
                    focusedTextColor = NovaColors.TextPrimary,
                    unfocusedTextColor = NovaColors.TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sections.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = settingsSection == idx,
                        onClick = { settingsSection = idx },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.25f),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                when (settingsSection) {
                    0 -> PlaybackTab(
                        playback = config.playback,
                        searchQuery = filterQuery,
                        onUpdate = onUpdatePlayback
                    )
                    1 -> SponsorBlockTab(
                        sponsorBlock = config.sponsorBlock,
                        searchQuery = filterQuery,
                        onUpdate = onUpdateSponsorBlock,
                        onToggleCategory = onToggleSponsorCategory,
                        onSetSkipMode = onSetSkipMode
                    )
                    2 -> ThemesTab(
                        ui = config.ui,
                        searchQuery = filterQuery,
                        onUpdate = onUpdateUi
                    )
                    3 -> UiCustomizationTab(
                        ui = config.ui,
                        searchQuery = filterQuery,
                        onUpdate = onUpdateUi,
                        onToggleTabVisibility = onToggleTabVisibility,
                        onMoveTab = onMoveTab
                    )
                    else -> UpdateTab()
                }
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onResetDefaults,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset All Settings to Defaults", color = NovaColors.TextPrimary)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
