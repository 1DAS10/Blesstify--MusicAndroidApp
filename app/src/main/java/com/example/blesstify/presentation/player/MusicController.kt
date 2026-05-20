package com.example.blesstify.presentation.player

import com.example.blesstify.domain.model.Song
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode {
    NONE, ONE, ALL
}

data class PlaybackQueueState(
    val items: List<Song> = emptyList(),
    val currentIndex: Int = -1
) {
    val current: Song? get() = items.getOrNull(currentIndex)
}

interface MusicController {
    val currentSong: StateFlow<Song?>
    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val isShuffleEnabled: StateFlow<Boolean>
    val repeatMode: StateFlow<RepeatMode>
    val canNext: StateFlow<Boolean>
    val canPrevious: StateFlow<Boolean>
    val currentPlaylistId: String?
    val currentSource: String
    val onSongFinished: SharedFlow<Pair<Song, Long>> // Emits (Song, DurationListenedMs) when song should be recorded

    // Manual queue state (for UI + add-to-queue)
    val queueState: StateFlow<PlaybackQueueState>

    fun play(song: Song)
    fun pause()
    fun resume()
    fun seekTo(position: Long)
    fun stop()

    /** Replace queue with [songs], start at [startIndex]. */
    fun setPlaylist(
        songs: List<Song>,
        startIndex: Int = 0,
        playlistId: String? = null,
        source: String = "unknown"
    )

    fun next()
    fun previous()
    fun toggleShuffle()
    fun toggleRepeat()
    fun setRepeatMode(mode: RepeatMode)
    fun applyEqualizer(settings: com.example.blesstify.domain.model.EqualizerSettings)
    fun enableSmartShuffle(prioritySongIds: List<String>)
    fun disableShuffle()

    // Queue ops
    fun addToQueue(song: Song)
    fun addToQueueNext(song: Song)
    fun playQueueIndex(index: Int)
    fun removeQueueIndex(index: Int)
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun clearQueue(keepCurrent: Boolean = true)
}
