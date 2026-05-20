package com.example.blesstify.data.remote

import com.example.blesstify.data.remote.dto.ApiSongDto
import com.example.blesstify.data.remote.dto.ApiSongsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BlesstifyApi {
    // Sample endpoint: GET /v1/songs/search?q=...&limit=...
    @GET("v1/songs/search")
    suspend fun searchSongs(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): ApiSongsResponse

    // Sample endpoint: GET /v1/songs/trending?limit=...
    @GET("v1/songs/trending")
    suspend fun getTrendingSongs(
        @Query("limit") limit: Int = 20
    ): ApiSongsResponse
}
