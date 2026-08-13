package com.anitrack.app.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnimeModel(
    val id: Int,
    @Json(name = "title")
    val title: TitleModel? = null,
    @Json(name = "coverImage")
    val coverImage: CoverImageModel? = null,
    @Json(name = "bannerImage")
    val bannerImage: String? = null,
    @Json(name = "episodes")
    val episodes: Int? = null,
    @Json(name = "averageScore")
    val averageScore: Double? = null,
    @Json(name = "meanScore")
    val meanScore: Double? = null,
    @Json(name = "genres")
    val genres: List<String>? = null,
    @Json(name = "tags")
    val tags: List<TagModel>? = null,
    @Json(name = "status")
    val status: String? = null,
    @Json(name = "description")
    val description: String? = null,
    @Json(name = "seasonYear")
    val seasonYear: Int? = null,
    @Json(name = "season")
    val season: String? = null,
    @Json(name = "format")
    val format: String? = null,
    @Json(name = "duration")
    val duration: Int? = null,
    @Json(name = "source")
    val source: String? = null,
    @Json(name = "studios")
    val studios: StudioConnection? = null,
    @Json(name = "startDate")
    val startDate: FuzzyDateModel? = null,
    @Json(name = "endDate")
    val endDate: FuzzyDateModel? = null,
    @Json(name = "nextAiringEpisode")
    val nextAiringEpisode: AiringScheduleModel? = null,
    @Json(name = "siteUrl")
    val siteUrl: String? = null,
    @Json(name = "isAdult")
    val isAdult: Boolean = false,
    // Local fields for favorites
    var isFavorite: Boolean = false,
    var addedAt: Long = 0L
) {
    fun getDisplayTitle(): String {
        return title?.english ?: title?.romaji ?: title?.native ?: "Unknown"
    }
    
    fun getCoverUrl(): String {
        return coverImage?.extraLarge ?: coverImage?.large ?: ""
    }
}

@JsonClass(generateAdapter = true)
data class TitleModel(
    @Json(name = "romaji") val romaji: String? = null,
    @Json(name = "english") val english: String? = null,
    @Json(name = "native") val native: String? = null,
    @Json(name = "userPreferred") val userPreferred: String? = null
)

@JsonClass(generateAdapter = true)
data class CoverImageModel(
    @Json(name = "extraLarge") val extraLarge: String? = null,
    @Json(name = "large") val large: String? = null,
    @Json(name = "medium") val medium: String? = null,
    @Json(name = "color") val color: String? = null
)

@JsonClass(generateAdapter = true)
data class TagModel(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val isGeneralSpoiler: Boolean = false,
    val isMediaSpoiler: Boolean = false,
    val isAdult: Boolean = false
)

@JsonClass(generateAdapter = true)
data class StudioConnection(
    val nodes: List<StudioNode>? = null
)

@JsonClass(generateAdapter = true)
data class StudioNode(
    val id: Int,
    val name: String? = null,
    val isAnimationStudio: Boolean = false
)

@JsonClass(generateAdapter = true)
data class FuzzyDateModel(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

@JsonClass(generateAdapter = true)
data class AiringScheduleModel(
    val id: Int,
    val airingAt: Long? = null,
    val timeUntilAiring: Long? = null,
    val episode: Int? = null
)
