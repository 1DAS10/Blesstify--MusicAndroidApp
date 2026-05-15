package com.example.blesstify.domain.repository

import com.example.blesstify.domain.model.Recommendation

interface RecommendationRepository {
    suspend fun getLatestRecommendation(userId: String): Recommendation?
    suspend fun saveRecommendation(recommendation: Recommendation)
}

