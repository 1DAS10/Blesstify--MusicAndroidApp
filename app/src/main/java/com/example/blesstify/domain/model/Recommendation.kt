package com.example.blesstify.domain.model

data class Recommendation(
    val id: String = "",
    val userId: String = "",
    val type: String = "ai",
    val generatedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val mood: String? = null,
    val energy: Double? = null,
    val songIds: List<String> = emptyList()
)
