package com.appnova.novatube.youtube

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream

class YouTubeRepository {

    suspend fun fetchTrending(): Result<List<VideoSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val kiosk = ServiceList.YouTube.kioskList.defaultKioskExtractor
            kiosk.fetchPage()
            kiosk.initialPage.items.filterIsInstance<StreamInfoItem>().mapNotNull { it.toSummary() }
        }
    }

    suspend fun searchVideos(query: String): Result<List<VideoSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            if (query.isBlank()) return@runCatching emptyList()
            val extractor = ServiceList.YouTube.getSearchExtractor(query)
            extractor.fetchPage()
            extractor.initialPage.items.filterIsInstance<StreamInfoItem>().mapNotNull { it.toSummary() }
        }
    }

    suspend fun loadStreamDetails(videoId: String): Result<StreamDetails> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)

            val bestMuxed = info.videoStreams
                .filter { it.isUrl }
                .sortedWith(
                    compareByDescending<VideoStream> { it.format == MediaFormat.MPEG_4 }
                        .thenByDescending { it.height }
                )
                .firstOrNull()
                ?.content

            // Group by height and strongly prefer MPEG_4 (avc1 / H.264) over VP9/AV1
            // so every resolution has a seekable fMP4 sidx index compatible with ExoPlayer.
            val videoOnlyCandidates = info.videoOnlyStreams
                .filter { it.isUrl && it.height in 144..1080 }
                .groupBy { it.height }
                .mapValues { (_, streams) ->
                    streams.sortedWith(
                        compareByDescending<VideoStream> { it.format == MediaFormat.MPEG_4 }
                            .thenByDescending { it.codec?.startsWith("avc1") == true }
                            .thenByDescending { it.codec?.startsWith("vp9") == true }
                    ).first()
                }
                .values
                .sortedByDescending { it.height }

            val qualityOptions = buildList {
                if (bestMuxed != null) {
                    add(
                        QualityOption(
                            label = "360p (Fast)",
                            height = 360,
                            url = bestMuxed,
                            isMuxed = true
                        )
                    )
                }
                videoOnlyCandidates.forEach {
                    add(
                        QualityOption(
                            label = "${it.height}p",
                            height = it.height,
                            url = it.content,
                            isMuxed = false
                        )
                    )
                }
            }

            val bestVideoOnly = videoOnlyCandidates
                .firstOrNull { it.height <= 720 }?.content
                ?: videoOnlyCandidates.firstOrNull()?.content

            // Prefer M4A (mp4a) audio over WebM Opus so both video and audio use
            // seekable fMP4 containers with sidx segment indexes when switching quality.
            val bestAudioOnly = info.audioStreams
                .filter { it.isUrl }
                .sortedWith(
                    compareByDescending<AudioStream> { it.format == MediaFormat.M4A }
                        .thenByDescending { it.averageBitrate }
                )
                .firstOrNull()
                ?.content

            val related = info.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .mapNotNull { it.toSummary() }

            StreamDetails(
                videoId = videoId,
                title = info.name.orEmpty(),
                channelName = info.uploaderName.orEmpty(),
                thumbnailUrl = info.thumbnails.lastOrNull()?.url.orEmpty(),
                description = info.description?.content.orEmpty(),
                muxedUrl = bestMuxed,
                videoOnlyUrl = bestVideoOnly,
                audioOnlyUrl = bestAudioOnly,
                qualityOptions = qualityOptions,
                durationMs = (info.duration * 1000L).coerceAtLeast(0L),
                relatedVideos = related
            )
        }
    }

    private fun StreamInfoItem.toSummary(): VideoSummary? {
        val id = extractVideoId(url) ?: return null
        return VideoSummary(
            id = id,
            title = name.orEmpty(),
            channelName = uploaderName.orEmpty(),
            thumbnailUrl = thumbnails.lastOrNull()?.url.orEmpty(),
            durationSeconds = duration,
            viewCount = viewCount,
            isShort = isShortFormContent
        )
    }

    companion object {
        fun extractVideoId(url: String?): String? {
            if (url.isNullOrBlank()) return null
            val uri = Uri.parse(url)
            uri.getQueryParameter("v")?.let { if (it.length == 11) return it }
            val segments = uri.pathSegments
            val shortsIdx = segments.indexOf("shorts")
            if (shortsIdx >= 0 && shortsIdx + 1 < segments.size) {
                return segments[shortsIdx + 1]
            }
            return segments.lastOrNull()?.takeIf { it.length == 11 }
        }
    }
}
