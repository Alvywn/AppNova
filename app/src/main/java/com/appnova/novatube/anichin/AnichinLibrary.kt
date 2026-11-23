package com.appnova.novatube.anichin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.appnova.novatube.R
import com.appnova.novatube.ui.MainActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AnichinHistoryEntry(
    val url: String,
    val title: String,
    val episodeLabel: String = "",
    val watchedAtMs: Long = System.currentTimeMillis()
)

@Serializable
data class AnichinFavoriteEntry(
    val seriesUrl: String,
    val title: String,
    val lastKnownEpisode: String = "",
    val latestEpisodeUrl: String = "",
    val hasUpdate: Boolean = false,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Serializable
data class AnichinLibraryData(
    val history: List<AnichinHistoryEntry> = emptyList(),
    val favorites: List<AnichinFavoriteEntry> = emptyList()
)

class AnichinLibraryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("novatube_anichin_library", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AnichinLibraryData> = _data.asStateFlow()

    private fun load(): AnichinLibraryData {
        val raw = prefs.getString("library_json", null) ?: return AnichinLibraryData()
        return runCatching { json.decodeFromString<AnichinLibraryData>(raw) }
            .getOrDefault(AnichinLibraryData())
    }

    private fun save(next: AnichinLibraryData) {
        _data.value = next
        prefs.edit().putString("library_json", json.encodeToString(next)).apply()
    }

    fun recordHistory(url: String, title: String, episodeLabel: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        val current = _data.value
        val updated = current.history
            .filterNot { it.url == cleanUrl }
            .toMutableList()
            .apply {
                add(
                    0,
                    AnichinHistoryEntry(
                        url = cleanUrl,
                        title = title.ifBlank { "Anichin" },
                        episodeLabel = episodeLabel.trim(),
                        watchedAtMs = System.currentTimeMillis()
                    )
                )
            }
            .take(150)
        save(current.copy(history = updated))
    }

    fun removeHistory(url: String) {
        val current = _data.value
        save(current.copy(history = current.history.filterNot { it.url == url }))
    }

    fun clearHistory() {
        save(_data.value.copy(history = emptyList()))
    }

    fun isFavorite(urlOrSeries: String): Boolean {
        val norm = normalizeSeriesKey(urlOrSeries)
        return _data.value.favorites.any { normalizeSeriesKey(it.seriesUrl) == norm || it.seriesUrl == urlOrSeries }
    }

    fun toggleFavorite(seriesUrl: String, title: String, currentEpisode: String, currentPageUrl: String): Boolean {
        val norm = normalizeSeriesKey(seriesUrl)
        val current = _data.value
        val existing = current.favorites.firstOrNull {
            normalizeSeriesKey(it.seriesUrl) == norm || it.seriesUrl == seriesUrl
        }
        return if (existing != null) {
            save(current.copy(favorites = current.favorites.filterNot { it == existing }))
            false
        } else {
            val cleanTitle = cleanSeriesTitle(title)
            val updated = listOf(
                AnichinFavoriteEntry(
                    seriesUrl = seriesUrl,
                    title = cleanTitle,
                    lastKnownEpisode = currentEpisode,
                    latestEpisodeUrl = currentPageUrl.ifBlank { seriesUrl },
                    hasUpdate = false
                )
            ) + current.favorites
            save(current.copy(favorites = updated))
            true
        }
    }

    fun removeFavorite(seriesUrl: String) {
        val current = _data.value
        save(current.copy(favorites = current.favorites.filterNot { it.seriesUrl == seriesUrl }))
    }

    fun markUpdateSeen(seriesUrl: String) {
        val current = _data.value
        val updated = current.favorites.map {
            if (it.seriesUrl == seriesUrl) it.copy(hasUpdate = false) else it
        }
        save(current.copy(favorites = updated))
    }

    fun applyDetectedUpdates(updatedList: List<AnichinFavoriteEntry>) {
        save(_data.value.copy(favorites = updatedList))
    }

    companion object {
        fun normalizeSeriesKey(url: String): String =
            url.lowercase()
                .removeSuffix("/")
                .replace(Regex("-episode-\\d+.*$"), "")
                .replace("/seri/", "/")

        fun cleanSeriesTitle(raw: String): String =
            raw.replace(Regex("\\s*Episode\\s*\\d+.*$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*Subtitle Indonesia.*$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*-\\s*Anichin.*$", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { "Donghua" }

        fun parseEpisodeNumber(label: String): Int {
            val match = Regex("(\\d+)").find(label) ?: return -1
            return match.groupValues[1].toIntOrNull() ?: -1
        }
    }
}

object AnichinUpdateChecker {
    private const val CHANNEL_ID = "anichin_updates"

    suspend fun checkUpdates(
        favorites: List<AnichinFavoriteEntry>
    ): Pair<List<AnichinFavoriteEntry>, List<AnichinFavoriteEntry>> = withContext(Dispatchers.IO) {
        if (favorites.isEmpty()) return@withContext favorites to emptyList()

        val newlyUpdated = mutableListOf<AnichinFavoriteEntry>()
        val updatedFavorites = favorites.map { fav ->
            val latest = fetchLatestEpisodeForFavorite(fav)
            if (latest != null) {
                val (epLabel, epUrl) = latest
                val oldNum = AnichinLibraryRepository.parseEpisodeNumber(fav.lastKnownEpisode)
                val newNum = AnichinLibraryRepository.parseEpisodeNumber(epLabel)
                val isNewer = (newNum > oldNum && newNum > 0) ||
                    (epLabel.isNotBlank() && epLabel != fav.lastKnownEpisode && fav.lastKnownEpisode.isNotBlank() && epUrl != fav.latestEpisodeUrl)
                if (isNewer) {
                    val item = fav.copy(
                        lastKnownEpisode = epLabel,
                        latestEpisodeUrl = epUrl,
                        hasUpdate = true
                    )
                    newlyUpdated.add(item)
                    item
                } else {
                    fav
                }
            } else {
                fav
            }
        }
        updatedFavorites to newlyUpdated
    }

    private fun fetchLatestEpisodeForFavorite(fav: AnichinFavoriteEntry): Pair<String, String>? {
        val targetUrl = fav.seriesUrl.takeIf { it.startsWith("http") } ?: return null
        return runCatching {
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.88 Mobile Safari/537.36"
                )
            }
            if (conn.responseCode != 200) return@runCatching null
            val html = conn.inputStream.bufferedReader().use { it.readText() }

            // Match Anichin episode links: <a href="https://anichin.moe/...-episode-51-...">...Episode 51...</a>
            val linkRegex = Regex(
                """href=["'](https?://[^"']*anichin\.moe/[^"']*episode-(\d+)[^"']*)["']""",
                RegexOption.IGNORE_CASE
            )
            val best = linkRegex.findAll(html)
                .mapNotNull { m ->
                    val url = m.groupValues[1]
                    val num = m.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                    num to url
                }
                .maxByOrNull { it.first }

            best?.let { (num, url) -> "Episode $num" to url }
        }.getOrNull()
    }

    fun showUpdateNotification(context: Context, updated: List<AnichinFavoriteEntry>) {
        if (updated.isEmpty()) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anichin Donghua Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val first = updated.first()
        val title = if (updated.size == 1) {
            "Update Donghua: ${first.title}"
        } else {
            "${updated.size} Donghua Favorit Ada Update!"
        }
        val text = if (updated.size == 1) {
            "${first.lastKnownEpisode} sudah rilis di Anichin."
        } else {
            updated.joinToString(", ") { "${it.title} (${it.lastKnownEpisode})" }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            2025,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.novatube_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        runCatching { nm.notify(9001, notif) }
    }
}
