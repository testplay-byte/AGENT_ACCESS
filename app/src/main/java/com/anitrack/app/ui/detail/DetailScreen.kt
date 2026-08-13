package com.anitrack.app.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anitrack.app.R
import com.anitrack.app.ui.components.LoadingScreen
import com.anitrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    animeId: Int,
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = remember { 
        val context = LocalContext.current.applicationContext
        DetailViewModel.create(context)
    }
) {
    val uiState = viewModel.uiState.collectAsState().value
    
    LaunchedEffect(animeId) {
        viewModel.loadAnimeDetails(animeId)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingScreen()
            uiState.error != null -> ErrorContent(message = uiState.error!!, onRetry = { viewModel.loadAnimeDetails(animeId) })
            uiState.anime != null -> DetailContent(
                anime = uiState.anime!!,
                isFavorite = uiState.isFavorite,
                onBackClick = onBackClick,
                onToggleFavorite = { viewModel.toggleFavorite() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    anime: com.anitrack.app.data.api.models.AnimeModel,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val scrollState = rememberScrollState()
    val favoriteIconColor by animateColorAsState(
        targetValue = if (isFavorite) Pink500 else Color.White.copy(alpha = 0.8f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "favoriteColor"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Hero Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            AsyncImage(
                model = anime.coverImage ?: "",
                contentDescription = anime.title?.english ?: "",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            ),
                            startY = 200f
                        )
                    )
            )
            
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = favoriteIconColor
                    )
                }
            }
        }
        
        // Content Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-40).dp)
        ) {
            // Title Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Title
                    Text(
                        text = anime.title?.english 
                            ?: anime.title?.romaji 
                            ?: "Unknown Title",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Native title if available
                    if (!anime.title?.native.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = anime.title!!.native!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Stats Row
                    StatsRow(anime = anime)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Favorite Button
                    Button(
                        onClick = onToggleFavorite,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFavorite) Pink500 else Purple600
                        )
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFavorite) stringResource(R.string.detail_remove_from_favorites) 
                                   else stringResource(R.string.detail_add_to_favorites)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Synopsis Section
            if (!anime.description.isNullOrEmpty()) {
                InfoSection(title = R.string.detail_synopsis) {
                    Text(
                        text = stripHtmlTags(anime.description!!),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Genres Section
            if (!anime.genres.isNullOrEmpty()) {
                InfoSection(title = R.string.detail_genres) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        anime.genres!!.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Purple100
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Purple700,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Additional Info
            InfoCard(title = "Additional Information") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow(label = "Status", value = formatStatus(anime.status))
                    InfoRow(label = "Episodes", value = anime.episodes?.toString() ?: stringResource(R.string.detail_not_available))
                    InfoRow(label = "Average Score", value = "${anime.averageScore ?: "N/A"} / 100")
                    InfoRow(label = "Season Year", value = anime.seasonYear?.toString() ?: stringResource(R.string.detail_not_available))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatsRow(anime: com.anitrack.app.data.api.models.AnimeModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            label = "Score",
            value = "${anime.averageScore ?: "N/A"}%",
            color = getScoreColor(anime.averageScore)
        )
        
        StatItem(
            label = "Episodes",
            value = "${anime.episodes ?: "?"}",
            color = Purple600
        )
        
        StatItem(
            label = "Status",
            value = formatShortStatus(anime.status),
            color = getStatusColor(anime.status)
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoSection(title: Int, content: @Composable () -> Unit) {
    Column {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

// Helper functions
private fun stripHtmlTags(html: String): String {
    return html.replace(Regex("<[^>]*>"), "").trim()
}

private fun formatStatus(status: String?): String {
    return when (status?.uppercase()) {
        "FINISHED" -> "Finished"
        "RELEASING" -> "Releasing"
        "NOT_YET_RELEASED" -> "Not Yet Released"
        "CANCELLED" -> "Cancelled"
        "HIATUS" -> "Hiatus"
        else -> status ?: "N/A"
    }
}

private fun formatShortStatus(status: String?): String {
    return when (status?.uppercase()) {
        "FINISHED" -> "Done"
        "RELEASING" -> "Airing"
        "NOT_YET_RELEASED" -> "TBA"
        "CANCELLED" -> "Cancelled"
        "HIATUS" -> "Hiatus"
        else -> "?"
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

private fun getStatusColor(status: String?): Color {
    return when (status?.uppercase()) {
        "FINISHED" -> StatusFinished
        "RELEASING" -> StatusReleasing
        "NOT_YET_RELEASED" -> StatusNotYetReleased
        "CANCELLED" -> StatusCancelled
        "HIATUS" -> StatusHiatus
        else -> StatusHiatus
    }
}
