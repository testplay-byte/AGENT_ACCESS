package com.anitrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anitrack.app.data.api.models.AnimeModel
import com.anitrack.app.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context

data class HomeUiState(
    val isLoading: Boolean = true,
    val trendingAnime: List<AnimeModel> = emptyList(),
    val popularThisSeason: List<AnimeModel> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val repository: AnimeRepository
) : ViewModel() {

    companion object {
        fun create(context: Context): HomeViewModel {
            return HomeViewModel(
                repository = AnimeRepository.getInstance(context)
            )
        }
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load trending anime
                val trendingResult = repository.getTrendingAnime()
                
                // Load popular this season (current season)
                val seasonResult = repository.getPopularThisSeason()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    trendingAnime = trendingResult,
                    popularThisSeason = seasonResult
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun refresh() {
        loadHomeData()
    }
}
