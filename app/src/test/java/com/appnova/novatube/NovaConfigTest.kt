package com.appnova.novatube

import com.appnova.novatube.config.NovaConfig
import com.appnova.novatube.config.PlaybackConfig
import com.appnova.novatube.config.SkipMode
import com.appnova.novatube.config.SponsorBlockConfig
import com.appnova.novatube.config.SponsorCategory
import com.appnova.novatube.config.YouTubeTabConfig
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
}
