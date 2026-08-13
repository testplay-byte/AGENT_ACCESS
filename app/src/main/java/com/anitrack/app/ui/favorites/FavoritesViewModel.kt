package com.anitrack.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anitrack.app.data.api.models.AnimeModel
import com.anitrack.app.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val favorites: List<AnimeModel> = emptyList(),
    val isEmpty: Boolean = false,
    val error: String? = null
)

class FavoritesViewModel(
    private val repository: AnimeRepository
) : ViewModel() {

    companion object {
        fun create(context: Context): FavoritesViewModel {
            return FavoritesViewModel(
                repository = AnimeRepository.getInstance(context)
            )
        }
    }

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val favorites = repository.getFavorites()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    favorites = favorites,
                    isEmpty = favorites.isEmpty()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load favorites"
                )
            }
        }
    }

    fun removeFromFavorites(animeId: Int) {
        viewModelScope.launch {
            repository.removeFromFavorites(animeId)
            
            // Update UI state by removing from list
            val currentList = _uiState.value.favorites.toMutableList()
            currentList.removeAll { it.id == animeId }
            
            _uiState.value = _uiState.value.copy(
                favorites = currentList,
                isEmpty = currentList.isEmpty()
            )
        }
    }

    fun refresh() {
        loadFavorites()
    }
}
