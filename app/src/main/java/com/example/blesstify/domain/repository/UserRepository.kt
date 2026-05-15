package com.example.blesstify.domain.repository

import android.net.Uri
import com.example.blesstify.domain.model.EqualizerSettings
import com.example.blesstify.domain.model.User
import com.example.blesstify.domain.user.UserResult

interface UserRepository {
    suspend fun createUser(user: User): UserResult<Unit>
    suspend fun getUser(userId: String): UserResult<User?>
    suspend fun updateUser(
        userId: String,
        displayName: String?,
        avatarUrl: String?,
        email: String?,
        locale: String?
    ): UserResult<Unit>
    suspend fun updateUserAvatar(userId: String, imageUri: Uri): UserResult<String>
    suspend fun getEqualizerSettings(userId: String): UserResult<EqualizerSettings?>
    suspend fun saveEqualizerSettings(userId: String, settings: EqualizerSettings): UserResult<Unit>
}
