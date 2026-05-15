package com.example.blesstify.presentation.player

import com.example.blesstify.domain.model.Song
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode {
    NONE, ONE, ALL
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

    fun play(song: Song)
    fun pause()
    fun resume()
    fun seekTo(position: Long)
    fun stop()
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0, playlistId: String? = null, source: String = "unknown")
    fun next()
    fun previous()
    fun toggleShuffle()
    fun toggleRepeat()
    fun setRepeatMode(mode: RepeatMode)
    fun applyEqualizer(settings: com.example.blesstify.domain.model.EqualizerSettings)
    fun enableSmartShuffle(prioritySongIds: List<String>)
    fun disableShuffle()
}
