package com.anitrack.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anitrack.app.ui.theme.*

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated loading indicator
            CircularProgressIndicator(
                color = Purple600,
                trackColor = Purple100,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ShimmerAnimeCard(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        ShimmerStart,
        ShimmerEnd,
        ShimmerStart
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 125.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = shimmerColors,
                            start = Offset(translateAnim, translateAnim),
                            end = Offset(translateAnim + 300f, translateAnim + 300f)
                        )
                    )
                    .padding(12.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Title placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = shimmerColors,
                                start = Offset(translateAnim, translateAnim),
                                end = Offset(translateAnim + 200f, translateAnim + 200f)
                            )
                        )
                )
                
                // Subtitle placeholders
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 2) 0.5f else (0.3f + index * 0.1f))
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = shimmerColors,
                                    start = Offset(translateAnim, translateAnim),
                                    end = Offset(translateAnim + 150f, translateAnim + 150f)
                                )
                            )
                    )
                }
                
                // Genre placeholders
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = shimmerColors,
                                        start = Offset(translateAnim, translateAnim),
                                        end = Offset(translateAnim + 100f, translateAnim + 100f)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
