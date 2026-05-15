package com.example.blesstify.domain.repository

import android.net.Uri
import com.example.blesstify.domain.model.Playlist
import com.example.blesstify.domain.model.PlaylistTrack
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    suspend fun createPlaylist(playlist: Playlist): String
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlistId: String)
    suspend fun getPlaylistById(playlistId: String): Playlist?
    suspend fun getPublicPlaylists(limit: Int = 50): List<Playlist>
    suspend fun getUserPlaylists(userId: String, limit: Int = 50): List<Playlist>
    suspend fun addTrack(playlistId: String, track: PlaylistTrack)
    suspend fun removeTrack(playlistId: String, songId: String)
    suspend fun getTracks(playlistId: String): List<PlaylistTrack>
    suspend fun uploadPlaylistCover(playlistId: String, uri: Uri): String
    suspend fun likePlaylist(userId: String, playlistId: String)
    suspend fun unlikePlaylist(userId: String, playlistId: String)
    fun isPlaylistLiked(userId: String, playlistId: String): Flow<Boolean>
    suspend fun getLikedPlaylists(userId: String): List<Playlist>
}
