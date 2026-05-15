package com.example.blesstify.domain.auth

sealed class AuthState {
    object Unknown : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String) : AuthState()
}

