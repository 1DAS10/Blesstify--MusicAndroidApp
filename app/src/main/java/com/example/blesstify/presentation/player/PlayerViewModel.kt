package com.example.blesstify.presentation.player

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.core.utils.ShareUtils
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.model.ListeningHistory
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.usecase.AddTrackToPlaylistUseCase
import com.example.blesstify.domain.usecase.GetEqualizerSettingsUseCase
import com.example.blesstify.domain.usecase.GetLatestRecommendationUseCase
import com.example.blesstify.domain.usecase.GetListeningHistoryUseCase
import com.example.blesstify.domain.usecase.GetUserPlaylistsUseCase
import com.example.blesstify.domain.usecase.GetPublicSongsUseCase
import com.example.blesstify.domain.usecase.GetSongsByIdsUseCase
import com.example.blesstify.domain.usecase.IsLikedUseCase
import com.example.blesstify.domain.usecase.LikeSongUseCase
import com.example.blesstify.domain.usecase.LogListeningHistoryUseCase
import com.example.blesstify.domain.usecase.ObserveAuthStateUseCase
import com.example.blesstify.domain.usecase.UnlikeSongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val musicController: MusicController,
    @ApplicationContext private val appContext: Context,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val isLikedUseCase: IsLikedUseCase,
    private val likeSongUseCase: LikeSongUseCase,
    private val unlikeSongUseCase: UnlikeSongUseCase,
    private val logListeningHistoryUseCase: LogListeningHistoryUseCase,
    private val getListeningHistoryUseCase: GetListeningHistoryUseCase,
    private val getLatestRecommendationUseCase: GetLatestRecommendationUseCase,
    private val getSongsByIdsUseCase: GetSongsByIdsUseCase,
    private val getPublicSongsUseCase: GetPublicSongsUseCase,
    private val getEqualizerSettingsUseCase: GetEqualizerSettingsUseCase,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase
) : ViewModel() {

    val currentSong = musicController.currentSong
    val isPlaying = musicController.isPlaying
    val currentPosition = musicController.currentPosition
    val duration = musicController.duration
    val isShuffleEnabled = musicController.isShuffleEnabled
    val repeatMode = musicController.repeatMode
    val canNext = musicController.canNext
    val canPrevious = musicController.canPrevious
    val queueState = musicController.queueState

    private val _isLiked = MutableStateFlow(false)
    val isLiked = _isLiked.asStateFlow()

    private var currentUserId: String? = null
    private var likeJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs = _sleepTimerRemainingMs.asStateFlow()

    private val _uiEvents = MutableStateFlow<PlayerUiEvent?>(null)
    val uiEvents = _uiEvents.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<com.example.blesstify.domain.model.Playlist>>(emptyList())
    val userPlaylists = _userPlaylists.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                observeAuthStateUseCase().collectLatest { state ->
                    if (state is AuthState.Authenticated) {
                        currentUserId = state.userId
                        observeLikeStatus()
                        loadEqualizerSettings()
                        refreshUserPlaylists()
                    } else {
                        currentUserId = null
                    }
                }
            } catch (e: Exception) {
                currentUserId = null
                _isLiked.value = false
            }
        }

        viewModelScope.launch {
            try {
                currentSong.collectLatest {
                    observeLikeStatus()
                }
            } catch (e: Exception) {
                _isLiked.value = false
            }
        }

        viewModelScope.launch {
            try {
                musicController.onSongFinished.collect { (song, durationMs) ->
                    Log.d("HistoryDebug", "VM: onSongFinished received song='${song.title}' durationMs=$durationMs uid=$currentUserId")
                    logHistory(song, durationMs)
                }
            } catch (e: Exception) {
                Log.e("HistoryDebug", "VM: onSongFinished collector failed", e)
            }
        }
    }

    private fun observeLikeStatus() {
        likeJob?.cancel()
        val uid = currentUserId
        val songId = currentSong.value?.id
        
        if (uid != null && songId != null) {
            likeJob = viewModelScope.launch {
                isLikedUseCase(uid, songId).collect { liked ->
                    _isLiked.value = liked
                }
            }
        } else {
            _isLiked.value = false
        }
    }

    private fun loadEqualizerSettings() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val result = getEqualizerSettingsUseCase(uid)
            if (result is com.example.blesstify.domain.user.UserResult.Success && result.data != null) {
                musicController.applyEqualizer(result.data)
            }
        }
    }

    private fun refreshUserPlaylists() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                val playlists = getUserPlaylistsUseCase(uid, limit = 100)
                _userPlaylists.value = playlists
            } catch (_: Exception) {
                _userPlaylists.value = emptyList()
            }
        }
    }

    private fun emitMessage(text: String) {
        _uiEvents.value = PlayerUiEvent.Message(text)
        // Clear quickly so same message can be emitted again
        viewModelScope.launch {
            delay(50)
            _uiEvents.value = null
        }
    }

    // --- More actions (3-dots) ---

    fun shareCurrentSong() {
        val song = currentSong.value ?: return
        try {
            ShareUtils.shareSong(appContext, song)
        } catch (e: Exception) {
            emitMessage("Share failed")
        }
    }

    fun addCurrentSongToQueue() {
        val song = currentSong.value ?: return
        musicController.addToQueue(song)
        emitMessage("Added to queue")
    }

    fun playCurrentSongNext() {
        val song = currentSong.value ?: return
        musicController.addToQueueNext(song)
        emitMessage("Will play next")
    }


    fun addCurrentSongToPlaylist(playlistId: String) {
        val uid = currentUserId ?: run {
            emitMessage("Please login")
            return
        }
        val song = currentSong.value ?: return
        viewModelScope.launch {
            try {
                val track = com.example.blesstify.domain.model.PlaylistTrack(
                    songId = song.id,
                    order = 0,
                    addedBy = uid
                )
                addTrackToPlaylistUseCase(playlistId, track)
                refreshUserPlaylists()
                emitMessage("Added to playlist")
            } catch (_: Exception) {
                emitMessage("Add to playlist failed")
            }
        }
    }

    fun toggleLike() {
        val uid = currentUserId ?: return
        val songId = currentSong.value?.id ?: return
        
        viewModelScope.launch {
            try {
                if (_isLiked.value) {
                    unlikeSongUseCase(uid, songId)
                } else {
                    likeSongUseCase(uid, songId)
                }
            } catch (e: Exception) {
                // Best effort
            }
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            try {
                val queue = buildAutoQueueForSingleSong(song)
                if (queue.size > 1) {
                    musicController.setPlaylist(queue, 0, playlistId = null, source = "single_auto")
                } else {
                    musicController.play(song)
                }
            } catch (e: Exception) {
                musicController.play(song)
            }
        }
    }

    fun playSongFromSearch(song: Song) {
        viewModelScope.launch {
            try {
                val queue = buildSearchQueue(song)
                if (queue.size > 1) {
                    musicController.setPlaylist(queue, 0, playlistId = null, source = "search_random")
                } else {
                    musicController.play(song)
                }
            } catch (e: Exception) {
                musicController.play(song)
            }
        }
    }

    fun playFromList(
        songs: List<Song>,
        startIndex: Int,
        source: String,
        playlistId: String? = null
    ) {
        if (songs.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        musicController.setPlaylist(songs, safeIndex, playlistId, source)
    }

    fun playPause() {
        if (isPlaying.value) {
            musicController.pause()
        } else {
            musicController.resume()
        }
    }

    fun seekTo(position: Long) {
        musicController.seekTo(position)
    }

    fun next() {
        musicController.next()
    }

    fun previous() {
        musicController.previous()
    }

    fun toggleShuffle() {
        val enable = !isShuffleEnabled.value
        if (!enable) {
            musicController.disableShuffle()
            return
        }
        viewModelScope.launch {
            val priorityIds = buildTrendPrioritySongIds()
            musicController.enableSmartShuffle(priorityIds)
        }
    }

    fun toggleRepeat() {
        musicController.toggleRepeat()
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingMs.value = null
            return
        }
        val durationMs = minutes * 60_000L
        val endAtMs = System.currentTimeMillis() + durationMs
        _sleepTimerRemainingMs.value = durationMs
        sleepTimerJob = viewModelScope.launch {
            while (true) {
                val remaining = endAtMs - System.currentTimeMillis()
                if (remaining <= 0L) break
                _sleepTimerRemainingMs.value = remaining
                delay(min(1_000L, remaining))
            }
            musicController.pause()
            _sleepTimerRemainingMs.value = null
        }
    }

    fun clearSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMs.value = null
    }

    private suspend fun buildTrendPrioritySongIds(): List<String> {
        val uid = currentUserId ?: return emptyList()
        val recommendationIds = try {
            getLatestRecommendationUseCase(uid)?.songIds.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }

        val historyIds = try {
            val history = getListeningHistoryUseCase(uid, 200)
            val now = System.currentTimeMillis()
            history
                .groupBy { it.songId }
                .map { (songId, items) ->
                    val totalDuration = items.sumOf { it.durationSec }
                    val latestPlayedAt = items.maxOfOrNull { it.playedAt } ?: 0L
                    val daysAgo = ((now - latestPlayedAt).coerceAtLeast(0L) / 86_400_000.0)
                    val recencyBoost = 1.0 / (1.0 + daysAgo)
                    val score = totalDuration + (recencyBoost * 300)
                    songId to score
                }
                .sortedByDescending { it.second }
                .map { it.first }
        } catch (e: Exception) {
            emptyList()
        }

        return (recommendationIds + historyIds)
            .distinct()
            .take(30)
    }

    private suspend fun buildAutoQueueForSingleSong(song: Song): List<Song> {
        val allSongs = fetchPublicSongs(limit = 200)
        val shuffledPool = allSongs
            .filterNot { it.id == song.id }
            .distinctBy { it.id }
            .shuffled()

        return buildList {
            add(song)
            shuffledPool
                .take(100)
                .forEach { add(it) }
        }
    }

    private suspend fun buildSearchQueue(song: Song): List<Song> {
        val publicSongs = fetchPublicSongs(limit = 200)
        val shuffledPool = publicSongs
            .filterNot { it.id == song.id }
            .distinctBy { it.id }
            .shuffled()
            .take(50)

        return buildList {
            add(song)
            shuffledPool.forEach { add(it) }
        }
    }

    private suspend fun fetchSongsByIds(songIds: List<String>): List<Song> {
        if (songIds.isEmpty()) return emptyList()
        val result = try {
            getSongsByIdsUseCase(songIds)
                .first { it !is Resource.Loading }
        } catch (e: Exception) {
            return emptyList()
        }
        return (result as? Resource.Success)?.data.orEmpty()
    }

    private suspend fun fetchPublicSongs(limit: Int): List<Song> {
        val result = try {
            getPublicSongsUseCase(limit)
                .first { it !is Resource.Loading }
        } catch (e: Exception) {
            return emptyList()
        }
        return (result as? Resource.Success)?.data.orEmpty()
    }

    private fun logHistory(song: Song, durationMs: Long) {
        val uid = currentUserId
        if (uid == null) {
            Log.w("HistoryDebug", "VM logHistory: SKIPPED - currentUserId is null")
            return
        }
        viewModelScope.launch {
            try {
                val item = ListeningHistory(
                    songId = song.id,
                    playlistId = musicController.currentPlaylistId,
                    durationSec = (durationMs / 1000).toInt(),
                    source = musicController.currentSource,
                    device = "android"
                )
                Log.d("HistoryDebug", "VM logHistory: calling useCase for songId=${song.id} durationSec=${item.durationSec} uid=$uid")
                logListeningHistoryUseCase(uid, item)
                Log.d("HistoryDebug", "VM logHistory: SUCCESS for '${song.title}'")
            } catch (e: Exception) {
                Log.e("HistoryDebug", "VM logHistory: FAILED for '${song.title}'", e)
            }
        }
    }
}
