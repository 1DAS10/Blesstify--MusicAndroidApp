package com.example.blesstify.presentation.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.model.EqualizerSettings
import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import androidx.media3.common.util.UnstableApi

@UnstableApi
class ExoPlayerMusicController(
    private val player: ExoPlayer
) : MusicController {

    companion object {
        private const val TAG = "HistoryDebug"
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var effectSessionId: Int = 0
    private var currentSettings: EqualizerSettings? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    override val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    override val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _canNext = MutableStateFlow(false)
    override val canNext: StateFlow<Boolean> = _canNext.asStateFlow()

    private val _canPrevious = MutableStateFlow(false)
    override val canPrevious: StateFlow<Boolean> = _canPrevious.asStateFlow()

    private val _onSongFinished = MutableSharedFlow<Pair<Song, Long>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val onSongFinished: SharedFlow<Pair<Song, Long>> = _onSongFinished

    private var _currentPlaylistId: String? = null
    override val currentPlaylistId: String? get() = _currentPlaylistId

    private var _currentSource: String = "unknown"
    override val currentSource: String get() = _currentSource

    private var playlist: List<Song> = emptyList()
    private var originalPlaylist: List<Song> = emptyList()
    private var lastPrioritySongIds: List<String> = emptyList()
    private var lastRecordedSong: Song? = null
    private var startTimeMs: Long = 0L
    private var totalTimeListenedMs: Long = 0L

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startTrackingPosition()
                } else {
                    stopTrackingPosition()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                updateSkipAvailability()

                if (mediaItem != null) {
                    val songId = mediaItem.mediaId
                    val song = playlist.find { it.id == songId }
                    if (song != null) {
                        Log.d(TAG, "onMediaItemTransition: new song='${song.title}' id=${song.id} reason=$reason")
                        _currentSong.value = song
                        lastRecordedSong = song
                        startTimeMs = 0L
                        totalTimeListenedMs = 0L
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "onPlaybackStateChanged: state=$playbackState lastSong=${lastRecordedSong?.title}")
                if (playbackState == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                    updateSkipAvailability()
                    // Apply EQ if we have settings
                    currentSettings?.let { applyEqualizer(it) }
                } else if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "STATE_ENDED: recording history for '${lastRecordedSong?.title}'")
                    checkAndRecordHistory(player.duration)
                }
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                updateSkipAvailability()
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                updateSkipAvailability()
                Log.d(TAG, "onPositionDiscontinuity: reason=$reason oldPos=${oldPosition.positionMs} newPos=${newPosition.positionMs} lastSong=${lastRecordedSong?.title}")

                // If the reason is an auto-transition or a skip, record the history for the PREVIOUS song
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION || 
                    reason == Player.DISCONTINUITY_REASON_SKIP) {
                    // IMPORTANT: We must record BEFORE onMediaItemTransition changes lastRecordedSong.
                    // But ExoPlayer fires onPositionDiscontinuity BEFORE onMediaItemTransition,
                    // so lastRecordedSong still points to the previous (correct) song here.
                    checkAndRecordHistory(oldPosition.positionMs)
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                super.onAudioSessionIdChanged(audioSessionId)
                currentSettings?.let { applyEqualizer(it) }
            }
        })
    }

    override fun play(song: Song) {
        playlist = listOf(song)
        originalPlaylist = playlist
        lastPrioritySongIds = emptyList()
        _isShuffleEnabled.value = false
        player.shuffleModeEnabled = false
        _currentSong.value = song
        // Initialize tracking for history recording
        lastRecordedSong = song
        startTimeMs = 0L
        totalTimeListenedMs = 0L
        Log.d(TAG, "play(): initialized tracking for '${song.title}' id=${song.id}")
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.audioUrl)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        updateSkipAvailability()
    }

    override fun setPlaylist(songs: List<Song>, startIndex: Int, playlistId: String?, source: String) {
        if (songs.isEmpty()) return
        this.playlist = songs
        this.originalPlaylist = songs
        this._currentPlaylistId = playlistId
        this._currentSource = source
        // Initialize tracking for the starting song
        val startSong = songs.getOrNull(startIndex.coerceIn(0, songs.lastIndex))
        if (startSong != null) {
            lastRecordedSong = startSong
            startTimeMs = 0L
            totalTimeListenedMs = 0L
            Log.d(TAG, "setPlaylist(): initialized tracking for '${startSong.title}' id=${startSong.id}")
        }
        player.shuffleModeEnabled = false
        if (_isShuffleEnabled.value) {
            val startSongId = songs.getOrNull(startIndex)?.id
            applySmartShuffle(lastPrioritySongIds, startSongId)
        } else {
            applyMediaItems(songs, startIndex, 0L, playWhenReady = true)
        }
        updateSkipAvailability()
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        player.play()
    }

    override fun seekTo(position: Long) {
        player.seekTo(position)
        _currentPosition.value = position
    }

    override fun stop() {
        checkAndRecordHistory(player.currentPosition)
        player.stop()
        player.clearMediaItems()
        _currentSong.value = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        stopTrackingPosition()
        updateSkipAvailability()
    }

    override fun next() {
        if (player.hasNextMediaItem()) {
            checkAndRecordHistory(player.currentPosition)
            player.seekToNextMediaItem()
        }
        updateSkipAvailability()
    }

    override fun previous() {
        if (player.hasPreviousMediaItem()) {
            checkAndRecordHistory(player.currentPosition)
            player.seekToPreviousMediaItem()
        }
        updateSkipAvailability()
    }

    private fun checkAndRecordHistory(currentPos: Long? = null) {
        val song = lastRecordedSong
        if (song == null) {
            Log.d(TAG, "checkAndRecordHistory: SKIPPED - lastRecordedSong is null")
            return
        }
        val finalPos = currentPos ?: player.currentPosition
        val durationPlayed = totalTimeListenedMs + (finalPos - startTimeMs).coerceAtLeast(0L)

        // Rule: finished OR > 30s
        val isFinished = player.playbackState == Player.STATE_ENDED || 
                        (player.duration > 0 && finalPos >= player.duration - 1000)
        val isLongEnough = durationPlayed >= 30_000L

        Log.d(TAG, "checkAndRecordHistory: song='${song.title}' finalPos=$finalPos startTime=$startTimeMs totalListened=$totalTimeListenedMs durationPlayed=${durationPlayed}ms playerDuration=${player.duration} isFinished=$isFinished isLongEnough=$isLongEnough")

        if (isFinished || isLongEnough) {
            Log.d(TAG, "checkAndRecordHistory: EMITTING history for '${song.title}' durationMs=$durationPlayed")
            val hasEmitted = _onSongFinished.tryEmit(song to durationPlayed)
            if (!hasEmitted) {
                Log.d(TAG, "checkAndRecordHistory: tryEmit failed, using suspend emit")
                scope.launch {
                    _onSongFinished.emit(song to durationPlayed)
                }
            }
        } else {
            Log.d(TAG, "checkAndRecordHistory: NOT recording - conditions not met")
        }

        // Reset tracking
        startTimeMs = finalPos
        totalTimeListenedMs = 0L
    }

    override fun toggleShuffle() {
        if (_isShuffleEnabled.value) {
            disableShuffle()
        } else {
            enableSmartShuffle(lastPrioritySongIds)
        }
    }

    override fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.NONE -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.NONE
        }
        setRepeatMode(nextMode)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        player.repeatMode = when (mode) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    override fun enableSmartShuffle(prioritySongIds: List<String>) {
        applySmartShuffle(prioritySongIds, _currentSong.value?.id)
        updateSkipAvailability()
    }

    override fun disableShuffle() {
        val baseList = originalPlaylist
        if (baseList.isEmpty()) return
        val currentSongId = _currentSong.value?.id
        val playWhenReady = _isPlaying.value
        val startPositionMs = if (currentSongId != null) player.currentPosition else 0L
        playlist = baseList
        _isShuffleEnabled.value = false
        player.shuffleModeEnabled = false
        val startIndex = baseList.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)
        applyMediaItems(baseList, startIndex, startPositionMs, playWhenReady)
        updateSkipAvailability()
    }

    private fun applySmartShuffle(prioritySongIds: List<String>, startSongId: String?) {
        val baseList = originalPlaylist.ifEmpty { playlist }
        if (baseList.isEmpty()) return
        lastPrioritySongIds = prioritySongIds
        val currentSongId = startSongId
        val playWhenReady = _isPlaying.value
        val startPositionMs = if (currentSongId != null) player.currentPosition else 0L
        val ordered = buildSmartOrder(baseList, prioritySongIds, currentSongId)
        playlist = ordered
        _isShuffleEnabled.value = true
        player.shuffleModeEnabled = false
        val startIndex = ordered.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)
        applyMediaItems(ordered, startIndex, startPositionMs, playWhenReady)
    }

    private fun buildSmartOrder(
        baseList: List<Song>,
        prioritySongIds: List<String>,
        currentSongId: String?
    ): List<Song> {
        val distinctPriorityIds = prioritySongIds.distinct()
        val prioritySongs = distinctPriorityIds.mapNotNull { id -> baseList.firstOrNull { it.id == id } }
        val priorityIdSet = prioritySongs.map { it.id }.toSet()
        val remaining = baseList.filterNot { it.id in priorityIdSet }
        val shuffledRemaining = remaining.shuffled()
        val combined = prioritySongs + shuffledRemaining
        val currentSong = currentSongId?.let { id -> baseList.firstOrNull { it.id == id } }
        return if (currentSong != null) {
            listOf(currentSong) + combined.filterNot { it.id == currentSong.id }
        } else {
            combined
        }
    }

    private fun applyMediaItems(
        songs: List<Song>,
        startIndex: Int,
        startPositionMs: Long,
        playWhenReady: Boolean
    ) {
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        val mediaItems = songs.map {
            MediaItem.Builder()
                .setMediaId(it.id)
                .setUri(it.audioUrl)
                .build()
        }
        player.setMediaItems(mediaItems, safeIndex, startPositionMs)
        player.prepare()
        if (playWhenReady) {
            player.play()
        } else {
            player.pause()
        }
        updateSkipAvailability()
    }

    private fun startTrackingPosition() {
        updateJob?.cancel()
        startTimeMs = player.currentPosition
        updateJob = scope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition
                _duration.value = player.duration.coerceAtLeast(0L)
                delay(1000L) // Update every second
            }
        }
    }

    private fun stopTrackingPosition() {
        totalTimeListenedMs += (player.currentPosition - startTimeMs).coerceAtLeast(0L)
        updateJob?.cancel()
    }

    private fun updateSkipAvailability() {
        _canNext.value = player.hasNextMediaItem()
        _canPrevious.value = player.hasPreviousMediaItem()
    }

    override fun applyEqualizer(settings: EqualizerSettings) {
        currentSettings = settings
        val sessionId = player.audioSessionId
        if (sessionId == 0) return // Not ready yet

        try {
            // Re-create effects if audio session changed (e.g. new song)
            if (effectSessionId != sessionId) {
                equalizer?.release()
                bassBoost?.release()
                virtualizer?.release()

                equalizer = Equalizer(0, sessionId)
                bassBoost = BassBoost(0, sessionId)
                virtualizer = Virtualizer(0, sessionId)

                effectSessionId = sessionId
                Log.d("EQDebug", "Created new audio effects for session $sessionId")
            }

            // --- Equalizer ---
            equalizer?.enabled = settings.isEnabled
            if (settings.isEnabled) {
                // Apply user-configured band levels
                settings.bands.forEachIndexed { index, level ->
                    val milliLevel = (level * 100).toShort() // Convert -12..12 to -1200..1200
                    try {
                        equalizer?.setBandLevel(index.toShort(), milliLevel)
                    } catch (e: Exception) {
                        Log.e("EQDebug", "Error setting band $index: ${e.message}")
                    }
                }
                Log.d("EQDebug", "EQ ON: bands=${settings.bands} preset=${settings.preset}")
            } else {
                // Reset all bands to 0 (flat) when disabled
                val numBands = equalizer?.numberOfBands ?: 5
                for (i in 0 until numBands) {
                    try {
                        equalizer?.setBandLevel(i.toShort(), 0)
                    } catch (_: Exception) {}
                }
                Log.d("EQDebug", "EQ OFF: all bands reset to 0")
            }

            // --- Bass Boost ---
            bassBoost?.enabled = settings.isEnabled
            if (settings.isEnabled) {
                bassBoost?.setStrength((settings.bassBoost * 1000).toInt().toShort())
                Log.d("EQDebug", "BassBoost ON: strength=${(settings.bassBoost * 1000).toInt()}")
            } else {
                bassBoost?.setStrength(0)
                Log.d("EQDebug", "BassBoost OFF: strength reset to 0")
            }

            // --- Virtualizer ---
            virtualizer?.enabled = settings.isEnabled
            if (settings.isEnabled) {
                virtualizer?.setStrength((settings.virtualizer * 1000).toInt().toShort())
                Log.d("EQDebug", "Virtualizer ON: strength=${(settings.virtualizer * 1000).toInt()}")
            } else {
                virtualizer?.setStrength(0)
                Log.d("EQDebug", "Virtualizer OFF: strength reset to 0")
            }

        } catch (e: Exception) {
            Log.e("EQDebug", "Failed to apply audio effects", e)
        }
    }
}
