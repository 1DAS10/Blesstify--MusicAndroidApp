package com.example.blesstify.data.repository

import com.example.blesstify.domain.model.ListeningHistory
import com.example.blesstify.domain.repository.HistoryRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FirebaseHistoryRepository(
    private val firestore: FirebaseFirestore
) : HistoryRepository {
    override suspend fun logHistory(userId: String, item: ListeningHistory) {
        Log.d("HistoryDebug", "Repo: logHistory called - userId=$userId songId=${item.songId} durationSec=${item.durationSec}")
        try {
            firestore.runTransaction { transaction ->
                val songDoc = firestore.collection(FirestoreKeys.SONGS).document(item.songId)
                val snapshot = transaction.get(songDoc)

                val historyCollection = firestore.collection(FirestoreKeys.USERS)
                    .document(userId)
                    .collection(FirestoreKeys.LISTENING_HISTORY)
                val historyDoc = historyCollection.document()
                
                val historyData = hashMapOf(
                    "songId" to item.songId,
                    "playlistId" to item.playlistId,
                    "durationSec" to item.durationSec,
                    "source" to item.source,
                    "device" to item.device,
                    "playedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                
                transaction.set(historyDoc, historyData)
                if (snapshot.exists()) {
                    transaction.update(songDoc, "playCount", com.google.firebase.firestore.FieldValue.increment(1))
                }
                Log.d("HistoryDebug", "Repo: transaction completed inside lambda")
            }.await()
            Log.d("HistoryDebug", "Repo: logHistory SUCCESS")
        } catch (e: Exception) {
            Log.e("HistoryDebug", "Repo: logHistory FAILED", e)
            throw e
        }
    }

    override suspend fun evaluateDailyListeningMilestones(userId: String) {
        val now = Calendar.getInstance()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(now.time)
        val todayPlayCount = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LISTENING_HISTORY)
            .whereGreaterThanOrEqualTo("playedAt", Timestamp(startOfDay.time))
            .whereLessThanOrEqualTo("playedAt", Timestamp(endOfDay.time))
            .get()
            .await()
            .size()

        val milestones = listOf(10, 30)
        milestones
            .filter { todayPlayCount >= it }
            .forEach { milestone ->
                createDailyMilestoneNotificationIfNeeded(
                    userId = userId,
                    milestone = milestone,
                    dayKey = dayKey,
                    todayPlayCount = todayPlayCount
                )
            }
    }

    private suspend fun createDailyMilestoneNotificationIfNeeded(
        userId: String,
        milestone: Int,
        dayKey: String,
        todayPlayCount: Int
    ) {
        val notificationId = "${userId}_daily_listening_${milestone}_$dayKey"
        val notificationRef = firestore.collection(FirestoreKeys.NOTIFICATIONS).document(notificationId)
        val snapshot = notificationRef.get().await()
        if (snapshot.exists()) return

        val notificationData = hashMapOf(
            "userId" to userId,
            "title" to "Chúc mừng bạn! 🎧",
            "body" to "Bạn đã nghe $milestone bài hôm nay. Tiếp tục giữ nhịp cùng Blesstify nhé!",
            "type" to "listening_milestone",
            "data" to mapOf(
                "milestone" to milestone.toString(),
                "playCount" to todayPlayCount.toString(),
                "dayKey" to dayKey
            ),
            "createdAt" to FieldValue.serverTimestamp(),
            "readAt" to null
        )
        notificationRef.set(notificationData).await()
    }

    override suspend fun getHistory(userId: String, limit: Int): List<ListeningHistory> {
        val snapshots = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LISTENING_HISTORY)
            .orderBy("playedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        return snapshots.documents.mapNotNull { doc ->
            val songId = doc.getString("songId") ?: return@mapNotNull null
            val playedAt = doc.getTimestamp("playedAt")?.toDate()?.time ?: 0L
            ListeningHistory(
                id = doc.id,
                songId = songId,
                playlistId = doc.getString("playlistId"),
                durationSec = doc.getLong("durationSec")?.toInt() ?: 0,
                source = doc.getString("source") ?: "unknown",
                device = doc.getString("device") ?: "android",
                playedAt = playedAt
            )
        }.distinctBy { it.songId } // Keep only the most recent entry for each song
    }

    override fun observeHistory(userId: String, limit: Int): kotlinx.coroutines.flow.Flow<List<ListeningHistory>> = kotlinx.coroutines.flow.callbackFlow {
        val subscription = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LISTENING_HISTORY)
            .orderBy("playedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val items = snapshots.documents.mapNotNull { doc ->
                        val songId = doc.getString("songId") ?: return@mapNotNull null
                        val playedAt = doc.getTimestamp("playedAt")?.toDate()?.time ?: 0L
                        ListeningHistory(
                            id = doc.id,
                            songId = songId,
                            playlistId = doc.getString("playlistId"),
                            durationSec = doc.getLong("durationSec")?.toInt() ?: 0,
                            source = doc.getString("source") ?: "unknown",
                            device = doc.getString("device") ?: "android",
                            playedAt = playedAt
                        )
                    }.distinctBy { it.songId } // Keep only the most recent entry for each song
                    trySend(items)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun clearHistory(userId: String) {
        val collection = firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.LISTENING_HISTORY)
        val snapshots = collection.get().await()
        val batch = firestore.batch()
        for (doc in snapshots.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }
}
