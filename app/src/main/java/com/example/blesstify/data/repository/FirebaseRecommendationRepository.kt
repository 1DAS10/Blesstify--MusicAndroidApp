package com.example.blesstify.data.repository

import com.example.blesstify.domain.model.Recommendation
import com.example.blesstify.domain.repository.RecommendationRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseRecommendationRepository(
    private val firestore: FirebaseFirestore
) : RecommendationRepository {
    override suspend fun getLatestRecommendation(userId: String): Recommendation? {
        val snapshots = firestore.collection(FirestoreKeys.RECOMMENDATIONS)
            .whereEqualTo("userId", userId)
            .orderBy("generatedAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        val doc = snapshots.documents.firstOrNull() ?: return null
        return doc.toObject(Recommendation::class.java)?.copy(id = doc.id)
    }

    override suspend fun saveRecommendation(recommendation: Recommendation) {
        val data = hashMapOf(
            "userId" to recommendation.userId,
            "type" to recommendation.type,
            "mood" to recommendation.mood,
            "energy" to recommendation.energy,
            "songIds" to recommendation.songIds,
            "generatedAt" to FieldValue.serverTimestamp(),
            "expiresAt" to Timestamp(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
        )
        firestore.collection(FirestoreKeys.RECOMMENDATIONS).add(data).await()
    }
}

