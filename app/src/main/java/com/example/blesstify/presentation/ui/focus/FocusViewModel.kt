package com.example.blesstify.presentation.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Song
import com.example.blesstify.presentation.player.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.repository.PlaylistRepository
import com.example.blesstify.domain.repository.SongRepository
import com.example.blesstify.domain.usecase.GetSongsByIdsUseCase
import com.example.blesstify.domain.usecase.ObserveAuthStateUseCase
import com.example.blesstify.domain.usecase.GetLikedSongIdsUseCase
import com.example.blesstify.domain.usecase.GetPublicSongsUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import com.example.blesstify.presentation.player.RepeatMode
import com.example.blesstify.domain.usecase.SearchSongsUseCase

enum class FocusMode { WORK, BREAK }

data class FocusUiState(
    val remainingTime: String = "25:00",
    val progress: Float = 1.0f,
    val isRunning: Boolean = false,
    val mode: FocusMode = FocusMode.WORK,
    val sessionLabel: String = "Deep Work",
    val sessionInfo: String = "SESSION 1/4",
    val workDurationMin: Int = 25,
    val breakDurationMin: Int = 5,
    val focusSongs: List<Song> = emptyList(),
    val recommendedSongs: List<Song> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val addingSongId: String? = null,
    val currentSong: Song? = null
)


