package com.appnova.novatube.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appnova.novatube.anichin.AnichinLibraryData
import com.appnova.novatube.ui.theme.LocalNovaThemeStyle
import com.appnova.novatube.ui.theme.NovaColors

@Composable
fun AnichinLibrarySheet(
    library: AnichinLibraryData,
    isCheckingUpdates: Boolean,
    onCheckUpdates: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onMarkFavoriteSeen: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val theme = LocalNovaThemeStyle.current
    val accent = Color(theme.accentHex)
    val cardColor = Color(theme.cardHex)
    val borderColor = Color(theme.borderHex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.82f)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Anichin Library",
                color = NovaColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = NovaColors.TextSecondary)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                leadingIcon = {
                    Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("Favorit (${library.favorites.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0x33FBBF24),
                    selectedLabelColor = Color(0xFFFBBF24),
                    selectedLeadingIconColor = Color(0xFFFBBF24)
                )
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                leadingIcon = {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("History (${library.history.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.22f),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = accent
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (selectedTab == 0) {
                IconButton(onClick = onCheckUpdates, enabled = !isCheckingUpdates) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Cek Update Donghua",
                        tint = if (isCheckingUpdates) NovaColors.TextSecondary else accent
                    )
                }
            } else if (library.history.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("Hapus Semua", color = NovaColors.AccentRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            if (library.favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada donghua favorit. Buka donghua di Anichin lalu tap ★ Favorit di Nova Menu.",
                        color = NovaColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(library.favorites, key = { it.seriesUrl }) { fav ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onMarkFavoriteSeen(fav.seriesUrl)
                                    onOpenUrl(fav.latestEpisodeUrl.ifBlank { fav.seriesUrl })
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = cardColor,
                            border = BorderStroke(
                                width = if (fav.hasUpdate) 1.5.dp else 1.dp,
                                color = if (fav.hasUpdate) Color(0xFFFBBF24) else borderColor
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = fav.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (fav.hasUpdate) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFBBF24))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "UPDATE",
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                    if (fav.lastKnownEpisode.isNotBlank()) {
                                        Text(
                                            text = "Episode terakhir: ${fav.lastKnownEpisode}",
                                            color = if (fav.hasUpdate) Color(0xFFFBBF24) else NovaColors.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                IconButton(onClick = { onRemoveFavorite(fav.seriesUrl) }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Hapus Favorit",
                                        tint = NovaColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (library.history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada riwayat tontonan Anichin.",
                        color = NovaColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(library.history, key = { it.url }) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenUrl(item.url) },
                            shape = RoundedCornerShape(14.dp),
                            color = cardColor,
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.episodeLabel.isNotBlank()) {
                                        Text(
                                            text = item.episodeLabel,
                                            color = accent,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                IconButton(onClick = { onRemoveHistory(item.url) }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Hapus History",
                                        tint = NovaColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
