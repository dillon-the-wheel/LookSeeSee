package com.lookseesee.app.ui.setup

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lookseesee.app.data.MediaPickKey
import com.lookseesee.app.data.MediaRepository
import com.lookseesee.app.data.MediaTypeFilter
import com.lookseesee.app.data.SettingsRepository
import com.lookseesee.app.data.model.Album
import com.lookseesee.app.data.model.MediaEntry
import com.lookseesee.app.util.AppLanguage
import com.lookseesee.app.util.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val albums: List<Album> = emptyList(),
    val selectedAlbumIds: Set<String> = emptySet(),
    val selectedMediaKeys: Set<String> = emptySet(),
    val minutes: Int = 10,
    val hasPin: Boolean = false,
    val silenceNotifications: Boolean = false,
    val mediaTypeFilter: MediaTypeFilter = MediaTypeFilter.BOTH,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val hasMediaPermission: Boolean = false,
    val isLoadingAlbums: Boolean = false,
    val readyToBegin: Boolean = false,
    // Album-detail browsing: non-null/true while the parent has drilled into either a
    // specific album (to hand-pick items within it) or their cross-album "My Picks" set.
    val openAlbum: Album? = null,
    val viewingPicks: Boolean = false,
    val albumDetailMedia: List<MediaEntry> = emptyList(),
    val isLoadingAlbumDetail: Boolean = false,
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
                        selectedMediaKeys = settings.selectedMediaKeys,
                        minutes = settings.minutes,
                        hasPin = settings.pinHash != null,
                        silenceNotifications = settings.silenceNotifications,
                        mediaTypeFilter = settings.mediaTypeFilter,
                        readyToBegin = settings.isReadyToBegin,
                    )
                }
            }
        }
        checkMediaPermission()
    }

    /** Checks the real Android permission state so a previous grant is remembered across launches. */
    private fun checkMediaPermission() {
        val context = getApplication<Application>()
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES,
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(hasMediaPermission = granted) }
        if (granted) refreshAlbums()
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

    /** Opens an album's own media grid so the parent can hand-pick specific items within it. */
    fun openAlbum(album: Album) {
        _uiState.update {
            it.copy(openAlbum = album, viewingPicks = false, isLoadingAlbumDetail = true, albumDetailMedia = emptyList())
        }
        viewModelScope.launch {
            val media = mediaRepository.loadMedia(bucketIds = setOf(album.bucketId))
            _uiState.update { it.copy(albumDetailMedia = media, isLoadingAlbumDetail = false) }
        }
    }

    /** Opens the cross-album grid of everything the parent has individually picked so far. */
    fun openPicks() {
        _uiState.update {
            it.copy(openAlbum = null, viewingPicks = true, isLoadingAlbumDetail = true, albumDetailMedia = emptyList())
        }
        viewModelScope.launch {
            val media = mediaRepository.loadMedia(bucketIds = emptySet(), individualKeys = _uiState.value.selectedMediaKeys)
            _uiState.update { it.copy(albumDetailMedia = media, isLoadingAlbumDetail = false) }
        }
    }

    fun closeAlbumDetail() {
        _uiState.update {
            it.copy(openAlbum = null, viewingPicks = false, albumDetailMedia = emptyList(), isLoadingAlbumDetail = false)
        }
    }

    fun toggleMediaPick(entry: MediaEntry) {
        val key = MediaPickKey.encode(entry.id, entry is MediaEntry.Video)
        val current = _uiState.value.selectedMediaKeys
        val isRemoving = key in current
        val next = if (isRemoving) current - key else current + key
        // The "My Picks" grid shows only picked items, so unpicking one there should
        // drop it from view immediately rather than waiting on a re-query.
        if (isRemoving && _uiState.value.viewingPicks) {
            _uiState.update { it.copy(albumDetailMedia = it.albumDetailMedia.filterNot { m -> m.id == entry.id }) }
        }
        viewModelScope.launch { settingsRepository.setSelectedMediaKeys(next) }
    }

    fun setMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setMinutes(minutes) }
    }

    fun setSilenceNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSilenceNotifications(enabled) }
    }

    fun setMediaTypeFilter(filter: MediaTypeFilter) {
        viewModelScope.launch { settingsRepository.setMediaTypeFilter(filter) }
    }

    fun setLanguage(language: AppLanguage) {
        LocaleManager.apply(language)
        _uiState.update { it.copy(language = language) }
    }

    fun submitPin(pin: String) {
        if (pin.length != 4) return
        viewModelScope.launch { settingsRepository.setPin(pin) }
    }
}