@HiltViewModel
class FocusViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val getSongsByIdsUseCase: GetSongsByIdsUseCase,
    private val getLikedSongIdsUseCase: GetLikedSongIdsUseCase,
    private val getPublicSongsUseCase: GetPublicSongsUseCase,
    private val searchSongsUseCase: SearchSongsUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val musicController: MusicController
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var loadFocusJob: Job? = null
    private var totalSeconds = 25 * 60
    private var currentSeconds = totalSeconds
    private var currentSession = 1
    private var currentUserId: String? = null
    private var focusInitialized: Boolean? = null

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collectLatest { state ->
                if (state is AuthState.Authenticated) {
                    currentUserId = state.userId
                    focusInitialized = null
                    loadFocusMusic(state.userId)
                }
            }
        }

        // Sync local currentSong with MusicController's actual state
        viewModelScope.launch {
            musicController.currentSong.collectLatest { song ->
                if (song != null && _uiState.value.focusSongs.any { it.id == song.id }) {
                    _uiState.update { it.copy(currentSong = song) }
                }
            }
        }

        updateTimerDisplay()

        // Load recommended songs for the "Add Song" dialog
        viewModelScope.launch {
            getPublicSongsUseCase(20).collect { resource ->
                if (resource is Resource.Success<*>) {
                    _uiState.update { it.copy(recommendedSongs = resource.data ?: emptyList()) }
                }
            }
        }
    }

    fun addSongToFocus(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(addingSongId = song.id) }
            delay(300) // Brief delay for visual feedback

            val updatedSongs = (_uiState.value.focusSongs + song).distinctBy { it.id }
            _uiState.update { it.copy(focusSongs = updatedSongs, addingSongId = null) }

            // Save to Firestore AFTER state is updated, using the new list
            currentUserId?.let { userId ->
                val updatedIds = updatedSongs.map { it.id }
                focusInitialized = true
                songRepository.saveFocusSongIds(userId, updatedIds)
            }
        }
    }

    fun removeSongFromFocus(song: Song) {
        viewModelScope.launch {
            val updatedSongs = _uiState.value.focusSongs.filter { it.id != song.id }
            _uiState.update { it.copy(focusSongs = updatedSongs) }

            currentUserId?.let { userId ->
                focusInitialized = true
                songRepository.saveFocusSongIds(userId, updatedSongs.map { it.id })
            }
        }
    }

    private fun loadFocusMusic(userId: String) {
        loadFocusJob?.cancel()
        loadFocusJob = viewModelScope.launch {
            try {
                android.util.Log.d("FocusVM", "Loading focus music for user: $userId")

                if (focusInitialized == null) {
                    focusInitialized = when (val initResource = songRepository.getFocusInitialized(userId)) {
                        is Resource.Success<*> -> initResource.data == true
                        else -> false
                    }
                }

                // Real-time listener — collectLatest cancels previous inner work when new snapshot arrives
                songRepository.getFocusSongIds(userId).collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading<*> -> Unit // ignore loading state
                        is Resource.Success<*> -> {
                            val savedIds = resource.data ?: emptyList()
                            if (savedIds.isNotEmpty()) {
                                // Use first() to get a single value from the inner Flow
                                val songsResource = getSongsByIdsUseCase(savedIds)
                                    .first { it !is Resource.Loading<*> }
                                if (songsResource is Resource.Success<*>) {
                                    updateFocusSongs(songsResource.data ?: emptyList())
                                }
                            } else {
                                // No saved songs yet — auto-generate only for first-time setup
                                if (focusInitialized != true && _uiState.value.focusSongs.isEmpty()) {
                                    focusInitialized = true
                                    runAutoGeneration(userId)
                                }
                            }
                        }
                        is Resource.Error<*> -> {
                            android.util.Log.e("FocusVM", "Error loading focus song IDs: ${resource.error?.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusVM", "Failed to load focus music", e)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isNotEmpty()) {
            searchSongs(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    private fun searchSongs(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            searchSongsUseCase(query, currentUserId).collect { resource ->
                if (resource is Resource.Success<*>) {
                    _uiState.update { it.copy(searchResults = resource.data ?: emptyList(), isSearching = false) }
                } else if (resource is Resource.Error<*>) {
                    _uiState.update { it.copy(isSearching = false) }
                }
            }
        }
    }


    private fun runAutoGeneration(userId: String) {
        viewModelScope.launch {
            val allSongs = mutableListOf<Song>()

            // 1. Fetch from Liked Playlists
            val likedPlaylists = playlistRepository.getLikedPlaylists(userId)
            for (playlist in likedPlaylists.take(3)) {
                val tracks = playlistRepository.getTracks(playlist.id)
                val songIds = tracks.map { it.songId }
                if (songIds.isNotEmpty()) {
                    getSongsByIdsUseCase(songIds).collect { resource ->
                        if (resource is Resource.Success) {
                            allSongs.addAll(resource.data ?: emptyList())
                        }
                    }
                }
            }

            // 2. Fetch from Liked Songs (if still few)
            if (allSongs.size < 5) {
                val likedIds = getLikedSongIdsUseCase(userId)
                if (likedIds.isNotEmpty()) {
                    getSongsByIdsUseCase(likedIds.take(20)).collect { resource ->
                        if (resource is Resource.Success) {
                            allSongs.addAll(resource.data ?: emptyList())
                        }
                    }
                }
            }

            // 3. Fallback to Public Focus Music (if still empty)
            if (allSongs.isEmpty()) {
                getPublicSongsUseCase(15).collect { resource ->
                    if (resource is Resource.Success) {
                        allSongs.addAll(resource.data ?: emptyList())
                    }
                }
            }

            // Update UI and persist to Firestore so next launch restores them
            val distinctSongs = allSongs.distinctBy { it.id }
            updateFocusSongs(distinctSongs)
            if (distinctSongs.isNotEmpty()) {
                songRepository.saveFocusSongIds(userId, distinctSongs.map { it.id })
            }
        }
    }

    private fun updateFocusSongs(songs: List<Song>) {
        val distinctSongs = songs.distinctBy { it.id }
        _uiState.update { it.copy(focusSongs = distinctSongs) }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(isRunning = true) }
        
        // Music logic: Resume or start playing
        if (!musicController.isPlaying.value) {
            val currentSong = _uiState.value.currentSong
            if (currentSong != null) {
                musicController.resume()
            } else if (_uiState.value.focusSongs.isNotEmpty()) {
                playSong(_uiState.value.focusSongs.first())
            }
        }

        timerJob = viewModelScope.launch {
            while (currentSeconds > 0) {
                delay(1000)
                currentSeconds--
                updateTimerDisplay()
            }
            onTimerFinished()
        }
    }

    private fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerJob?.cancel()
        musicController.pause()
    }

    fun resetTimer() {
        pauseTimer()
        val duration = if (_uiState.value.mode == FocusMode.WORK) _uiState.value.workDurationMin else _uiState.value.breakDurationMin
        totalSeconds = duration * 60
        currentSeconds = totalSeconds
        updateTimerDisplay()
    }

    fun skipSession() {
        onTimerFinished()
    }

    private fun onTimerFinished() {
        // Skip to next song on session change if music is playing
        if (musicController.isPlaying.value) {
            musicController.next()
        }

        if (_uiState.value.mode == FocusMode.WORK) {
            // Switch to Break
            _uiState.update { 
                it.copy(
                    mode = FocusMode.BREAK,
                    sessionLabel = "Short Break"
                )
            }
            totalSeconds = _uiState.value.breakDurationMin * 60
            currentSeconds = totalSeconds
            updateTimerDisplay()
            startTimer() // Auto start break
        } else {
            // Switch to Work
            if (currentSession < 4) {
                currentSession++
                _uiState.update { 
                    it.copy(
                        mode = FocusMode.WORK,
                        sessionLabel = "Deep Work",
                        sessionInfo = "SESSION $currentSession/4"
                    )
                }
                totalSeconds = _uiState.value.workDurationMin * 60
                currentSeconds = totalSeconds
                updateTimerDisplay()
                startTimer() // Auto start next work session
            } else {
                // Completed all 4 sessions
                pauseTimer()
                currentSession = 1
                _uiState.update { 
                    it.copy(
                        mode = FocusMode.WORK,
                        sessionLabel = "Set Completed!",
                        sessionInfo = "SESSION 1/4",
                        isRunning = false
                    )
                }
                resetTimer()
            }
        }
    }

    fun updateWorkDuration(minutes: Int) {
        _uiState.update { it.copy(workDurationMin = minutes) }
        if (_uiState.value.mode == FocusMode.WORK) resetTimer()
    }

    fun updateBreakDuration(minutes: Int) {
        _uiState.update { it.copy(breakDurationMin = minutes) }
        if (_uiState.value.mode == FocusMode.BREAK) resetTimer()
    }

    private fun updateTimerDisplay() {
        val minutes = currentSeconds / 60
        val seconds = currentSeconds % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)
        val progress = if (totalSeconds > 0) currentSeconds.toFloat() / totalSeconds else 0f
        _uiState.update { it.copy(remainingTime = timeStr, progress = progress) }
    }

    fun playSong(song: Song) {
        val allSongs = _uiState.value.focusSongs
        val index = allSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        
        _uiState.update { it.copy(currentSong = song) }
        musicController.setPlaylist(
            songs = allSongs,
            startIndex = index,
            playlistId = "focus_session",
            source = "focus_mode"
        )
        musicController.setRepeatMode(RepeatMode.ALL)
    }
}
