package com.appnova.novatube

import com.appnova.novatube.anichin.AnichinLibraryRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class AnichinLibraryTest {
    @Test
    fun normalizes_series_key_and_parses_episode_number() {
        val epUrl = "https://anichin.moe/renegade-immortal-episode-50-subtitle-indonesia/"
        val seriesUrl = "https://anichin.moe/seri/renegade-immortal/"
        assertEquals(
            "https://anichin.moe/renegade-immortal",
            AnichinLibraryRepository.normalizeSeriesKey(epUrl)
        )
        assertEquals(
            "https://anichin.moe/renegade-immortal",
            AnichinLibraryRepository.normalizeSeriesKey(seriesUrl)
        )
        assertEquals(50, AnichinLibraryRepository.parseEpisodeNumber("Episode 50"))
        assertEquals("Renegade Immortal", AnichinLibraryRepository.cleanSeriesTitle("Renegade Immortal Episode 50 Subtitle Indonesia - Anichin"))
    }
}
