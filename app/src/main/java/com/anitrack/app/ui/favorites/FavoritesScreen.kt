package com.anitrack.app.ui.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anitrack.app.ui.favorites.FavoritesViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anitrack.app.R
import com.anitrack.app.data.api.models.AnimeModel
import com.anitrack.app.ui.components.AniTrackBottomNavigation
import com.anitrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory),
    onAnimeClick: (Int) -> Unit
) {
    val uiState = viewModel.uiState.collectAsState().value
    
    Scaffold(
        bottomBar = {
            AniTrackBottomNavigation(
                currentRoute = "favorites",
                onNavigate = { }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> LoadingContent()
                uiState.isEmpty -> EmptyFavorites()
                else -> FavoritesList(
                    favorites = uiState.favorites,
                    onAnimeClick = onAnimeClick,
                    onRemoveFavorite = { animeId -> viewModel.removeFromFavorites(animeId) }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = CoralAccent,
                trackColor = CoralAccent.copy(alpha = 0.2f),
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Loading favorites...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyFavorites() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CoralAccent.copy(alpha = 0.1f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = CoralAccent
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.favorites_empty),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.favorites_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FavoritesList(
    favorites: List<AnimeModel>,
    onAnimeClick: (Int) -> Unit,
    onRemoveFavorite: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Section
        item {
            FavoritesHeader()
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        items(favorites, key = { it.id }) { anime ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                FavoriteCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime.id) },
                    onRemove = { onRemoveFavorite(anime.id) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    anime: AnimeModel,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image with favorite indicator
            Box(
                modifier = Modifier.size(width = 85.dp, height = 115.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 3.dp
                ) {
                    AsyncImage(
                        model = anime.coverImage?.extraLarge ?: "",
                        contentDescription = anime.title?.english ?: "",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Favorite badge - Premium Style
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(CoralAccent, OrangeAccent)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = anime.title?.english 
                        ?: anime.title?.romaji 
                        ?: "Unknown Title",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score Badge - Premium Style
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = getScoreColor(anime.averageScore).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent
                            )
                            Text(
                                text = "${anime.averageScore ?: "N/A"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = getScoreColor(anime.averageScore)
                            )
                        }
                    }
                    
                    // Episodes
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Aqua50
                    ) {
                        Text(
                            text = "${anime.episodes ?: "?"} eps",
                            style = MaterialTheme.typography.labelSmall,
                            color = Aqua700,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                
                if (!anime.genres.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        anime.genres!!.take(2).forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CoralAccent.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoralAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Remove button - Styled
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = CoralAccent.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from favorites",
                    tint = CoralAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FavoritesHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CoralAccent.copy(alpha = 0.9f),
                        OrangeAccent.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.25f)
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.favorites_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Your collection of loved anime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
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
