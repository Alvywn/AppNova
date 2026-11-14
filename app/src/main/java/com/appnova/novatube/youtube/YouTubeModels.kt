package com.appnova.novatube.youtube

data class QualityOption(
    val label: String,
    val height: Int,
    val url: String,
    val isMuxed: Boolean = false
)

data class VideoSummary(
    val id: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val viewCount: Long,
    val isShort: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (durationSeconds <= 0) return "LIVE"
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val formattedViews: String
        get() = when {
            viewCount <= 0L -> ""
            viewCount >= 1_000_000_000L -> String.format("%.1fB views", viewCount / 1_000_000_000.0)
            viewCount >= 1_000_000L -> String.format("%.1fM views", viewCount / 1_000_000.0)
            viewCount >= 1_000L -> String.format("%.1fK views", viewCount / 1_000.0)
            else -> "$viewCount views"
        }
}

data class StreamDetails(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val description: String,
    val muxedUrl: String?,
    val videoOnlyUrl: String?,
    val audioOnlyUrl: String?,
    val qualityOptions: List<QualityOption>,
    val durationMs: Long,
    val relatedVideos: List<VideoSummary>
)
