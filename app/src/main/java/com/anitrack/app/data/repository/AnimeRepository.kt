package com.anitrack.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.anitrack.app.data.api.AniListApi
import com.anitrack.app.data.api.MediaData
import com.anitrack.app.data.api.models.GraphQLRequest
import com.anitrack.app.data.api.models.AnimeModel
import com.anitrack.app.data.api.models.CoverImageModel
import com.anitrack.app.data.api.models.TitleModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

val Context.remoteControlDataStore: DataStore<Preferences> by preferencesDataStore(name = "remote_control")

class AnimeRepository private constructor(
    private val api: AniListApi,
    private val context: Context
) {
    
    private val favoritesDataStore = context.favoritesDataStore
    
    companion object {
        @Volatile
        private var instance: AnimeRepository? = null
        
        fun getInstance(context: Context): AnimeRepository {
            return instance ?: synchronized(this) {
                instance ?: AnimeRepository(
                    api = createAniListApi(),
                    context = context.applicationContext
                ).also { instance = it }
            }
        }
    }

    suspend fun getTrendingAnime(): List<AnimeModel> {
        try {
            val request = GraphQLRequest(
                query = AniListApi.TRENDING_QUERY,
                variables = emptyMap()
            )
            
            val response = api.getTrendingAnime(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                body?.data?.page?.media?.let { mediaList ->
                    return mediaList.map { it.copy(coverImage = it.coverImage?.copy(extraLarge = it.getCoverUrl())) }
                }
            }
            
            return getFallbackTrendingAnime()
        } catch (e: Exception) {
            return getFallbackTrendingAnime()
        }
    }

    suspend fun searchAnime(query: String): List<AnimeModel> {
        try {
            val request = GraphQLRequest(
                query = AniListApi.SEARCH_QUERY,
                variables = mapOf("search" to query)
            )
            
            val response = api.searchAnime(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                body?.data?.page?.media?.let { mediaList ->
                    return mediaList.map { it.copy(coverImage = it.coverImage?.copy(extraLarge = it.getCoverUrl())) }
                }
            }
            
            return emptyList()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getAnimeById(animeId: Int): AnimeModel {
        try {
            val request = GraphQLRequest(
                query = AniListApi.DETAIL_QUERY,
                variables = mapOf("id" to animeId)
            )
            
            val response = api.getAnimeById(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                body?.data?.media?.let { anime ->
                    return anime.copy(coverImage = anime.coverImage?.copy(extraLarge = anime.getCoverUrl()))
                }
            }
            
            throw Exception("Failed to load anime details")
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getPopularThisSeason(): List<AnimeModel> {
        try {
            val request = GraphQLRequest(
                query = AniListApi.SEASON_POPULAR_QUERY,
                variables = emptyMap()
            )
            
            val response = api.getPopularThisSeason(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                body?.data?.page?.media?.let { mediaList ->
                    return mediaList.map { it.copy(coverImage = it.coverImage?.copy(extraLarge = it.getCoverUrl())) }
                }
            }
            
            return getFallbackSeasonAnime()
        } catch (e: Exception) {
            return getFallbackSeasonAnime()
        }
    }

    suspend fun addToFavorites(anime: AnimeModel) {
        favoritesDataStore.edit { preferences ->
            preferences[booleanPreferencesKey("fav_${anime.id}")] = true
        }
    }

    suspend fun removeFromFavorites(animeId: Int) {
        favoritesDataStore.edit { preferences ->
            preferences.remove(booleanPreferencesKey("fav_$animeId"))
        }
    }

    fun isFavorite(animeId: Int): Boolean {
        return false
    }

    suspend fun isFavoriteAsync(animeId: Int): Boolean {
        val preferences = favoritesDataStore.data.first()
        return preferences[booleanPreferencesKey("fav_$animeId")] == true
    }

    suspend fun getFavorites(): List<AnimeModel> {
        val preferences = favoritesDataStore.data.first()
        val favoriteIds = preferences.asMap()
            .filter { (key, value) -> key.name.startsWith("fav_") && value == true }
            .mapKeys { (key, _) -> key.name.removePrefix("fav_").toIntOrNull() }
            .filterKeys { it != null }
            .keys
            .toList()
        
        return favoriteIds.filterNotNull().map { id ->
            AnimeModel(
                id = id,
                title = null,
                coverImage = null,
                isFavorite = true
            )
        }
    }

    private fun getFallbackTrendingAnime(): List<AnimeModel> {
        return listOf(
            createFallbackAnime(1, "Attack on Titan", "Shingeki no Kyojin", 87, 25, "FINISHED", listOf("Action", "Drama", "Fantasy")),
            createFallbackAnime(2, "Demon Slayer", "Kimetsu no Yaiba", 86, 26, "FINISHED", listOf("Action", "Supernatural")),
            createFallbackAnime(3, "Jujutsu Kaisen", "Jujutsu Kaisen", 87, 24, "RELEASING", listOf("Action", "Fantasy")),
            createFallbackAnime(4, "My Hero Academia", "Boku no Hero Academia", 81, 138, "RELEASING", listOf("Action", "Comedy")),
            createFallbackAnime(5, "One Piece", "Wan Pīsu", 87, 1100, "RELEASING", listOf("Action", "Adventure")),
            createFallbackAnime(6, "Spy x Family", "Spy × Family", 88, 25, "FINISHED", listOf("Action", "Comedy")),
            createFallbackAnime(7, "Chainsaw Man", "Chainsaw Man", 85, 12, "FINISHED", listOf("Action", "Horror")),
            createFallbackAnime(8, "Bleach: Thousand-Year Blood War", "Bleach: Sennen Kessen-hen", 89, 26, "FINISHED", listOf("Action", "Supernatural")),
            createFallbackAnime(9, "Mob Psycho 100 III", "Mob Psycho Hyaku III", 90, 12, "FINISHED", listOf("Action", "Comedy")),
            createFallbackAnime(10, "Frieren: Beyond Journey's End", "Sousou no Frieren", 93, 28, "FINISHED", listOf("Adventure", "Fantasy"))
        )
    }

    private fun getFallbackSeasonAnime(): List<AnimeModel> {
        return listOf(
            createFallbackAnime(101, "Solo Leveling", "Na Honjaman Levelup", 83, 12, "RELEASING", listOf("Action", "Fantasy")),
            createFallbackAnime(102, "Mashle: Magic and Muscles Season 2", "Mashu", 80, 12, "RELEASING", listOf("Action", "Comedy")),
            createFallbackAnime(103, "The Apothecary Diaries", "Kusuriya no Hitorigoto", 88, 24, "FINISHED", listOf("Mystery", "Historical")),
            createFallbackAnime(104, "Undead Unluck", "Ando Rakkku", 82, 24, "FINISHED", listOf("Action", "Supernatural")),
            createFallbackAnime(105, "Shangri-La Frontier", "Sunraba Furontia", 84, 20, "RELEASING", listOf("Action", "Game")),
            createFallbackAnime(106, "The Foolish Angel Ditzes Around", "Oyaku Angel Pochiikusu", 76, 12, "FINISHED", listOf("Comedy", "Romance")),
            createFallbackAnime(107, "Tsuki ga Michibiku Isekai Douchuu 2nd Season", "Tsukimichi -Isekai Dochuu 2nd Season-", 81, 25, "FINISHED", listOf("Action", "Isekai")),
            createFallbackAnime(108, "Sakamoto Days", "Sakamoto Deizu", 85, 12, "RELEASING", listOf("Action", "Comedy")),
            createFallbackAnime(109, "Delicious in Dungeon", "Dungeon Meshi", 87, 24, "FINISHED", listOf("Adventure", "Comedy")),
            createFallbackAnime(110, "Classroom of the Elite III", "Youkoso Jitsuryoku Shijou Shugi no Kyoushitsu e 3rd Season", 79, 13, "FINISHED", listOf("Drama", "Psychological"))
        )
    }

    private fun createFallbackAnime(
        id: Int,
        englishTitle: String,
        romajiTitle: String,
        score: Int,
        episodes: Int,
        status: String,
        genres: List<String>
    ): AnimeModel {
        return AnimeModel(
            id = id,
            title = TitleModel(
                romaji = romajiTitle,
                english = englishTitle,
                native = null
            ),
            coverImage = CoverImageModel(
                extraLarge = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/${id}.jpg",
                large = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/${id}.jpg",
                medium = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/${id}.jpg"
            ),
            episodes = episodes,
            averageScore = score,
            genres = genres,
            status = status,
            seasonYear = 2024
        )
    }
}

class RemoteControlPreferences(
    context: Context
) {
    private val dataStore = context.remoteControlDataStore
    
    companion object {
        val REMOTE_CONTROL_ENABLED = booleanPreferencesKey("remote_control_enabled")
    }

    val isRemoteControlEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[REMOTE_CONTROL_ENABLED] ?: false
    }

    suspend fun setRemoteControlEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REMOTE_CONTROL_ENABLED] = enabled
        }
    }
}

fun createAniListApi(): AniListApi {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(AniListApi.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    return retrofit.create(AniListApi::class.java)
}
