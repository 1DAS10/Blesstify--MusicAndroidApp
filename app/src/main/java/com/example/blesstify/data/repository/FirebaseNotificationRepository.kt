package com.example.blesstify.data.repository

import com.example.blesstify.core.error.AppError
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Notification
import com.example.blesstify.domain.repository.NotificationRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepository(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    override fun getNotifications(userId: String): Flow<Resource<List<Notification>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(AppError.Unknown(error.message)))
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Resource.Success(notifications))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("readAt", FieldValue.serverTimestamp())
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(AppError.Unknown(e.message))
        }
    }

    override fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("readAt", null)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }
}
