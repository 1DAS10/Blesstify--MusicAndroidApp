package com.example.blesstify.domain.model

import com.google.firebase.Timestamp

data class SearchHistory(
    val id: String = "",
    val query: String = "",
    val searchedAt: Timestamp? = null
)
