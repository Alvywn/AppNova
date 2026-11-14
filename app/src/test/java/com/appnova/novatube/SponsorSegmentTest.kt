package com.appnova.novatube

import com.appnova.novatube.config.SponsorCategory
import com.appnova.novatube.sponsorblock.SponsorBlockClient
import com.appnova.novatube.sponsorblock.SponsorSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SponsorSegmentTest {

    @Test
    fun `parseApiResponse filters by enabled categories and sorts by startMs`() {
        val raw = """
            [
              {"segment":[30.5,45.0],"category":"outro","actionType":"skip","UUID":"u2"},
              {"segment":[10.0,20.5],"category":"sponsor","actionType":"skip","UUID":"u1"},
              {"segment":[5.0,8.0],"category":"filler","actionType":"skip","UUID":"u3"}
            ]
        """.trimIndent()

        val segments = SponsorSegment.parseApiResponse(
            rawJson = raw,
            enabledCategories = setOf(SponsorCategory.SPONSOR, SponsorCategory.OUTRO)
        )

        assertEquals(2, segments.size)
        assertEquals("u1", segments[0].uuid)
        assertEquals(10_000L, segments[0].startMs)
        assertEquals(20_500L, segments[0].endMs)
        assertEquals("u2", segments[1].uuid)
    }

    @Test
    fun `findActiveSegment matches position inside segment bounds`() {
        val client = SponsorBlockClient()
        val segments = listOf(
            SponsorSegment("u1", SponsorCategory.SPONSOR, 10_000L, 20_000L)
        )

        assertNull(client.findActiveSegment(segments, 9_999L))
        assertNotNull(client.findActiveSegment(segments, 10_000L))
        assertNotNull(client.findActiveSegment(segments, 15_000L))
        assertNull(client.findActiveSegment(segments, 20_000L))
    }
}
