package com.example.blesstify.domain.repository

import com.example.blesstify.domain.model.SearchHistory

interface SearchHistoryRepository {
    suspend fun saveSearch(userId: String, query: String)
    suspend fun getRecentSearches(userId: String, limit: Int = 10): List<SearchHistory>
    suspend fun deleteSearch(userId: String, searchId: String)
    suspend fun clearAll(userId: String)
}
