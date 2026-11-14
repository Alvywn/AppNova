package com.appnova.novatube.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appnova.novatube.ui.components.GlassCard
import com.appnova.novatube.ui.components.VideoCard
import com.appnova.novatube.ui.theme.NovaColors
import com.appnova.novatube.youtube.VideoSummary

private val feedCategories = listOf("All", "Music", "Gaming", "News", "Podcasts", "Tech")

@Composable
fun HomeFeedTab(
    videos: List<VideoSummary>,
    bookmarks: List<VideoSummary>,
    selectedCategory: String,
    hideShorts: Boolean,
    isLoading: Boolean,
    error: String?,
    onSelectCategory: (String) -> Unit,
    onRefresh: () -> Unit,
    onVideoClick: (VideoSummary) -> Unit,
    onToggleBookmark: (VideoSummary) -> Unit
) {
    val visibleVideos = if (hideShorts) videos.filterNot { it.isShort } else videos

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(feedCategories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onSelectCategory(category) },
                        label = { Text(category, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            containerColor = NovaColors.GlassSurface,
                            labelColor = NovaColors.TextPrimary
                        )
                    )
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = NovaColors.TextSecondary)
            }
        }

        when {
            isLoading && visibleVideos.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NovaColors.AccentRed)
                }
            }
            error != null && visibleVideos.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard {
                        Text(
                            text = "Could not load YouTube feed",
                            color = NovaColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = error, color = NovaColors.TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = NovaColors.AccentRed)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleVideos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            isBookmarked = bookmarks.any { it.id == video.id },
                            onClick = { onVideoClick(video) },
                            onToggleBookmark = { onToggleBookmark(video) }
                        )
                    }
                }
            }
        }
    }
}
