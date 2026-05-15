package com.example.blesstify.data.repository

import com.example.blesstify.core.error.AppError
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Subscription
import com.example.blesstify.domain.repository.SubscriptionRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSubscriptionRepository(
    private val firestore: FirebaseFirestore
) : SubscriptionRepository {

    override suspend fun getSubscription(userId: String): Resource<Subscription?> {
        return try {
            val snapshot = firestore.collection("subscriptions")
                .document(userId)
                .get()
                .await()
            
            if (snapshot.exists()) {
                Resource.Success(snapshot.toObject(Subscription::class.java))
            } else {
                Resource.Success(null)
            }
        } catch (e: Exception) {
            Resource.Error(AppError.Unknown(e.message))
        }
    }
}
