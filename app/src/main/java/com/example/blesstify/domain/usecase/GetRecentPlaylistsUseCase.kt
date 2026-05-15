package com.example.blesstify.domain.usecase

import com.example.blesstify.domain.model.Playlist
import com.example.blesstify.domain.repository.HistoryRepository
import com.example.blesstify.domain.repository.PlaylistRepository
import javax.inject.Inject

class GetRecentPlaylistsUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(userId: String, limit: Int = 10): List<Playlist> {
        val history = historyRepository.getHistory(userId, 50) // Scan more history to find unique playlists
        val uniquePlaylistIds = history
            .mapNotNull { it.playlistId }
            .distinct()
            .take(limit)
        
        return uniquePlaylistIds.mapNotNull { id ->
            playlistRepository.getPlaylistById(id)
        }
    }
}
