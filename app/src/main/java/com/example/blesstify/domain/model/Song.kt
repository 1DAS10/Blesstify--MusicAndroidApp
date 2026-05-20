package com.example.blesstify.domain.model

import com.google.firebase.Timestamp

data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String? = null,
    val artistIds: List<String> = emptyList(),
    val album: String? = null,
    val albumId: String? = null,
    val durationSec: Int = 0,
    val coverUrl: String? = null,
    val thumbnailUrl: String? = null,
    val albumCoverUrl: String? = null,
    val artistCoverUrl: String? = null,
    val audioUrl: String? = null,
    val genre: List<String> = emptyList(),
    val moodTags: List<String> = emptyList(),
    val energy: Double = 0.5,
    val ownerId: String = "",
    val isPublic: Boolean = false,
    val status: String = "draft",
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val isHiRes: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun Song.listArtworkUrl(): String? = thumbnailUrl?.takeIf { it.isNotBlank() } ?: coverUrl

fun Song.detailArtworkUrl(): String? = coverUrl?.takeIf { it.isNotBlank() } ?: thumbnailUrl
