package com.appnova.novatube.sponsorblock

import com.appnova.novatube.config.SponsorCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SponsorApiSegmentDto(
    val segment: List<Double>,
    val category: String,
    val actionType: String = "skip",
    val UUID: String = ""
)

data class SponsorSegment(
    val uuid: String,
    val category: SponsorCategory,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

    fun contains(positionMs: Long): Boolean = positionMs in startMs until endMs

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parseApiResponse(
            rawJson: String,
            enabledCategories: Set<SponsorCategory>
        ): List<SponsorSegment> {
            if (rawJson.isBlank() || rawJson.trim() == "Not Found") return emptyList()
            val dtos = runCatching {
                json.decodeFromString<List<SponsorApiSegmentDto>>(rawJson)
            }.getOrElse { return emptyList() }

            return dtos.mapNotNull { dto ->
                if (dto.segment.size < 2) return@mapNotNull null
                val category = SponsorCategory.fromApiName(dto.category) ?: return@mapNotNull null
                if (category !in enabledCategories) return@mapNotNull null
                val startMs = (dto.segment[0] * 1000.0).toLong().coerceAtLeast(0L)
                val endMs = (dto.segment[1] * 1000.0).toLong().coerceAtLeast(startMs)
                if (endMs <= startMs) return@mapNotNull null
                SponsorSegment(
                    uuid = dto.UUID,
                    category = category,
                    startMs = startMs,
                    endMs = endMs
                )
            }.sortedBy { it.startMs }
        }
    }
}
