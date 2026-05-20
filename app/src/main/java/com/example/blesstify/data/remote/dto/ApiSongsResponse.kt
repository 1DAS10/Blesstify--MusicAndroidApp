package com.example.blesstify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiSongsResponse(
    @SerializedName("data") val data: List<ApiSongDto> = emptyList(),
    @SerializedName("total") val total: Int = data.size
)
