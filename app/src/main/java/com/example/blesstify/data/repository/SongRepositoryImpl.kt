package com.example.blesstify.data.repository

import com.example.blesstify.data.remote.BlesstifyRemoteDataSource
import com.example.blesstify.data.remote.NetworkResult
import com.example.blesstify.data.remote.dto.ApiSongsResponse
import javax.inject.Inject

class SongRepositoryImpl @Inject constructor(
    private val remote: BlesstifyRemoteDataSource
) : SongRepository {

    override suspend fun searchSongs(query: String, limit: Int): NetworkResult<ApiSongsResponse> =
        remote.searchSongs(query = query, limit = limit)

    override suspend fun getTrendingSongs(limit: Int): NetworkResult<ApiSongsResponse> =
        remote.getTrendingSongs(limit = limit)
}
