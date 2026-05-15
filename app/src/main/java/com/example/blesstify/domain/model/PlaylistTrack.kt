package com.example.blesstify.domain.model

import com.google.firebase.Timestamp

data class PlaylistTrack(
    val songId: String = "",
    val order: Int = 0,
    val addedAt: Timestamp? = null,
    val addedBy: String = ""
)
