package com.example.blesstify.domain.repository

import kotlinx.coroutines.flow.Flow

interface LikesRepository {
    suspend fun likeSong(userId: String, songId: String)
    suspend fun unlikeSong(userId: String, songId: String)
    fun isLiked(userId: String, songId: String): Flow<Boolean>
    suspend fun getLikedSongIds(userId: String): List<String>
}
