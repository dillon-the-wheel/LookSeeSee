package com.lookseesee.app.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lookseesee.app.data.MediaRepository
import com.lookseesee.app.data.SettingsRepository
import com.lookseesee.app.data.model.MediaEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val mediaList: List<MediaEntry> = emptyList(),
    val currentIndex: Int = 0,
    val showGrid: Boolean = false,
    val remainingMillis: Long = 0L,
    val isLoading: Boolean = true,
)

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState

    private val _sessionEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionEnded: SharedFlow<Unit> = _sessionEnded

    private var totalDurationMillis: Long = 0L
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val media = mediaRepository.loadMedia(settings.selectedAlbumIds)
            totalDurationMillis = settings.minutes * 60_000L
            _uiState.update {
                it.copy(
                    mediaList = media,
                    currentIndex = 0,
                    remainingMillis = totalDurationMillis,
                    isLoading = false,
                )
            }
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.remainingMillis > 0) {
                delay(1_000)
                _uiState.update { it.copy(remainingMillis = (it.remainingMillis - 1_000).coerceAtLeast(0)) }
            }
            _sessionEnded.tryEmit(Unit)
        }
    }

    fun goNext() = moveBy(1)

    fun goPrevious() = moveBy(-1)

    private fun moveBy(delta: Int) {
        val state = _uiState.value
        if (state.mediaList.isEmpty()) return
        val nextIndex = (state.currentIndex + delta).mod(state.mediaList.size)
        _uiState.update { it.copy(currentIndex = nextIndex) }
    }

    fun openGrid() {
        _uiState.update { it.copy(showGrid = true) }
    }

    fun selectFromGrid(index: Int) {
        _uiState.update { it.copy(currentIndex = index, showGrid = false) }
    }

    fun closeGrid() {
        _uiState.update { it.copy(showGrid = false) }
    }

    /** Bottom-right corner tap: skip straight to the end screen. */
    fun skipToEnd() {
        countdownJob?.cancel()
        _sessionEnded.tryEmit(Unit)
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
