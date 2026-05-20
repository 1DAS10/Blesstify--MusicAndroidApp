package com.example.blesstify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiSongDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("coverUrl") val coverUrl: String?,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String?
)
