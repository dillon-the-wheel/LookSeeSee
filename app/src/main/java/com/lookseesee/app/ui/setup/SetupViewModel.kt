package com.lookseesee.app.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lookseesee.app.data.MediaRepository
import com.lookseesee.app.data.SettingsRepository
import com.lookseesee.app.data.model.Album
import com.lookseesee.app.util.AppLanguage
import com.lookseesee.app.util.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val albums: List<Album> = emptyList(),
    val selectedAlbumIds: Set<String> = emptySet(),
    val minutes: Int = 10,
    val hasPin: Boolean = false,
    val pinMismatch: Boolean = false,
    val silenceNotifications: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val hasMediaPermission: Boolean = false,
    val isLoadingAlbums: Boolean = false,
    val readyToBegin: Boolean = false,
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(SetupUiState(language = LocaleManager.current()))
    val uiState: StateFlow<SetupUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        selectedAlbumIds = settings.selectedAlbumIds,
                        minutes = settings.minutes,
                        hasPin = settings.pinHash != null,
                        silenceNotifications = settings.silenceNotifications,
                        readyToBegin = settings.isReadyToBegin,
                    )
                }
            }
        }
    }

    fun onMediaPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasMediaPermission = granted) }
        if (granted) refreshAlbums()
    }

    fun refreshAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAlbums = true) }
            val albums = mediaRepository.loadAlbums()
            _uiState.update { it.copy(albums = albums, isLoadingAlbums = false) }
        }
    }

    fun toggleAlbum(bucketId: String) {
        val current = _uiState.value.selectedAlbumIds
        val next = if (bucketId in current) current - bucketId else current + bucketId
        viewModelScope.launch { settingsRepository.setSelectedAlbums(next) }
    }

    fun setMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setMinutes(minutes) }
    }

    fun setSilenceNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSilenceNotifications(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        LocaleManager.apply(language)
        _uiState.update { it.copy(language = language) }
    }

    /** Returns true and persists the PIN if [pin] and [confirmPin] match; otherwise flags a mismatch. */
    fun submitNewPin(pin: String, confirmPin: String): Boolean {
        if (pin.length < 4 || pin != confirmPin) {
            _uiState.update { it.copy(pinMismatch = true) }
            return false
        }
        _uiState.update { it.copy(pinMismatch = false) }
        viewModelScope.launch { settingsRepository.setPin(pin) }
        return true
    }

    fun clearPinMismatch() {
        _uiState.update { it.copy(pinMismatch = false) }
    }
}
