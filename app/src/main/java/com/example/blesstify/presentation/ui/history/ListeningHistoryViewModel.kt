package com.example.blesstify.presentation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.domain.model.ListeningHistory
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.usecase.ClearListeningHistoryUseCase
import com.example.blesstify.domain.usecase.GetListeningHistoryUseCase
import com.example.blesstify.domain.usecase.GetSongsByIdsUseCase
import com.example.blesstify.domain.usecase.ObserveAuthStateUseCase
import com.example.blesstify.domain.usecase.ObserveListeningHistoryUseCase
import com.example.blesstify.presentation.player.MusicController
import com.example.blesstify.core.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class HistoryStats(
    val totalHours: Double = 0.0,
    val totalTracks: Int = 0,
    val totalArtists: Int = 0,
    val hiResPercentage: Int = 0
)

data class HistoryItem(
    val history: ListeningHistory,
    val song: Song?
)

data class HistoryGroup(
    val title: String,
    val items: List<HistoryItem>
)

data class HistoryUiState(
    val groups: List<HistoryGroup> = emptyList(),
    val stats: HistoryStats = HistoryStats(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ListeningHistoryViewModel @Inject constructor(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getListeningHistoryUseCase: GetListeningHistoryUseCase,
    private val observeListeningHistoryUseCase: ObserveListeningHistoryUseCase,
    private val getSongsByIdsUseCase: GetSongsByIdsUseCase,
    private val clearListeningHistoryUseCase: ClearListeningHistoryUseCase,
    private val musicController: MusicController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var historyJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { state ->
                if (state is com.example.blesstify.domain.auth.AuthState.Authenticated) {
                    currentUserId = state.userId
                    loadHistory()
                } else {
                    currentUserId = null
                    historyJob?.cancel()
                    _uiState.value = _uiState.value.copy(groups = emptyList(), isLoading = false)
                }
            }
        }
    }

    fun loadHistory() {
        val uid = currentUserId ?: return
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                observeListeningHistoryUseCase(uid).collect { historyEntries ->
                    val songIds = historyEntries.map { it.songId }.distinct()
                    
                    val songMap = if (songIds.isNotEmpty()) {
                        val resource = getSongsByIdsUseCase(songIds)
                            .filter { it is Resource.Success || it is Resource.Error }
                            .first()
                        
                        if (resource is Resource.Success) {
                            resource.data?.associateBy { it.id } ?: emptyMap()
                        } else {
                            emptyMap()
                        }
                    } else {
                        emptyMap()
                    }

                    val items = historyEntries.map { history ->
                        HistoryItem(history, songMap[history.songId])
                    }
                    val groups = groupHistory(items)
                    val stats = calculateStats(items)

                    _uiState.value = HistoryUiState(groups = groups, stats = stats, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun clearHistory() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                clearListeningHistoryUseCase(uid)
                _uiState.value = HistoryUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun playSong(song: Song) {
        musicController.setPlaylist(listOf(song), 0, null, "history")
    }

    private fun groupHistory(items: List<HistoryItem>): List<HistoryGroup> {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.timeInMillis

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = dateFormat.format(Date(today))
        val yesterdayStr = dateFormat.format(Date(yesterday))

        // Count play frequency per songId across all history
        val playCountMap = items.groupBy { it.history.songId }
            .mapValues { it.value.size }

        return items.groupBy { item ->
            val dateStr = dateFormat.format(Date(item.history.playedAt))
            when (dateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(item.history.playedAt))
            }
        }.map { (title, groupItems) ->
            // Sort: most played songs first, then by most recent
            val sorted = groupItems.sortedWith(
                compareByDescending<HistoryItem> { playCountMap[it.history.songId] ?: 0 }
                    .thenByDescending { it.history.playedAt }
            )
            HistoryGroup(title, sorted)
        }
    }

    private fun calculateStats(items: List<HistoryItem>): HistoryStats {
        val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
        val recentItems = items.filter { it.history.playedAt >= sevenDaysAgo }
        
        if (recentItems.isEmpty()) return HistoryStats()

        val totalSec = recentItems.sumOf { it.history.durationSec }
        val distinctTracks = recentItems.map { it.history.songId }.distinct().size
        val distinctArtists = recentItems.mapNotNull { it.song?.artist }.distinct().size
        val hiResCount = recentItems.count { it.song?.isHiRes == true }
        val hiResPercentage = (hiResCount.toDouble() / recentItems.size * 100).toInt()

        return HistoryStats(
            totalHours = totalSec.toDouble() / 3600.0,
            totalTracks = distinctTracks,
            totalArtists = distinctArtists,
            hiResPercentage = hiResPercentage
        )
    }
}
