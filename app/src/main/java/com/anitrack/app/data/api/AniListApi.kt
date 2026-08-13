package com.anitrack.app.data.api

import com.anitrack.app.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface AniListApi {
    
    @POST("/")
    suspend fun getTrendingAnime(
        @Body request: GraphQLRequest
    ): Response<GraphQLResponse<PageResponse<AnimeModel>>>
    
    @POST("/")
    suspend fun searchAnime(
        @Body request: GraphQLRequest
    ): Response<GraphQLResponse<PageResponse<AnimeModel>>>
    
    @POST("/")
    suspend fun getAnimeById(
        @Body request: GraphQLRequest
    ): Response<GraphQLResponse<MediaData>>
    
    @POST("/")
    suspend fun getPopularThisSeason(
        @Body request: GraphQLRequest
    ): Response<GraphQLResponse<PageResponse<AnimeModel>>>

    companion object {
        const val BASE_URL = "https://graphql.anilist.info/"
        
        // GraphQL Queries
        const val TRENDING_QUERY = """
            query {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, sort: TRENDING_DESC) {
                        id
                        title { romaji english native }
                        coverImage { extraLarge large medium }
                        bannerImage
                        episodes
                        averageScore
                        meanScore
                        genres
                        status
                        description(asHtml: false)
                        seasonYear
                        season
                        format
                        duration
                        studios(isMain: true) { nodes { name isAnimationStudio } }
                        siteUrl
                    }
                }
            }
        """
        
        const val SEARCH_QUERY = """
            query(${'$'}search: String) {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, search: ${'$'}search) {
                        id
                        title { romaji english native }
                        coverImage { extraLarge large medium }
                        episodes
                        averageScore
                        genres
                        status
                        seasonYear
                        format
                    }
                }
            }
        """
        
        const val DETAIL_QUERY = """
            query(${'$'}id: Int) {
                Media(id: ${'$'}id, type: ANIME) {
                    id
                    title { romaji english native userPreferred }
                    coverImage { extraLarge large medium color }
                    bannerImage
                    episodes
                    averageScore
                    meanScore
                    genres
                    tags { name category isMediaSpoiler isAdult }
                    status
                    description(asHtml: false)
                    seasonYear
                    season
                    format
                    duration
                    source
                    studios(isMain: true) { nodes { name isAnimationStudio } }
                    startDate { year month day }
                    endDate { year month day }
                    nextAiringEpisode { airingAt timeUntilAiring episode }
                    siteUrl
                    isAdult
                }
            }
        """
        
        const val SEASON_POPULAR_QUERY = """
            query {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, sort: POPULARITY_DESC, season: WINTER, seasonYear: 2024) {
                        id
                        title { romaji english native }
                        coverImage { extraLarge large medium }
                        episodes
                        averageScore
                        genres
                        status
                        seasonYear
                        format
                    }
                }
            }
        """
    }
}

// Response wrapper for single anime detail
@JsonClass(generateAdapter = true)
data class MediaData(
    @Json(name = "Media") val media: AnimeModel? = null
)
