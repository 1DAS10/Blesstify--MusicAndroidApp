package com.example.blesstify.presentation.ui.notification

import com.example.blesstify.domain.model.Notification

sealed class InAppNotificationEvent {
    data class Show(val notification: Notification) : InAppNotificationEvent()
}
