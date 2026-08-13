package com.anitrack.app.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anitrack.app.ui.home.HomeViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anitrack.app.R
import com.anitrack.app.data.api.models.AnimeModel
import com.anitrack.app.ui.components.AnimeCard
import com.anitrack.app.ui.components.AniTrackBottomNavigation
import com.anitrack.app.ui.components.LoadingScreen
import androidx.compose.ui.geometry.Offset
import com.anitrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    onAnimeClick: (Int) -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val uiState = viewModel.uiState.collectAsState().value
    
    // Content without Scaffold - handled by AniTrackNavHost
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        when {
            uiState.isLoading -> LoadingScreen()
            uiState.error != null -> ErrorScreen(
                message = uiState.error ?: "Unknown error",
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
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            SkyBlue600,
                            SkyBlue400,
                            Aqua400,
                            Aqua500
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1f, 1f)
                    )
                )
                .padding(28.dp)
        ) {
            Column {
                // App Logo/Brand
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AniTrack",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = stringResource(R.string.home_welcome_back),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discover and track your favorite anime!",
                    style = MaterialTheme.typography.bodyLarge,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Decorative accent bar
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SkyBlue500,
                modifier = Modifier.size(4.dp, 20.dp)
            ) {}
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = stringResource(R.string.see_all),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SkyBlue600
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
            .width(170.dp)
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Cover Image with gradient overlay
            Box {
                AsyncImage(
                    model = anime.coverImage?.extraLarge ?: "",
                    contentDescription = anime.title?.english ?: anime.title?.romaji ?: "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentScale = ContentScale.Crop
                )
                
                // Subtle gradient at bottom of image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )
            }
            
            // Info Section
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = anime.title?.english 
                        ?: anime.title?.romaji 
                        ?: "Unknown Title",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Score Badge - Premium Style
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getScoreColor(anime.averageScore).copy(alpha = 0.15f),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent
                            )
                            Text(
                                text = "${anime.averageScore ?: "N/A"}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = getScoreColor(anime.averageScore)
                            )
                        }
                    }
                    
                    // Episodes
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SkyBlue50
                    ) {
                        Text(
                            text = "${anime.episodes ?: "?"} eps",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyBlue700,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

private fun getScoreColor(score: Double?): Color {
    return when {
        score == null -> RatingAverage
        score >= 80.0 -> RatingExcellent
        score >= 60.0 -> RatingGood
        score >= 40.0 -> RatingAverage
        else -> RatingPoor
    }
}
