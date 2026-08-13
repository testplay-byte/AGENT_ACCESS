package com.anitrack.app.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anitrack.app.R
import com.anitrack.app.data.api.models.AnimeModel
import com.anitrack.app.ui.components.AnimeCard
import com.anitrack.app.ui.components.AniTrackBottomNavigation
import com.anitrack.app.ui.components.LoadingScreen
import com.anitrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = remember { HomeViewModel.create(android.app.ActivityThread.currentApplication()!!) },
    onAnimeClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState
    
    Scaffold(
        bottomBar = {
            AniTrackBottomNavigation(
                currentRoute = "home",
                onNavigate = { }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AniTrack",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            brush = Brush.linearGradient(
                                colors = listOf(Purple600, Pink500)
                            )
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> LoadingScreen()
                uiState.error != null -> ErrorScreen(
                    message = uiState.error!!,
                    onRetry = { viewModel.refresh() }
                )
                else -> HomeContent(
                    trendingAnime = uiState.trendingAnime,
                    popularThisSeason = uiState.popularThisSeason,
                    onAnimeClick = onAnimeClick
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    trendingAnime: List<AnimeModel>,
    popularThisSeason: List<AnimeModel>,
    onAnimeClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Welcome Section
        item {
            WelcomeSection()
        }
        
        // Trending Carousel Section
        item {
            SectionHeader(title = R.string.home_trending)
        }
        
        item {
            TrendingCarousel(
                animeList = trendingAnime,
                onAnimeClick = onAnimeClick
            )
        }
        
        // Popular This Season Section
        item {
            SectionHeader(title = R.string.home_popular_this_season)
        }
        
        items(popularThisSeason) { anime ->
            AnimeCard(
                anime = anime,
                onClick = { onAnimeClick(anime.id) }
            )
        }
    }
}

@Composable
private fun WelcomeSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Purple600.copy(alpha = 0.8f),
                            Pink500.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_welcome_back),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Discover and track your favorite anime!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: Int, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            )
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = stringResource(R.string.see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TrendingCarousel(
    animeList: List<AnimeModel>,
    onAnimeClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        animeList.forEachIndexed { index, anime ->
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "scale"
            )
            
            TrendingCard(
                anime = anime,
                scale = scale,
                onClick = { onAnimeClick(anime.id) }
            )
        }
    }
}

@Composable
private fun TrendingCard(
    anime: AnimeModel,
    scale: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            // Cover Image
            AsyncImage(
                model = anime.coverImage ?: "",
                contentDescription = anime.title?.english ?: anime.title?.romaji ?: "",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )
            
            // Info Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = anime.title?.english 
                            ?: anime.title?.romaji 
                            ?: "Unknown Title",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Score Badge
                        Surface(
                            shape = CircleShape,
                            color = getScoreColor(anime.averageScore).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${anime.averageScore ?: "N/A"}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = getScoreColor(anime.averageScore),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        
                        // Episodes
                        Text(
                            text = "${anime.episodes ?: "?"} eps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

private fun getScoreColor(score: Int?): Color {
    return when {
        score == null -> RatingAverage
        score >= 80 -> RatingExcellent
        score >= 60 -> RatingGood
        score >= 40 -> RatingAverage
        else -> RatingPoor
    }
}
