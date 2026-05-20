package com.example.blesstify.data.repository

import com.example.blesstify.data.remote.NetworkResult
import com.example.blesstify.data.remote.dto.ApiSongsResponse

interface SongRepository {
    suspend fun searchSongs(query: String, limit: Int = 20): NetworkResult<ApiSongsResponse>
    suspend fun getTrendingSongs(limit: Int = 20): NetworkResult<ApiSongsResponse>
}
