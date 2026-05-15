package com.example.blesstify.domain.model

data class ListeningHistory(
    val id: String = "",
    val songId: String = "",
    val playlistId: String? = null,
    val playedAt: Long = 0L,
    val durationSec: Int = 0,
    val source: String = "unknown",
    val device: String = "android"
)

