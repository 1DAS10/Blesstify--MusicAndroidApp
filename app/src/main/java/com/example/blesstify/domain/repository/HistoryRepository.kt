package com.example.blesstify.domain.repository

import com.example.blesstify.domain.model.ListeningHistory

interface HistoryRepository {
    suspend fun logHistory(userId: String, item: ListeningHistory)
    suspend fun getHistory(userId: String, limit: Int = 100): List<ListeningHistory>
    fun observeHistory(userId: String, limit: Int = 100): kotlinx.coroutines.flow.Flow<List<ListeningHistory>>
    suspend fun clearHistory(userId: String)
}

