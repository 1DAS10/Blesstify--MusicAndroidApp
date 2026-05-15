package com.example.blesstify.domain.repository

import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(userId: String): Flow<Resource<List<Notification>>>
    suspend fun markAsRead(notificationId: String): Resource<Unit>
    fun getUnreadCount(userId: String): Flow<Int>
}
