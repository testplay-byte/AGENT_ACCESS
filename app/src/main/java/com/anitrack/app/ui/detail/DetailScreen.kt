package com.anitrack.app.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anitrack.app.ui.detail.DetailViewModel
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
    viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory)
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
                model = anime.coverImage?.extraLarge ?: "",
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
            
            // Top Bar - Premium Style with blur effect
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Back Button
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Favorite Button with gradient when active
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(48.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isFavorite) {
                                    Brush.linearGradient(
                                        colors = listOf(CoralAccent, OrangeAccent)
                                    )
                                } else {
                                    Brush.solidColor(Color.Black.copy(alpha = 0.45f))
                                },
                                CircleShape
                            ),
                        shape = CircleShape,
                        onClick = onToggleFavorite,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color.White else favoriteIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
            // Title Card - Premium Style
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp)
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
                    anime.title?.native?.takeIf { it.isNotEmpty() }?.let { nativeTitle ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = nativeTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Stats Row
                    StatsRow(anime = anime)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Favorite Button - Premium Gradient Style
                    Surface(
                        onClick = onToggleFavorite,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = if (isFavorite) {
                                            listOf(CoralAccent, OrangeAccent)
                                        } else {
                                            listOf(SkyBlue500, Aqua400)
                                        }
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Text(
                                    text = if (isFavorite) stringResource(R.string.detail_remove_from_favorites) 
                                           else stringResource(R.string.detail_add_to_favorites),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
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
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        anime.genres!!.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = SkyBlue50,
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SkyBlue700,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
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
            icon = "★",
            iconColor = GoldAccent,
            color = getScoreColor(anime.averageScore)
        )
        
        StatItem(
            label = "Episodes",
            value = "${anime.episodes ?: "?"}",
            icon = "▶",
            iconColor = SkyBlue500,
            color = SkyBlue600
        )
        
        StatItem(
            label = "Status",
            value = formatShortStatus(anime.status),
            icon = "●",
            iconColor = getStatusColor(anime.status),
            color = getStatusColor(anime.status)
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: String, iconColor: Color, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.1f),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.labelLarge,
                    color = iconColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoSection(title: Int, content: @Composable () -> Unit) {
    Column {
        // Section Header with accent bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SkyBlue500,
                modifier = Modifier.size(4.dp, 18.dp)
            ) {}
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Aqua500,
                    modifier = Modifier.size(4.dp, 18.dp)
                ) {}
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
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
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CoralAccent.copy(alpha = 0.1f),
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = CoralAccent
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SkyBlue500
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
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

private fun getScoreColor(score: Double?): Color {
    return when {
        score == null -> RatingAverage
        score >= 80.0 -> RatingExcellent
        score >= 60.0 -> RatingGood
        score >= 40.0 -> RatingAverage
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
