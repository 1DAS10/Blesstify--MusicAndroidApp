package com.example.blesstify.domain.repository

import android.net.Uri
import com.example.blesstify.domain.model.Album

interface AlbumRepository {
    suspend fun getAllAlbums(limit: Long = 200): List<Album>
    suspend fun getAlbumById(albumId: String): Album?
    suspend fun createAlbum(album: Album): String
    suspend fun updateAlbum(album: Album)
    suspend fun uploadAlbumCover(albumId: String, imageUri: Uri): String
    suspend fun deleteAlbum(albumId: String)
}
