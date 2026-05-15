package com.example.blesstify.domain.repository

import android.net.Uri
import com.example.blesstify.domain.model.Song
import com.example.blesstify.core.utils.Resource
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSong(songId: String): Flow<Resource<Song>>
    fun getSongsByIds(songIds: List<String>): Flow<Resource<List<Song>>>
    fun addSong(song: Song): Flow<Resource<String>>
    fun searchSongs(query: String, userId: String? = null): Flow<Resource<List<Song>>>

    /** Upload audio file to Storage, optionally upload cover, create song + uploadJob documents */
    fun uploadSong(
        audioUri: Uri,
        audioFileName: String,
        coverUri: Uri?,
        coverFileName: String?,
        song: Song
    ): Flow<Resource<String>>

    fun getPublicSongs(limit: Int = 10): Flow<Resource<List<Song>>>
    fun getUserSongs(userId: String): Flow<Resource<List<Song>>>
    fun getTrendingSongs(limit: Int = 10, monthsBack: Int = 2): Flow<Resource<List<Song>>>
    fun getSongsByMood(mood: String): Flow<Resource<List<Song>>>
    fun getSongsByGenre(genre: String, userId: String? = null): Flow<Resource<List<Song>>>

    // Focus Mode Persistence
    fun getFocusSongIds(userId: String): Flow<Resource<List<String>>>
    suspend fun saveFocusSongIds(userId: String, songIds: List<String>): Resource<Unit>
    suspend fun getFocusInitialized(userId: String): Resource<Boolean>
}
