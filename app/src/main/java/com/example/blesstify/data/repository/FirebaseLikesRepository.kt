package com.example.blesstify.data.repository

import com.example.blesstify.domain.repository.LikesRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseLikesRepository(
    private val firestore: FirebaseFirestore
) : LikesRepository {

    override suspend fun likeSong(userId: String, songId: String) {
        val likedSongRef = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LIKED_SONGS)
            .document(songId)
            
        val songRef = firestore.collection(FirestoreKeys.SONGS).document(songId)

        try {
            firestore.runTransaction { transaction ->
                val likedSnapshot = transaction.get(likedSongRef)
                if (!likedSnapshot.exists()) {
                    transaction.set(likedSongRef, mapOf("likedAt" to FieldValue.serverTimestamp()))
                    transaction.update(songRef, "likeCount", FieldValue.increment(1))
                }
            }.await()
        } catch (e: Exception) {
            // If transaction fails (e.g. due to security rules preventing likeCount update for non-owners),
            // fallback to just updating the user's subcollection
            likedSongRef.set(mapOf("likedAt" to FieldValue.serverTimestamp())).await()
        }
    }

    override suspend fun unlikeSong(userId: String, songId: String) {
        val likedSongRef = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LIKED_SONGS)
            .document(songId)
            
        val songRef = firestore.collection(FirestoreKeys.SONGS).document(songId)

        try {
            firestore.runTransaction { transaction ->
                val likedSnapshot = transaction.get(likedSongRef)
                if (likedSnapshot.exists()) {
                    transaction.delete(likedSongRef)
                    transaction.update(songRef, "likeCount", FieldValue.increment(-1))
                }
            }.await()
        } catch (e: Exception) {
            // Fallback if transaction fails
            likedSongRef.delete().await()
        }
    }

    override fun isLiked(userId: String, songId: String): Flow<Boolean> = callbackFlow {
        val listenerRegistration = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LIKED_SONGS)
            .document(songId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() ?: false)
            }
            
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getLikedSongIds(userId: String): List<String> {
        val snapshots = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LIKED_SONGS)
            .get()
            .await()
        return snapshots.documents.map { it.id }
    }
}
