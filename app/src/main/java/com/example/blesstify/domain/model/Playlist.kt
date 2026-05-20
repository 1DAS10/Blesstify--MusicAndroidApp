package com.example.blesstify.domain.model

import com.google.firebase.Timestamp

data class Playlist(
    val id: String = "",
    val ownerId: String = "",
    val collaboratorIds: List<String> = emptyList(),
    val title: String = "",
    val description: String? = null,
    val isPublic: Boolean = false,
    val isDefault: Boolean = false,
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val likeCount: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
