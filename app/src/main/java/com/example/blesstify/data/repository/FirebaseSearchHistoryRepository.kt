package com.example.blesstify.data.repository

import com.example.blesstify.domain.model.SearchHistory
import com.example.blesstify.domain.repository.SearchHistoryRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSearchHistoryRepository(
    private val firestore: FirebaseFirestore
) : SearchHistoryRepository {

    private fun historyRef(userId: String) =
        firestore.collection(FirestoreKeys.USERS)
            .document(userId)
            .collection(FirestoreKeys.SEARCH_HISTORY)

    override suspend fun saveSearch(userId: String, query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext

        // Check if this exact query already exists — if so, update its timestamp
        val existing = historyRef(userId)
            .whereEqualTo("query", trimmed)
            .limit(1)
            .get()
            .await()

        if (existing.documents.isNotEmpty()) {
            // Update existing entry's timestamp to move it to top
            existing.documents.first().reference.update(
                "searchedAt", FieldValue.serverTimestamp()
            ).await()
        } else {
            // Create new entry
            val payload = hashMapOf(
                "query" to trimmed,
                "searchedAt" to FieldValue.serverTimestamp()
            )
            historyRef(userId).add(payload).await()

            // Enforce max 20 entries — delete oldest if over limit
            val allEntries = historyRef(userId)
                .orderBy("searchedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            if (allEntries.size() > 20) {
                val toDelete = allEntries.documents.drop(20)
                val batch = firestore.batch()
                toDelete.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
    }

    override suspend fun getRecentSearches(userId: String, limit: Int): List<SearchHistory> =
        withContext(Dispatchers.IO) {
            val snapshots = historyRef(userId)
                .orderBy("searchedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshots.documents.mapNotNull { doc ->
                val query = doc.getString("query") ?: return@mapNotNull null
                SearchHistory(
                    id = doc.id,
                    query = query,
                    searchedAt = doc.getTimestamp("searchedAt")
                )
            }
        }

    override suspend fun deleteSearch(userId: String, searchId: String): Unit =
        withContext(Dispatchers.IO) {
            historyRef(userId).document(searchId).delete().await()
        }

    override suspend fun clearAll(userId: String): Unit = withContext(Dispatchers.IO) {
        val snapshots = historyRef(userId).get().await()
        val batch = firestore.batch()
        snapshots.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
