package com.example.blesstify.domain.model

import com.google.firebase.Timestamp

data class Subscription(
    val plan: String = "monthly", // monthly, yearly
    val status: String = "active", // active, canceled, past_due
    val currentPeriodEnd: Timestamp? = null
)
