package com.anitrack.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anitrack.app.R
import com.anitrack.app.data.api.models.AnimeModel
import com.anitrack.app.ui.theme.*

@Composable
fun AnimeCard(
    anime: AnimeModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image
            Surface(
                modifier = Modifier.size(width = 90.dp, height = 125.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = anime.coverImage ?: "",
                    contentDescription = anime.title?.english ?: "",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = anime.title?.english 
                            ?: anime.title?.romaji 
                            ?: "Unknown Title",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Score Badge
                        Surface(
                            shape = CircleShape,
                            color = getScoreColor(anime.averageScore).copy(alpha = 0.15f)
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
                        
                        // Status
                        if (!anime.status.isNullOrEmpty()) {
                            StatusBadge(status = anime.status!!)
                        }
                    }
                }
                
                // Genres (if available)
                if (!anime.genres.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        anime.genres!!.take(3).forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Purple100
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Purple700,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val backgroundColor = when (status.uppercase()) {
        "FINISHED" -> StatusFinished.copy(alpha = 0.15f)
        "RELEASING" -> StatusReleasing.copy(alpha = 0.15f)
        "NOT_YET_RELEASED" -> StatusNotYetReleased.copy(alpha = 0.15f)
        "CANCELLED" -> StatusCancelled.copy(alpha = 0.15f)
        else -> StatusHiatus.copy(alpha = 0.15f)
    }
    
    val textColor = when (status.uppercase()) {
        "FINISHED" -> StatusFinished
        "RELEASING" -> StatusReleasing
        "NOT_YET_RELEASED" -> StatusNotYetReleased
        "CANCELLED" -> StatusCancelled
        else -> StatusHiatus
    }
    
    Surface(
        shape = CircleShape,
        color = backgroundColor
    ) {
        Text(
            text = status.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
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
