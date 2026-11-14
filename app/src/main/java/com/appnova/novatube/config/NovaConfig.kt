package com.appnova.novatube.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class SkipMode(val label: String) {
    AUTO_SKIP("Auto Skip"),
    MANUAL_BUTTON("Manual Skip Pill"),
    SHOW_ONLY("Markers Only")
}

@Serializable
enum class SponsorCategory(
    val apiName: String,
    val displayName: String,
    val defaultColorHex: Long
) {
    SPONSOR("sponsor", "Sponsor", 0xFF00D400),
    SELF_PROMO("selfpromo", "Unpaid / Self Promotion", 0xFFFFFF00),
    INTERACTION("interaction", "Interaction Reminder", 0xFFCC00FF),
    INTRO("intro", "Intermission / Intro", 0xFF00FFFF),
    OUTRO("outro", "Endcards / Credits", 0xFF0202ED),
    PREVIEW("preview", "Preview / Recap", 0xFF008FD6),
    MUSIC_OFFTOPIC("music_offtopic", "Non-Music Section", 0xFFFF9900),
    FILLER("filler", "Filler Tangent", 0xFF7300FF);

    companion object {
        fun fromApiName(name: String): SponsorCategory? =
            entries.firstOrNull { it.apiName == name }
    }
}

@Serializable
data class PlaybackConfig(
    val backgroundPlayEnabled: Boolean = true,
    val pipUnlockEnabled: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val customSpeedOptionsEnabled: Boolean = true,
    val loopByDefault: Boolean = false,
    val gesturesEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val swipeSeekEnabled: Boolean = true,
    val seekStepSeconds: Int = 10
)

@Serializable
data class SponsorBlockConfig(
    val enabled: Boolean = true,
    val showSkipToast: Boolean = true,
    val showSeekbarMarkers: Boolean = true,
    val skipMode: SkipMode = SkipMode.AUTO_SKIP,
    val enabledCategories: Set<SponsorCategory> = setOf(
        SponsorCategory.SPONSOR,
        SponsorCategory.SELF_PROMO,
        SponsorCategory.INTERACTION,
        SponsorCategory.INTRO,
        SponsorCategory.OUTRO
    )
)

@Serializable
data class YouTubeTabConfig(
    val id: String,
    val title: String,
    val visible: Boolean = true
) {
    companion object {
        fun defaultTabs(): List<YouTubeTabConfig> = listOf(
            YouTubeTabConfig("home", "Home", visible = true),
            YouTubeTabConfig("search", "Search", visible = true),
            YouTubeTabConfig("bookmarks", "Saved", visible = true),
            YouTubeTabConfig("settings", "Settings", visible = true)
        )
    }
}

@Serializable
data class UiConfig(
    val adBlockEnabled: Boolean = true,
    val oledModeEnabled: Boolean = true,
    val liquidGlassHudEnabled: Boolean = true,
    val hideShortsShelf: Boolean = false,
    val hideCreateButton: Boolean = true,
    val hideCastButton: Boolean = false,
    val hideWatermark: Boolean = true,
    val tabs: List<YouTubeTabConfig> = YouTubeTabConfig.defaultTabs()
)

@Serializable
data class NovaConfig(
    val moduleEnabled: Boolean = true,
    val playback: PlaybackConfig = PlaybackConfig(),
    val sponsorBlock: SponsorBlockConfig = SponsorBlockConfig(),
    val ui: UiConfig = UiConfig()
) {
    fun toJson(): String = jsonFormat.encodeToString(this)

    companion object {
        private val jsonFormat = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromJson(raw: String?): NovaConfig {
            if (raw.isNullOrBlank()) return NovaConfig()
            return runCatching { jsonFormat.decodeFromString<NovaConfig>(raw) }
                .getOrDefault(NovaConfig())
        }
    }
}
