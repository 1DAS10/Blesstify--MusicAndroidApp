package com.example.blesstify.domain.repository

import android.net.Uri
import com.example.blesstify.domain.model.Artist

interface ArtistRepository {
    suspend fun getAllArtists(limit: Long = 200): List<Artist>
    suspend fun getArtistById(artistId: String): Artist?
    suspend fun createArtist(artist: Artist): String
    suspend fun updateArtist(artist: Artist)
    suspend fun uploadArtistCover(artistId: String, imageUri: Uri): String
    suspend fun deleteArtist(artistId: String)
}
