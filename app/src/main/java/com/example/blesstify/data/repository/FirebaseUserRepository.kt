package com.example.blesstify.data.repository

import android.net.Uri
import com.example.blesstify.core.error.AppError
import com.example.blesstify.domain.model.EqualizerSettings
import com.example.blesstify.domain.model.User
import com.example.blesstify.domain.repository.UserRepository
import com.example.blesstify.domain.user.UserResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {
    override suspend fun createUser(user: User): UserResult<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            val userId = user.id.trim()
            require(userId.isNotBlank()) { "User id is required" }
            val payload = hashMapOf(
                "displayName" to user.displayName,
                "email" to (user.email ?: ""),
                "photoUrl" to user.photoUrl,
                "locale" to user.locale,
                "tier" to user.tier,
                "subscriptionStatus" to null,
                "currentPeriodEnd" to null,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(FirestoreKeys.USERS)
                .document(userId)
                .set(payload)
                .await()
            Unit
        }
    }

    override suspend fun getUser(userId: String): UserResult<User?> = withContext(Dispatchers.IO) {
        safeCall {
            val trimmedId = userId.trim()
            require(trimmedId.isNotBlank()) { "User id is required" }
            val snapshot = firestore.collection(FirestoreKeys.USERS)
                .document(trimmedId)
                .get()
                .await()
            if (!snapshot.exists()) {
                null
            } else {
                snapshot.toObject(User::class.java)?.copy(id = snapshot.id)
            }
        }
    }

    override suspend fun updateUser(
        userId: String,
        displayName: String?,
        avatarUrl: String?,
        email: String?,
        locale: String?
    ): UserResult<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            val trimmedId = userId.trim()
            require(trimmedId.isNotBlank()) { "User id is required" }
            val updates = mutableMapOf<String, Any>()
            displayName?.let { updates["displayName"] = it }
            avatarUrl?.let { updates["photoUrl"] = it }
            email?.let { updates["email"] = it }
            locale?.let { updates["locale"] = it }
            if (updates.isEmpty()) {
                return@safeCall
            }
            updates["updatedAt"] = FieldValue.serverTimestamp()
            firestore.collection(FirestoreKeys.USERS)
                .document(trimmedId)
                .update(updates)
                .await()
            Unit
        }
    }

    override suspend fun updateUserAvatar(userId: String, imageUri: Uri): UserResult<String> =
        withContext(Dispatchers.IO) {
            safeCall {
                val trimmedId = userId.trim()
                require(trimmedId.isNotBlank()) { "User id is required" }
                val storageRef = storage.reference
                    .child("users/$trimmedId/avatars/avatar.jpg")
                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                val updates = mapOf<String, Any>(
                    "photoUrl" to downloadUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection(FirestoreKeys.USERS)
                    .document(trimmedId)
                    .update(updates)
                    .await()
                downloadUrl
        }
    }
    
    override suspend fun getEqualizerSettings(userId: String): UserResult<EqualizerSettings?> = withContext(Dispatchers.IO) {
        safeCall {
            val snapshot = firestore.collection(FirestoreKeys.USERS)
                .document(userId)
                .collection("settings")
                .document("equalizer")
                .get()
                .await()
            if (!snapshot.exists()) {
                null
            } else {
                val isEnabled = snapshot.getBoolean("isEnabled") ?: false
                val preset = snapshot.getString("preset") ?: "flat"
                val bands = (snapshot.get("bands") as? List<*>)?.map { (it as? Number)?.toInt() ?: 0 } ?: listOf(0, 0, 0, 0, 0)
                val virtualizer = (snapshot.getDouble("virtualizer") ?: 0.0).toFloat()
                val bassBoost = (snapshot.getDouble("bassBoost") ?: 0.0).toFloat()
                
                EqualizerSettings(
                    isEnabled = isEnabled,
                    preset = preset,
                    bands = bands,
                    virtualizer = virtualizer,
                    bassBoost = bassBoost
                )
            }
        }
    }

    override suspend fun saveEqualizerSettings(userId: String, settings: EqualizerSettings): UserResult<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            val data = hashMapOf(
                "isEnabled" to settings.isEnabled,
                "preset" to settings.preset,
                "bands" to settings.bands,
                "virtualizer" to settings.virtualizer,
                "bassBoost" to settings.bassBoost,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(FirestoreKeys.USERS)
                .document(userId)
                .collection("settings")
                .document("equalizer")
                .set(data)
                .await()
            Unit
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> T): UserResult<T> {
        return try {
            UserResult.Success(block())
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            UserResult.Error(mapUserException(exception))
        }
    }

    private fun mapUserException(exception: Exception): AppError {
        return when (exception) {
            is IllegalArgumentException -> AppError.InvalidData
            is FirebaseNetworkException -> AppError.Network
            is FirebaseFirestoreException -> when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.PermissionDenied
                FirebaseFirestoreException.Code.NOT_FOUND -> AppError.NotFound
                FirebaseFirestoreException.Code.INVALID_ARGUMENT -> AppError.InvalidData
                FirebaseFirestoreException.Code.UNAVAILABLE -> AppError.Network
                else -> AppError.Unknown(exception.localizedMessage)
            }
            is StorageException -> when (exception.errorCode) {
                StorageException.ERROR_NOT_AUTHORIZED -> AppError.PermissionDenied
                StorageException.ERROR_OBJECT_NOT_FOUND -> AppError.NotFound
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> AppError.Network
                StorageException.ERROR_QUOTA_EXCEEDED -> AppError.QuotaExceeded
                else -> AppError.Unknown(exception.localizedMessage)
            }
            else -> AppError.Unknown(exception.localizedMessage)
        }
    }
}
