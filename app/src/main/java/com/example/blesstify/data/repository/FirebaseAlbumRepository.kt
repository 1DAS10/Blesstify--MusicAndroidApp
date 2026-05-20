package com.example.blesstify.data.repository

import android.net.Uri
import android.util.Log
import com.example.blesstify.domain.model.Album
import com.example.blesstify.domain.repository.AlbumRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseAlbumRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : AlbumRepository {

    companion object {
        private const val TAG = FirestoreKeys.TAG
    }

    private fun requireSignedInUid(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        require(!uid.isNullOrBlank()) { "Not signed in" }
        return uid
    }

    override suspend fun getAllAlbums(limit: Long): List<Album> {
        Log.d(TAG, "[albums] getAllAlbums start limit=$limit")
        return try {
            val snapshot = firestore.collection("albums")
                .orderBy("name", Query.Direction.ASCENDING)
                .limit(limit)
                .get()
                .await()

            val albums = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Album::class.java)?.copy(id = doc.id)
            }
            Log.d(TAG, "[albums] getAllAlbums success count=${albums.size}")
            albums
        } catch (e: Exception) {
            Log.e(TAG, "[albums] getAllAlbums failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getAlbumById(albumId: String): Album? {
        val id = albumId.trim()
        if (id.isEmpty()) return null
        Log.d(TAG, "[albums] getAlbumById start id=$id")
        return try {
            val doc = firestore.collection("albums").document(id).get().await()
            val album = doc.toObject(Album::class.java)?.copy(id = doc.id)
            Log.d(TAG, "[albums] getAlbumById success id=$id exists=${doc.exists()}")
            album
        } catch (e: Exception) {
            Log.e(TAG, "[albums] getAlbumById failed id=$id: ${e.message}", e)
            throw e
        }
    }

    override suspend fun createAlbum(album: Album): String {
        val uid = requireSignedInUid()
        val name = album.name.trim()
        require(name.isNotEmpty()) { "Album.name empty" }

        val now = Timestamp.now()
        val docRef = firestore.collection("albums").document() // auto-id
        val id = docRef.id

        Log.d(TAG, "[albums] createAlbum start id=$id name=$name")

        val data = hashMapOf<String, Any?>(
            "ownerId" to uid,

            "name" to name,
            "coverUrl" to album.coverUrl,
            "thumbnailUrl" to album.thumbnailUrl,
            "artistIds" to album.artistIds,
            "artistNames" to album.artistNames,
            "songIds" to album.songIds,
            "songCount" to album.songCount,
            "createdAt" to now,
            "updatedAt" to now
        )

        try {
            docRef.set(data).await()
            Log.d(TAG, "[albums] createAlbum success id=$id")
            return id
        } catch (e: Exception) {
            Log.e(TAG, "[albums] createAlbum failed id=$id: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updateAlbum(album: Album) {
        val uid = requireSignedInUid()

        val id = album.id.trim()
        require(id.isNotEmpty()) { "Album.id empty (use createAlbum for new)" }

        Log.d(TAG, "[albums] updateAlbum start id=$id name=${album.name}")

        val docRef = firestore.collection("albums").document(id)
        val exists = try {
            docRef.get().await().exists()
        } catch (_: Exception) {
            // If network flaky, still attempt merge write; rules will enforce ownership.
            true
        }

        val now = Timestamp.now()
        val data = hashMapOf<String, Any?>(
            // rules require ownerId present + immutable
            "ownerId" to uid,

            "name" to album.name,
            "coverUrl" to album.coverUrl,
            "thumbnailUrl" to album.thumbnailUrl,
            "artistIds" to album.artistIds,
            "artistNames" to album.artistNames,
            "songIds" to album.songIds,
            "songCount" to album.songCount,
            "updatedAt" to now
        )

        if (!exists) {
            data["createdAt"] = now
        }

        try {
            docRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "[albums] updateAlbum success id=$id")
        } catch (e: Exception) {
            Log.e(TAG, "[albums] updateAlbum failed id=$id: ${e.message}", e)
            throw e
        }
    }

    override suspend fun uploadAlbumCover(albumId: String, imageUri: Uri): String {
        val id = albumId.trim()
        require(id.isNotEmpty()) { "albumId empty" }

        return try {
            Log.d(TAG, "[albums] uploadAlbumCover start id=$id uri=$imageUri")
            val ref = storage.reference
                .child("albums")
                .child(id)
                .child("cover")
                .child("${System.currentTimeMillis()}.jpg")

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "[albums] uploadAlbumCover success id=$id")
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "[albums] uploadAlbumCover failed id=$id: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteAlbum(albumId: String) {
        requireSignedInUid() // enforce signed-in
        val id = albumId.trim()
        require(id.isNotEmpty()) { "albumId empty" }

        Log.d(TAG, "[albums] deleteAlbum start id=$id")
        try {
            firestore.collection("albums").document(id).delete().await()
            Log.d(TAG, "[albums] deleteAlbum success id=$id")
        } catch (e: Exception) {
            Log.e(TAG, "[albums] deleteAlbum failed id=$id: ${e.message}", e)
            throw e
        }
    }
}
