package com.anitrack.app.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anitrack.app.data.datastore.RemoteControlPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isRemoteControlEnabled: Boolean = false,
    val isDarkModeEnabled: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val appVersion: String = "1.0.0"
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val remoteControlPreferences: RemoteControlPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            remoteControlPreferences.isRemoteControlEnabled.collect { isEnabled ->
                _uiState.value = _uiState.value.copy(isRemoteControlEnabled = isEnabled)
            }
        }
    }

    fun toggleRemoteControl(enabled: Boolean) {
        viewModelScope.launch {
            remoteControlPreferences.setRemoteControlEnabled(enabled)
            _uiState.value = _uiState.value.copy(isRemoteControlEnabled = enabled)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkModeEnabled = enabled)
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isNotificationsEnabled = enabled)
    }
}
