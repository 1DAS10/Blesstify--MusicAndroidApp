package com.example.blesstify.presentation.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.model.Notification
import com.example.blesstify.domain.usecase.GetNotificationsUseCase
import com.example.blesstify.domain.usecase.GetUnreadCountUseCase
import com.example.blesstify.domain.usecase.MarkNotificationAsReadUseCase
import com.example.blesstify.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    private val _notifications = MutableStateFlow<Resource<List<Notification>>>(Resource.Loading())
    val notifications = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    private val _inAppEvents = MutableSharedFlow<InAppNotificationEvent>(extraBufferCapacity = 1)
    val inAppEvents = _inAppEvents.asSharedFlow()

    private var currentUserId: String? = null
    private var lastEmittedNotificationId: String? = null

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collectLatest { state ->
                if (state is AuthState.Authenticated) {
                    currentUserId = state.userId
                    lastEmittedNotificationId = null
                    observeNotifications(state.userId)
                    observeUnreadCount(state.userId)
                } else {
                    currentUserId = null
                    lastEmittedNotificationId = null
                    _notifications.value = Resource.Success(emptyList())
                    _unreadCount.value = 0
                }
            }
        }
    }

    private fun observeNotifications(userId: String) {
        viewModelScope.launch {
            getNotificationsUseCase(userId).collect { resource ->
                _notifications.value = resource

                val latest = (resource as? Resource.Success)?.data
                    ?.firstOrNull { it.readAt == null }

                if (latest != null && latest.id.isNotBlank() && latest.id != lastEmittedNotificationId) {
                    lastEmittedNotificationId = latest.id
                    _inAppEvents.tryEmit(InAppNotificationEvent.Show(latest))
                }
            }
        }
    }

    private fun observeUnreadCount(userId: String) {
        viewModelScope.launch {
            getUnreadCountUseCase(userId).collect { count ->
                _unreadCount.value = count
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            markNotificationAsReadUseCase(notificationId)
        }
    }
}
