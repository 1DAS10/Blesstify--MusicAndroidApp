package com.example.blesstify.data.repository

import android.net.Uri
import android.util.Log
import com.example.blesstify.data.repository.FirestoreKeys
import com.example.blesstify.domain.model.Playlist
import com.example.blesstify.domain.model.PlaylistTrack
import com.example.blesstify.domain.repository.PlaylistRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebasePlaylistRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : PlaylistRepository {

    companion object {
        private const val TAG = FirestoreKeys.TAG
    }

    private fun DocumentSnapshot.toPlaylist(): Playlist? {
        if (!exists()) return null
        return try {
            Playlist(
                id = id,
                ownerId = getString("ownerId") ?: "",
                collaboratorIds = get("collaboratorIds") as? List<String> ?: emptyList(),
                title = getString("title") ?: "",
                description = getString("description"),
                isPublic = getBoolean("isPublic") ?: false,
                isDefault = getBoolean("isDefault") ?: false,
                coverUrl = getString("coverUrl"),
                trackCount = getLong("trackCount")?.toInt() ?: 0,
                likeCount = getLong("likeCount")?.toInt() ?: 0,
                createdAt = getTimestamp("createdAt"),
                updatedAt = getTimestamp("updatedAt")
            )
        } catch (e: Exception) {
            Log.e(TAG, "[playlists] Failed to parse playlist ${id}: ${e.message}")
            null
        }
    }

    private fun DocumentSnapshot.toPlaylistTrack(): PlaylistTrack? {
        if (!exists()) return null
        return try {
            PlaylistTrack(
                songId = getString("songId") ?: id,
                order = getLong("order")?.toInt() ?: 0,
                addedAt = getTimestamp("addedAt"),
                addedBy = getString("addedBy") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "[playlists] Failed to parse track ${id}: ${e.message}")
            null
        }
    }

    override suspend fun createPlaylist(playlist: Playlist): String {
        val doc = if (playlist.id.isBlank()) {
            firestore.collection(FirestoreKeys.PLAYLISTS).document()
        } else {
            firestore.collection(FirestoreKeys.PLAYLISTS).document(playlist.id)
        }
        val data = hashMapOf(
            "ownerId" to playlist.ownerId,
            "collaboratorIds" to playlist.collaboratorIds,
            "title" to playlist.title,
            "description" to playlist.description,
            "isPublic" to playlist.isPublic,
            "isDefault" to playlist.isDefault,
            "coverUrl" to playlist.coverUrl,
            "trackCount" to 0,
            "likeCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        Log.d(TAG, "[playlists] createPlaylist start ownerId=${playlist.ownerId} title=${playlist.title}")
        try {
            doc.set(data).await()
            Log.d(TAG, "[playlists] createPlaylist success id=${doc.id} ownerId=${playlist.ownerId}")
            return doc.id
        } catch (e: Exception) {
            Log.e(TAG, "[playlists] createPlaylist failed ownerId=${playlist.ownerId}: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        require(playlist.id.isNotBlank()) { "Playlist id is required" }
        val data = hashMapOf<String, Any?>(
            "title" to playlist.title,
            "description" to playlist.description,
            "isPublic" to playlist.isPublic,
            "coverUrl" to playlist.coverUrl,
            "collaboratorIds" to playlist.collaboratorIds,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        Log.d(TAG, "[playlists] updatePlaylist start id=${playlist.id} title=${playlist.title}")
        try {
            firestore.collection(FirestoreKeys.PLAYLISTS)
                .document(playlist.id)
                .update(data)
                .await()
            Log.d(TAG, "[playlists] updatePlaylist success id=${playlist.id}")
        } catch (e: Exception) {
            Log.e(TAG, "[playlists] updatePlaylist failed id=${playlist.id}: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deletePlaylist(playlistId: String) {
        // Simple delete for now. To be perfect, we should delete tracks too,
        // but Firestore requires manual deletion of subcollections or a cloud function.
        firestore.collection(FirestoreKeys.PLAYLISTS)
            .document(playlistId)
            .delete()
            .await()
    }

    override suspend fun getPlaylistById(playlistId: String): Playlist? {
        Log.d(TAG, "[playlists] getPlaylistById start id=$playlistId")
        return try {
            val snapshot = firestore.collection(FirestoreKeys.PLAYLISTS)
                .document(playlistId)
                .get()
                .await()
            val playlist = snapshot.toPlaylist()
            Log.d(TAG, "[playlists] getPlaylistById success id=$playlistId found=${playlist != null}")
            playlist
        } catch (e: Exception) {
            Log.e(TAG, "[playlists] getPlaylistById failed id=$playlistId: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getPublicPlaylists(limit: Int): List<Playlist> {
        Log.d(TAG, "[playlists] getPublicPlaylists start limit=$limit")
        return try {
            val snapshots = firestore.collection(FirestoreKeys.PLAYLISTS)
                .whereEqualTo("isPublic", true)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            val playlists = snapshots.documents.mapNotNull { it.toPlaylist() }
            Log.d(TAG, "[playlists] getPublicPlaylists success count=${playlists.size}")
            playlists
        } catch (e: Exception) {
            Log.e(TAG, "[playlists] getPublicPlaylists failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getUserPlaylists(userId: String, limit: Int): List<Playlist> {
        Log.d(TAG, "[playlists] getUserPlaylists start userId=$userId limit=$limit")
        val snapshots = try {
            firestore.collection(FirestoreKeys.PLAYLISTS)
                .where(
                    Filter.or(
                        Filter.equalTo("ownerId", userId),
                        Filter.arrayContains("collaboratorIds", userId)
                    )
                )
                .get()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "[playlists] getUserPlaylists failed userId=$userId: ${e.message}", e)
            throw e
        }

        val playlists = snapshots.documents.mapNotNull { it.toPlaylist() }
        Log.d(TAG, "[playlists] getUserPlaylists success count=${playlists.size}")

        // Default playlist first, then by updatedAt desc
        return playlists.sortedWith(
            compareByDescending<Playlist> { it.isDefault }.thenByDescending { it.updatedAt }
        )
    }

    override suspend fun addTrack(playlistId: String, track: PlaylistTrack) {
        // Add track to subcollection
        val trackData = hashMapOf(
            "songId" to track.songId,
            "order" to track.order,
            "addedAt" to FieldValue.serverTimestamp(),
            "addedBy" to track.addedBy
        )
        firestore.collection(FirestoreKeys.PLAYLISTS)
            .document(playlistId)
            .collection(FirestoreKeys.TRACKS)
            .document(track.songId)
            .set(trackData)
            .await()

        // Increment trackCount on parent playlist
        firestore.collection(FirestoreKeys.PLAYLISTS)
            .document(playlistId)
            .update(
                "trackCount", FieldValue.increment(1),
                "updatedAt", FieldValue.serverTimestamp()
            )
            .await()
        Log.d(TAG, "[playlists] addTrack success playlistId=$playlistId songId=${track.songId}")
    }

    override suspend fun removeTrack(playlistId: String, songId: String) {
        // Remove track from subcollection
        firestore.collection(FirestoreKeys.PLAYLISTS)
            .document(playlistId)
            .collection(FirestoreKeys.TRACKS)
            .document(songId)
            .delete()
            .await()

        // Decrement trackCount on parent playlist
        firestore.collection(FirestoreKeys.PLAYLISTS)
            .document(playlistId)
            .update(
                "trackCount", FieldValue.increment(-1),
                "updatedAt", FieldValue.serverTimestamp()
            )
            .await()
        Log.d(TAG, "[playlists] removeTrack success playlistId=$playlistId songId=$songId")
    }

    override suspend fun getTracks(playlistId: String): List<PlaylistTrack> {
        Log.d(TAG, "[playlists] getTracks start playlistId=$playlistId")
        return try {
            val snapshots = firestore.collection(FirestoreKeys.PLAYLISTS)
                .document(playlistId)
                .collection(FirestoreKeys.TRACKS)
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()
            val tracks = snapshots.documents.mapNotNull { it.toPlaylistTrack() }
            Log.d(TAG, "[playlists] getTracks success playlistId=$playlistId count=${tracks.size}")
            tracks
        } catch (e: Exception) {
            Log.e(TAG, "[playlists] getTracks failed playlistId=$playlistId: ${e.message}", e)
            throw e
        }
    }

    override suspend fun uploadPlaylistCover(playlistId: String, uri: Uri): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val path = "playlists/$playlistId/covers/$fileName"
        val ref = storage.reference.child(path)

        Log.d(TAG, "[uploadPlaylistCover] start playlistId=$playlistId uri=$uri path=$path")
        try {
            ref.putFile(uri).await()
            Log.d(TAG, "[uploadPlaylistCover] putFile success path=$path")

            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "[uploadPlaylistCover] downloadUrl=$downloadUrl")

            firestore.collection(FirestoreKeys.PLAYLISTS)
                .document(playlistId)
                .update("coverUrl", downloadUrl, "updatedAt", FieldValue.serverTimestamp())
                .await()

            Log.d(TAG, "[uploadPlaylistCover] firestore update success playlistId=$playlistId")
            return downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "[uploadPlaylistCover] failed playlistId=$playlistId: ${e.message}", e)
            throw e
        }
    }

    override suspend fun likePlaylist(userId: String, playlistId: String) {
        firestore.runTransaction { transaction ->
            val likedDoc = firestore.collection(FirestoreKeys.USERS)
                .document(userId)
                .collection(FirestoreKeys.LIKED_PLAYLISTS)
                .document(playlistId)

            transaction.set(likedDoc, mapOf("likedAt" to FieldValue.serverTimestamp()))

            val playlistDoc = firestore.collection(FirestoreKeys.PLAYLISTS).document(playlistId)
            transaction.update(playlistDoc, "likeCount", FieldValue.increment(1))
        }.await()
    }

    override suspend fun unlikePlaylist(userId: String, playlistId: String) {
        firestore.runTransaction { transaction ->
            val likedDoc = firestore.collection(FirestoreKeys.USERS)
                .document(userId)
                .collection(FirestoreKeys.LIKED_PLAYLISTS)
                .document(playlistId)

            transaction.delete(likedDoc)

            val playlistDoc = firestore.collection(FirestoreKeys.PLAYLISTS).document(playlistId)
            transaction.update(playlistDoc, "likeCount", FieldValue.increment(-1))
        }.await()
    }

    override fun isPlaylistLiked(userId: String, playlistId: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LIKED_PLAYLISTS)
            .document(playlistId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getLikedPlaylists(userId: String): List<Playlist> {
        val snapshots = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LIKED_PLAYLISTS)
            .get()
            .await()

        val playlistIds = snapshots.documents.map { it.id }
        if (playlistIds.isEmpty()) return emptyList()

        // Fetch playlist details
        return playlistIds.mapNotNull { getPlaylistById(it) }
    }
}
