package com.anitrack.app.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anitrack.app.ui.search.SearchViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
    onAnimeClick: (Int) -> Unit
) {
    val uiState = viewModel.uiState.collectAsState().value
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    Scaffold(
        bottomBar = {
            AniTrackBottomNavigation(
                currentRoute = "search",
                onNavigate = { }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClear = { viewModel.clearSearch() },
                focusRequester = focusRequester,
                focusManager = focusManager
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            when {
                uiState.isSearching -> LoadingState()
                uiState.hasSearched && uiState.searchResults.isEmpty() && !uiState.isSearching -> {
                    NoResultsState()
                }
                uiState.hasSearched && uiState.searchResults.isNotEmpty() -> {
                    SearchResultsList(
                        results = uiState.searchResults,
                        onAnimeClick = onAnimeClick
                    )
                }
                else -> InitialState()
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    focusManager: FocusManager
) {
    // Header section with gradient background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SkyBlue500,
                        SkyBlue400,
                        Aqua400
                    )
                )
            )
            .padding(top = 24.dp, bottom = 20.dp, horizontal = 16.dp)
    ) {
        // Title
        Text(
            text = "Search Anime",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Find your next favorite anime",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Input Field - Premium Style
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = SkyBlue500,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_hint),
                            color = SkyBlue300
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = SkyBlue500
                    )
                )
                
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = onClear) {
                        Surface(
                            shape = CircleShape,
                            color = SkyBlue50
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = SkyBlue600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SkyBlue50,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = SkyBlue400
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Find Your Anime",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Search for anime by title, genre, or keyword",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = SkyBlue500,
                trackColor = SkyBlue100,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Searching...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoResultsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.search_no_results),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.search_try_different),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchResultsList(
    results: List<AnimeModel>,
    onAnimeClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(results, key = { it.id }) { anime ->
            SearchResultItem(
                anime = anime,
                onClick = { onAnimeClick(anime.id) }
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    anime: AnimeModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image with subtle border
            Surface(
                modifier = Modifier.size(width = 75.dp, height = 105.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                AsyncImage(
                    model = anime.coverImage?.extraLarge ?: "",
                    contentDescription = anime.title?.english ?: "",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
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
                    
                    // Status
                    if (!anime.status.isNullOrEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = getStatusColor(anime.status!!).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = formatStatusShort(anime.status!!),
                                style = MaterialTheme.typography.labelSmall,
                                color = getStatusColor(anime.status!!),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Genres
                if (!anime.genres.isNullOrEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        anime.genres!!.take(3).forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SkyBlue50
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyBlue700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
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

private fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "FINISHED" -> StatusFinished
        "RELEASING" -> StatusReleasing
        "NOT_YET_RELEASED" -> StatusNotYetReleased
        "CANCELLED" -> StatusCancelled
        else -> StatusHiatus
    }
}

private fun formatStatusShort(status: String): String {
    return when (status.uppercase()) {
        "FINISHED" -> "Done"
        "RELEASING" -> "Airing"
        "NOT_YET_RELEASED" -> "TBA"
        "CANCELLED" -> "Cancelled"
        else -> status
    }
}
