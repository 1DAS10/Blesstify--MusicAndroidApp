package com.example.blesstify.domain.repository

import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Subscription

interface SubscriptionRepository {
    suspend fun getSubscription(userId: String): Resource<Subscription?>
}
