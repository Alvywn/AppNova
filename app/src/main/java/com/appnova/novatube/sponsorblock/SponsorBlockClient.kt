package com.appnova.novatube.sponsorblock

import com.appnova.novatube.config.SponsorCategory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class SponsorBlockClient {

    private val cache = ConcurrentHashMap<String, List<SponsorSegment>>()
    private val skippedInSession = ConcurrentHashMap.newKeySet<String>()
    private val executor = Executors.newSingleThreadExecutor()

    fun getCachedSegments(videoId: String): List<SponsorSegment>? = cache[videoId]

    fun fetchSegmentsAsync(
        videoId: String,
        enabledCategories: Set<SponsorCategory>,
        onResult: (List<SponsorSegment>) -> Unit
    ) {
        if (videoId.isBlank() || enabledCategories.isEmpty()) {
            onResult(emptyList())
            return
        }
        cache[videoId]?.let { cached ->
            onResult(cached.filter { it.category in enabledCategories })
            return
        }

        executor.execute {
            val segments = fetchSegmentsSync(videoId, enabledCategories)
            cache[videoId] = segments
            onResult(segments)
        }
    }

    fun fetchSegmentsSync(
        videoId: String,
        enabledCategories: Set<SponsorCategory>
    ): List<SponsorSegment> {
        return runCatching {
            val categoriesJson = enabledCategories.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ","
            ) { "\"${it.apiName}\"" }
            val encodedCategories = URLEncoder.encode(categoriesJson, "UTF-8")
            val encodedVideoId = URLEncoder.encode(videoId, "UTF-8")
            val url = URL("https://sponsor.ajay.app/api/skipSegments?videoID=$encodedVideoId&categories=$encodedCategories")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "NovaTube-Android/1.0")
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                SponsorSegment.parseApiResponse(body, enabledCategories)
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())
    }

    fun findActiveSegment(segments: List<SponsorSegment>, positionMs: Long): SponsorSegment? =
        segments.firstOrNull { it.contains(positionMs) }

    fun markSkipped(videoId: String, segment: SponsorSegment): Boolean =
        skippedInSession.add("${videoId}:${segment.uuid}")

    fun clearSessionForVideo(videoId: String) {
        skippedInSession.removeIf { it.startsWith("$videoId:") }
    }
}
