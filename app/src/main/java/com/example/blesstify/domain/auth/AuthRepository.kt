package com.example.blesstify.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun authState(): Flow<AuthState>
    fun currentUserId(): String?
    fun currentUser(): AuthUser?
    fun signIn(email: String, password: String): Flow<AuthResult>
    fun signUp(email: String, password: String, displayName: String?): Flow<AuthResult>
    fun signOut(): Flow<AuthResult>
    fun signInWithGoogle(idToken: String): Flow<AuthResult>
    fun signInWithFacebook(accessToken: String): Flow<AuthResult>
}
