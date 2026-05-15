package com.example.blesstify.domain.model

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "system", // playlist_update, new_song, recommendation, subscription, system
    val data: Map<String, String> = emptyMap(),
    val createdAt: Long = 0L,
    val readAt: Long? = null
)
