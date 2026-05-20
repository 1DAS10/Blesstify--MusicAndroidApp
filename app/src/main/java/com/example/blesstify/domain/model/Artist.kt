package com.example.blesstify.domain.model

import com.google.firebase.Timestamp

data class Artist(
    val id: String = "",
    val name: String = "",
    val coverUrl: String? = null,
    val thumbnailUrl: String? = null,
    val albumIds: List<String> = emptyList(),
    val songIds: List<String> = emptyList(),
    val songCount: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
