package com.example.blesstify.domain.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val email: String? = null,
    val tier: String = "free",
    val locale: String = "vi-VN",
    val subscriptionStatus: String? = null,
    val currentPeriodEnd: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
