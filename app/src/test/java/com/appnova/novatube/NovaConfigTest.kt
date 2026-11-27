package com.appnova.novatube

import com.appnova.novatube.config.NovaConfig
import com.appnova.novatube.config.NovaThemeStyle
import com.appnova.novatube.config.PlaybackConfig
import com.appnova.novatube.config.SkipMode
import com.appnova.novatube.config.SponsorBlockConfig
import com.appnova.novatube.config.SponsorCategory
import com.appnova.novatube.config.UiConfig
import com.appnova.novatube.config.YouTubeTabConfig
import com.appnova.novatube.ui.screens.buildNovaYouTubeCss
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaConfigTest {

    @Test
    fun `serialize and deserialize roundtrip preserves all settings`() {
        val original = NovaConfig(
            moduleEnabled = true,
            playback = PlaybackConfig(
                backgroundPlayEnabled = true,
                pipUnlockEnabled = false,
                defaultSpeed = 1.75f,
                seekStepSeconds = 15
            ),
            sponsorBlock = SponsorBlockConfig(
                enabled = true,
                skipMode = SkipMode.MANUAL_BUTTON,
                enabledCategories = setOf(SponsorCategory.SPONSOR, SponsorCategory.OUTRO)
            )
        )

        val json = original.toJson()
        val restored = NovaConfig.fromJson(json)

        assertEquals(original, restored)
    }

    @Test
    fun `invalid json falls back safely to defaults`() {
        val restored = NovaConfig.fromJson("{not-valid-json")
        assertTrue(restored.moduleEnabled)
        assertEquals(1.0f, restored.playback.defaultSpeed)
        assertFalse(restored.playback.loopByDefault)
        assertEquals(4, restored.ui.tabs.size)
        assertEquals(YouTubeTabConfig.defaultTabs(), restored.ui.tabs)
    }

    @Test
    fun `buildNovaYouTubeCss applies oled and ui cleanup flags`() {
        val cfg = NovaConfig(
            ui = UiConfig(
                themeStyle = NovaThemeStyle.LIQUID_GLASS,
                oledModeEnabled = true,
                hideShortsShelf = true,
                hideCreateButton = true,
                hideCastButton = true,
                hideWatermark = true,
                tabs = listOf(
                    YouTubeTabConfig("home", "Home", visible = false),
                    YouTubeTabConfig("search", "Search", visible = true),
                    YouTubeTabConfig("bookmarks", "Saved", visible = false),
                    YouTubeTabConfig("settings", "Settings", visible = true)
                )
            )
        )

        val css = buildNovaYouTubeCss(cfg)
        assertTrue(css.contains("#000000"))
        assertTrue(css.contains(".pivot-shorts"))
        assertTrue(css.contains(".pivot-uploads"))
        assertTrue(css.contains(".ytp-cast-button"))
        assertTrue(css.contains(".ytp-watermark"))
        assertTrue(css.contains(".pivot-w2w"))
        assertTrue(css.contains(".pivot-library"))
    }
}
