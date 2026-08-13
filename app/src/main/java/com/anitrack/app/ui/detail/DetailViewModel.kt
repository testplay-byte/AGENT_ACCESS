package com.anitrack.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.anitrack.app.data.api.models.AnimeModel
import com.anitrack.app.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context

data class DetailUiState(
    val isLoading: Boolean = true,
    val anime: AnimeModel? = null,
    val isFavorite: Boolean = false,
    val error: String? = null
)

class DetailViewModel(
    private val repository: AnimeRepository
) : ViewModel() {

    companion object {
        fun create(context: Context): DetailViewModel {
            return DetailViewModel(
                repository = AnimeRepository.getInstance(context)
            )
        }
        
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application
                DetailViewModel(AnimeRepository.getInstance(application))
            }
        }
    }

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadAnimeDetails(animeId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val anime = repository.getAnimeById(animeId)
                val isFavorite = repository.isFavorite(animeId)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    anime = anime,
                    isFavorite = isFavorite
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load anime details"
                )
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentAnime = _uiState.value.anime ?: return@launch
            
            if (_uiState.value.isFavorite) {
                repository.removeFromFavorites(currentAnime.id)
                _uiState.value = _uiState.value.copy(isFavorite = false)
            } else {
                repository.addToFavorites(currentAnime)
                _uiState.value = _uiState.value.copy(isFavorite = true)
            }
        }
    }

    fun checkIfFavorite(animeId: Int): Boolean {
        return repository.isFavorite(animeId)
    }
}
