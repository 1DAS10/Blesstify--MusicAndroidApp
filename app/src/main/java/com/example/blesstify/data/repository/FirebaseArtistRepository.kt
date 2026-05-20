package com.example.blesstify.data.repository

import android.net.Uri
import android.util.Log
import com.example.blesstify.domain.model.Artist
import com.example.blesstify.domain.repository.ArtistRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseArtistRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ArtistRepository {

    companion object {
        private const val TAG = FirestoreKeys.TAG
    }

    private fun requireSignedInUid(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        require(!uid.isNullOrBlank()) { "Not signed in" }
        return uid
    }

    override suspend fun getAllArtists(limit: Long): List<Artist> {
        Log.d(TAG, "[artists] getAllArtists start limit=$limit")
        return try {
            val snapshot = firestore.collection("artists")
                .orderBy("name", Query.Direction.ASCENDING)
                .limit(limit)
                .get()
                .await()

            val artists = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Artist::class.java)?.copy(id = doc.id)
            }
            Log.d(TAG, "[artists] getAllArtists success count=${artists.size}")
            artists
        } catch (e: Exception) {
            Log.e(TAG, "[artists] getAllArtists failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getArtistById(artistId: String): Artist? {
        val id = artistId.trim()
        if (id.isEmpty()) return null
        Log.d(TAG, "[artists] getArtistById start id=$id")
        return try {
            val doc = firestore.collection("artists").document(id).get().await()
            val artist = doc.toObject(Artist::class.java)?.copy(id = doc.id)
            Log.d(TAG, "[artists] getArtistById success id=$id exists=${doc.exists()}")
            artist
        } catch (e: Exception) {
            Log.e(TAG, "[artists] getArtistById failed id=$id: ${e.message}", e)
            throw e
        }
    }

    override suspend fun createArtist(artist: Artist): String {
        val uid = requireSignedInUid()
        val name = artist.name.trim()
        require(name.isNotEmpty()) { "Artist.name empty" }

        val now = Timestamp.now()
        val docRef = firestore.collection("artists").document() // auto-id
        val id = docRef.id

        Log.d(TAG, "[artists] createArtist start id=$id name=$name")

        val data = hashMapOf<String, Any?>(
            "ownerId" to uid,
            "name" to name,
            "coverUrl" to artist.coverUrl,
            "thumbnailUrl" to artist.thumbnailUrl,
            "albumIds" to artist.albumIds,
            "songIds" to artist.songIds,
            "songCount" to artist.songCount,
            "createdAt" to now,
            "updatedAt" to now
        )

        try {
            // set() (no merge) for new doc
            docRef.set(data).await()
            Log.d(TAG, "[artists] createArtist success id=$id")
            return id
        } catch (e: Exception) {
            Log.e(TAG, "[artists] createArtist failed id=$id: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updateArtist(artist: Artist) {
        val uid = requireSignedInUid()

        val id = artist.id.trim()
        require(id.isNotEmpty()) { "Artist.id empty (use createArtist for new)" }

        Log.d(TAG, "[artists] updateArtist start id=$id name=${artist.name}")

        val docRef = firestore.collection("artists").document(id)
        val exists = try {
            docRef.get().await().exists()
        } catch (_: Exception) {
            true
        }

        val now = Timestamp.now()
        val data = hashMapOf<String, Any?>(
            "ownerId" to uid,

            "name" to artist.name,
            "coverUrl" to artist.coverUrl,
            "thumbnailUrl" to artist.thumbnailUrl,
            "albumIds" to artist.albumIds,
            "songIds" to artist.songIds,
            "songCount" to artist.songCount,
            "updatedAt" to now
        )

        if (!exists) {
            data["createdAt"] = now
        }

        try {
            docRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "[artists] updateArtist success id=$id")
        } catch (e: Exception) {
            Log.e(TAG, "[artists] updateArtist failed id=$id: ${e.message}", e)
            throw e
        }
    }

    override suspend fun uploadArtistCover(artistId: String, imageUri: Uri): String {
        val id = artistId.trim()
        require(id.isNotEmpty()) { "artistId empty" }

        return try {
            Log.d(TAG, "[artists] uploadArtistCover start id=$id uri=$imageUri")
            val ref = storage.reference
                .child("artists")
                .child(id)
                .child("cover")
                .child("${System.currentTimeMillis()}.jpg")

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "[artists] uploadArtistCover success id=$id")
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "[artists] uploadArtistCover failed id=$id: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteArtist(artistId: String) {
        requireSignedInUid() // enforce signed-in
        val id = artistId.trim()
        require(id.isNotEmpty()) { "artistId empty" }

        Log.d(TAG, "[artists] deleteArtist start id=$id")
        try {
            firestore.collection("artists").document(id).delete().await()
            Log.d(TAG, "[artists] deleteArtist success id=$id")
        } catch (e: Exception) {
            Log.e(TAG, "[artists] deleteArtist failed id=$id: ${e.message}", e)
            throw e
        }
    }
}
