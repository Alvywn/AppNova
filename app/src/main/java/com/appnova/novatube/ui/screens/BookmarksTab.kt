package com.appnova.novatube.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appnova.novatube.ui.components.VideoCard
import com.appnova.novatube.ui.theme.NovaColors
import com.appnova.novatube.youtube.VideoSummary

@Composable
fun BookmarksTab(
    bookmarks: List<VideoSummary>,
    onVideoClick: (VideoSummary) -> Unit,
    onToggleBookmark: (VideoSummary) -> Unit
) {
    if (bookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No saved videos yet", color = NovaColors.TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(bookmarks, key = { it.id }) { video ->
            VideoCard(
                video = video,
                isBookmarked = true,
                onClick = { onVideoClick(video) },
                onToggleBookmark = { onToggleBookmark(video) }
            )
        }
    }
}
