package com.example.blesstify.data.repository

import com.example.blesstify.domain.auth.AuthRepository
import com.example.blesstify.domain.auth.AuthResult
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.auth.AuthUser
import com.example.blesstify.core.error.AppError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth
) : AuthRepository {
    override fun authState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val state = if (user == null) {
                AuthState.Unauthenticated
            } else {
                AuthState.Authenticated(user.uid)
            }
            trySend(state)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun currentUser(): AuthUser? {
        val user = auth.currentUser ?: return null
        return AuthUser(
            id = user.uid,
            displayName = user.displayName,
            email = user.email,
            photoUrl = user.photoUrl?.toString()
        )
    }

    override fun signIn(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: error("Missing user after sign in")
            emit(AuthResult.Success(userId))
        } catch (exception: Exception) {
            emit(AuthResult.Error(mapAuthException(exception)))
        }
    }.flowOn(Dispatchers.IO)

    override fun signUp(email: String, password: String, displayName: String?): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: error("Missing user after sign up")
            if (!displayName.isNullOrBlank()) {
                val updates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(updates).await()
            }
            emit(AuthResult.Success(user.uid))
        } catch (exception: Exception) {
            emit(AuthResult.Error(mapAuthException(exception)))
        }
    }.flowOn(Dispatchers.IO)

    override fun signOut(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            auth.signOut()
            emit(AuthResult.SignedOut)
        } catch (exception: Exception) {
            emit(AuthResult.Error(mapAuthException(exception)))
        }
    }.flowOn(Dispatchers.IO)

    override fun signInWithGoogle(idToken: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val userId = result.user?.uid ?: error("Missing user after Google sign in")
            emit(AuthResult.Success(userId))
        } catch (exception: Exception) {
            emit(AuthResult.Error(mapAuthException(exception)))
        }
    }.flowOn(Dispatchers.IO)

    private fun mapAuthException(exception: Exception): AppError {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> AppError.InvalidCredentials
            is FirebaseAuthInvalidUserException -> AppError.NotFound
            is FirebaseAuthUserCollisionException -> AppError.EmailAlreadyInUse
            is FirebaseNetworkException -> AppError.Network
            is FirebaseAuthException -> when (exception.errorCode) {
                "ERROR_WEAK_PASSWORD" -> AppError.WeakPassword
                else -> AppError.Unknown(exception.localizedMessage)
            }
            else -> AppError.Unknown(exception.localizedMessage)
        }
    }
}
